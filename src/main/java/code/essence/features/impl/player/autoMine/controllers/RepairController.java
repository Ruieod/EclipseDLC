package code.essence.features.impl.player.autoMine.controllers;

import code.essence.display.screens.clickgui.components.implement.autobuy.util.AuctionUtils;
import code.essence.utils.client.chat.ChatMessage;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.interactions.inv.InventoryResult;
import code.essence.utils.interactions.inv.InventoryTask;
import code.essence.utils.interactions.inv.InventoryToolkit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Контроллер починки кирки:
 * 1. Очищает 3 слота: выкидывает не-алмазы и не-кирки; консолидирует единичные алмазы
 * 2. Открывает /ah, ждёт 2000мс, покупает 3 стака XP до 100к за стак
 * 3. Кирка в левую руку, опыт в правую, useKey, смена слота при пустом
 * 4. Починено — кирка в основной слот, продолжаем майнить
 *
 * @since 14.02.2026
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RepairController implements QuickImports {

    /** Порог прочности (в процентах), ниже которого начинаем ремонт. */
    static final int DURABILITY_THRESHOLD_PERCENT = 15;

    /** Максимальная цена за стак пузырьков опыта. */
    static final int MAX_XP_STACK_PRICE = 100_000;

    /** Сколько стаков покупаем. */
    static final int XP_STACKS_TO_BUY = 3;

    /** Минимальный размер стака для покупки (только полные стаки). */
    static final int MIN_STACK_SIZE = 64;

    /** Задержка после покупки перед починкой — ждём синхронизацию предметов (мс). */
    static final long WAITING_AFTER_BUY_MS = 2500;

    /** Слотов под опыт должно быть свободно. */
    static final int SLOTS_TO_CLEAR = 3;

    /** Задержка между действиями в аукционе (мс). */
    static final long AUCTION_ACTION_DELAY = 350;

    /** Задержка после каждой покупки перед следующей (мс). */
    static final long POST_PURCHASE_DELAY_MS = 1000;

    /** Задержка между бросками опыта (мс), 0 = каждый тик. */
    static final long THROW_DELAY = 0;

    /** Задержка после свапа в руку перед первым броском (мс). */
    static final long POST_SWAP_DELAY_MS = 250;

    /** Задержка после /ah перед началом покупки (мс). */
    static final long POST_COMMAND_DELAY_MS = 2000;

    /** Задержка ожидания загрузки аукциона (мс). */
    static final long AUCTION_LOAD_DELAY = 3000;

    /** Макс время на покупку, после которого считаем фейл (мс). */
    static final long BUY_TIMEOUT = 30_000;

    /** Паттерн для парсинга цены из лора. */
    static final Pattern PRICE_PATTERN = Pattern.compile(
            "(?:\\$\\s*([\\d,.\\s]+)|([\\d,.\\s]+)\\s*\\$|(?:\u0426\u0435\u043d\u0430|Price)[:\\s]*([\\d,.\\s]+))");

    // --- Состояние покупки ---
    RepairPhase phase = RepairPhase.IDLE;
    long phaseStartTime = 0;
    long lastActionTime = 0;
    long lastPurchaseTime = 0;
    int stacksBought = 0;
    boolean auctionOpened = false;
    int refreshCount = 0;

    // --- Состояние броска ---
    int throwTickCounter = 0;
    long lastThrowTime = 0;
    /** Нужно отпустить useKey на следующем тике после броска. */
    boolean needReleaseUseKey = false;

    // --- Состояние очистки ---
    int dropActionIndex = 0;
    List<Integer> slotsToDrop = new ArrayList<>();
    int consolidateTicks = 0;
    int consolidateTargetFree = 0;

    /** Повторы ожидания предметов после покупки. */
    int noItemsRetryCount = 0;

    public enum RepairPhase {
        IDLE,
        CLEARING_INVENTORY,
        DROPPING_ITEMS,
        CONSOLIDATING_DIAMONDS,
        OPENING_AUCTION,
        WAITING_AUCTION,
        SCANNING_BUYING,
        DONE_BUYING,
        WAITING_AFTER_BUY,
        SWAPPING_PICKAXE_AND_XP,
        THROWING_XP,
        RETURNING_PICKAXE,
        DONE_REPAIR
    }

    // ============================
    // === ПРОВЕРКА ПРОЧНОСТИ ===
    // ============================

    /** Нужно ли чинить кирку? */
    public boolean needsRepair() {
        if (mc.player == null) return false;

        ItemStack pickaxe = findPickaxe();
        if (pickaxe == null || pickaxe.isEmpty()) return false;
        if (!pickaxe.isDamageable()) return false;
        if (!hasMending(pickaxe)) return false;

        int maxDurability = pickaxe.getMaxDamage();
        int currentDamage = pickaxe.getDamage();
        int remaining = maxDurability - currentDamage;
        int percent = maxDurability > 0 ? (remaining * 100) / maxDurability : 100;

        return percent <= DURABILITY_THRESHOLD_PERCENT;
    }

    /** Находит кирку: сначала в основной руке, потом в инвентаре. */
    private ItemStack findPickaxe() {
        if (mc.player == null) return null;

        ItemStack mainHand = mc.player.getMainHandStack();
        if (mainHand.getItem() instanceof PickaxeItem) return mainHand;

        InventoryResult result = InventoryToolkit.findInInventory(
                stack -> stack.getItem() instanceof PickaxeItem);
        return (result != null && result.slot() != -1) ? result.stack() : null;
    }

    /** Проверяет есть ли Mending на предмете. */
    private boolean hasMending(ItemStack stack) {
        var enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) return false;

        for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
            String id = entry.getIdAsString();
            if (id != null && id.toLowerCase().contains("mending")) {
                return true;
            }
        }
        return false;
    }

    /** Кирка полностью починена? */
    public boolean isPickaxeFullyRepaired() {
        if (mc.player == null) return true;

        ItemStack pickaxe = findPickaxe();
        if (pickaxe == null || pickaxe.isEmpty()) return true;
        if (!pickaxe.isDamageable()) return true;

        return pickaxe.getDamage() <= 5;
    }

    /** Текущий процент прочности кирки. */
    public int getPickaxeDurabilityPercent() {
        ItemStack pickaxe = findPickaxe();
        if (pickaxe == null || !pickaxe.isDamageable()) return 100;

        int max = pickaxe.getMaxDamage();
        int dmg = pickaxe.getDamage();
        return max > 0 ? ((max - dmg) * 100) / max : 100;
    }

    // ============================
    // === СТАРТ ПОЛНОГО РЕМОНТА ===
    // ============================

    /** Начать полный процесс починки (очистка -> покупка -> бросок). */
    public void startRepair() {
        ChatMessage.brandmessage("[AutoMine Repair] Начинаю починку кирки");
        phase = RepairPhase.CLEARING_INVENTORY;
        phaseStartTime = System.currentTimeMillis();
        lastActionTime = 0;
        stacksBought = 0;
        auctionOpened = false;
        refreshCount = 0;
        throwTickCounter = 0;
        lastThrowTime = 0;
        dropActionIndex = 0;
        slotsToDrop.clear();
        consolidateTicks = 0;
        consolidateTargetFree = 0;
        noItemsRetryCount = 0;
    }

    /**
     * Тик починки — единый метод для всего процесса.
     * @return true когда починка полностью завершена
     */
    public boolean tickRepair() {
        if (mc.player == null) return true;

        long now = System.currentTimeMillis();

        if (phase == RepairPhase.DROPPING_ITEMS || phase == RepairPhase.CONSOLIDATING_DIAMONDS) {
            mc.player.setPitch(0f);
        } else if (phase == RepairPhase.SWAPPING_PICKAXE_AND_XP || phase == RepairPhase.THROWING_XP || phase == RepairPhase.RETURNING_PICKAXE) {
            mc.player.setPitch(90f);
        }

        switch (phase) {
            case CLEARING_INVENTORY -> tickClearingInventory(now);
            case DROPPING_ITEMS -> tickDroppingItems(now);
            case CONSOLIDATING_DIAMONDS -> tickConsolidatingDiamonds(now);
            case OPENING_AUCTION, WAITING_AUCTION, SCANNING_BUYING, DONE_BUYING -> tickBuying();
            case WAITING_AFTER_BUY -> {
                if (now - phaseStartTime >= WAITING_AFTER_BUY_MS) {
                    ChatMessage.brandmessage("[AutoMine Repair] Начинаю починку");
                    phase = RepairPhase.SWAPPING_PICKAXE_AND_XP;
                }
            }
            case SWAPPING_PICKAXE_AND_XP -> tickSwappingPickaxeAndXp(now);
            case THROWING_XP -> tickThrowingXp(now);
            case RETURNING_PICKAXE -> tickReturningPickaxe(now);
            case DONE_REPAIR -> { return true; }
            default -> phase = RepairPhase.CLEARING_INVENTORY;
        }

        return phase == RepairPhase.DONE_REPAIR;
    }

    // ============================
    // === ОЧИСТКА ИНВЕНТАРЯ ===
    // ============================

    private void tickClearingInventory(long now) {
        if (hasAnyXpBottles() && findPickaxeInvIndex() >= 0) {
            ChatMessage.brandmessage("[AutoMine Repair] Есть опыт и кирка — чиню");
            phase = RepairPhase.SWAPPING_PICKAXE_AND_XP;
            return;
        }

        int freeSlots = countFreeSlots();
        if (freeSlots >= SLOTS_TO_CLEAR) {
            ChatMessage.brandmessage("[AutoMine Repair] Достаточно свободных слотов: " + freeSlots);
            phase = RepairPhase.OPENING_AUCTION;
            startBuying();
            return;
        }

        slotsToDrop.clear();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (isKeepItem(stack)) continue;
            slotsToDrop.add(i);
        }

        if (!slotsToDrop.isEmpty()) {
            ChatMessage.brandmessage("[AutoMine Repair] Выкидываю " + slotsToDrop.size() + " предметов");
            phase = RepairPhase.DROPPING_ITEMS;
            dropActionIndex = 0;
        } else {
            int slotsToFree = SLOTS_TO_CLEAR - freeSlots;
            if (slotsToFree > 0 && canConsolidateDiamonds()) {
                ChatMessage.brandmessage("[AutoMine Repair] Нет места — консолидирую до " + slotsToFree + " слотов");
                phase = RepairPhase.CONSOLIDATING_DIAMONDS;
                consolidateTicks = 0;
                consolidateTargetFree = SLOTS_TO_CLEAR;
            } else {
                ChatMessage.brandmessage("[AutoMine Repair] Не удалось освободить слоты, продолжаю");
                phase = RepairPhase.OPENING_AUCTION;
                startBuying();
            }
        }
    }

    private void tickDroppingItems(long now) {
        if (!canAct(now)) return;
        if (dropActionIndex >= slotsToDrop.size()) {
            phase = RepairPhase.CLEARING_INVENTORY;
            return;
        }
        int invIdx = slotsToDrop.get(dropActionIndex);
        if (mc.player.getInventory().getStack(invIdx).isEmpty()) {
            dropActionIndex++;
            return;
        }
        Slot slot = getSlotFromInvIndex(invIdx);
        if (slot != null) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot.id, 1, SlotActionType.THROW, mc.player);
        }
        dropActionIndex++;
        lastActionTime = now;
    }

    private void tickConsolidatingDiamonds(long now) {
        if (!canAct(now)) return;
        if (countFreeSlots() >= consolidateTargetFree) {
            phase = RepairPhase.CLEARING_INVENTORY;
            return;
        }
        consolidateTicks++;
        if (consolidateTicks > 30) {
            phase = RepairPhase.CLEARING_INVENTORY;
            return;
        }
        Slot singleSlot = findSingleDiamondSlot();
        Slot stackSlot = findDiamondStackWithSpace();
        if (singleSlot == null || stackSlot == null || singleSlot.id == stackSlot.id) {
            phase = RepairPhase.CLEARING_INVENTORY;
            return;
        }
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, singleSlot.id, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, stackSlot.id, 0, SlotActionType.PICKUP, mc.player);
        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, singleSlot.id, 0, SlotActionType.PICKUP, mc.player);
        }
        lastActionTime = now;
    }

    private int countFreeSlots() {
        int free = 0;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) free++;
        }
        return free;
    }

    /** Есть ли хотя бы 1 пузырёк опыта. */
    private boolean hasAnyXpBottles() {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.EXPERIENCE_BOTTLE)
                return true;
        }
        return false;
    }

    /** Индекс кирки в инвентаре (0-35), -1 если нет. */
    private int findPickaxeInvIndex() {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof PickaxeItem)
                return i;
        }
        return -1;
    }

    /** Получить Slot из currentScreenHandler по индексу инвентаря (0-35). */
    private Slot getSlotFromInvIndex(int invIndex) {
        if (mc.player.currentScreenHandler == null || invIndex < 0 || invIndex >= 36) return null;
        var inv = mc.player.getInventory();
        for (Slot slot : mc.player.currentScreenHandler.slots) {
            if (slot.inventory == inv && slot.getIndex() == invIndex) return slot;
        }
        return null;
    }

    private boolean isKeepItem(ItemStack stack) {
        if (stack.getItem() == Items.DIAMOND) return true;
        if (stack.getItem() instanceof PickaxeItem) return true;
        if (stack.getItem() == Items.EXPERIENCE_BOTTLE) return true;
        return false;
    }

    private boolean canConsolidateDiamonds() {
        return findSingleDiamondSlot() != null && findDiamondStackWithSpace() != null;
    }

    private Slot findSingleDiamondSlot() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.DIAMOND && stack.getCount() == 1) {
                Slot slot = getSlotFromInvIndex(i);
                if (slot != null) return slot;
            }
        }
        return null;
    }

    private Slot findDiamondStackWithSpace() {
        Slot best = null;
        int bestCount = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.DIAMOND && stack.getCount() < 64 && stack.getCount() > bestCount) {
                Slot slot = getSlotFromInvIndex(i);
                if (slot != null) {
                    best = slot;
                    bestCount = stack.getCount();
                }
            }
        }
        return best;
    }

    // ============================
    // === ПОКУПКА ОПЫТА ===
    // ============================

    /** Начать процесс покупки (внутренний вызов). */
    private void startBuying() {
        ChatMessage.brandmessage("[AutoMine Repair] Старт покупки пузырьков опыта");
        phase = RepairPhase.OPENING_AUCTION;
        phaseStartTime = System.currentTimeMillis();
        lastActionTime = 0;
        lastPurchaseTime = 0;
        stacksBought = 0;
        auctionOpened = false;
        refreshCount = 0;
    }

    /**
     * Тик покупки — вызывается из AutoMineController в состоянии BUYING_XP.
     * @return true когда покупка завершена (успех или таймаут)
     */
    public boolean tickBuying() {
        if (mc.player == null) return true;

        long now = System.currentTimeMillis();

        // Таймаут
        if (now - phaseStartTime > BUY_TIMEOUT && phase != RepairPhase.DONE_BUYING) {
            ChatMessage.brandmessage("[AutoMine Repair] Покупка: таймаут " + (BUY_TIMEOUT / 1000) + " сек");
            phase = RepairPhase.DONE_BUYING;
        }

        switch (phase) {
            case OPENING_AUCTION -> {
                if (canAct(now)) {
                    if (mc.currentScreen != null) {
                        mc.player.closeHandledScreen();
                        lastActionTime = now;
                        return false;
                    }
                    ChatMessage.brandmessage("[AutoMine Repair] Открываю аукцион: /ah search Пузырёк опыта");
                    sendCommand("/ah search \u041f\u0443\u0437\u044b\u0440\u0451\u043a \u043e\u043f\u044b\u0442\u0430");
                    phase = RepairPhase.WAITING_AUCTION;
                    phaseStartTime = now;
                    lastActionTime = now;
                }
            }
            case WAITING_AUCTION -> {
                long elapsed = now - phaseStartTime;
                if (mc.currentScreen instanceof GenericContainerScreen && elapsed >= POST_COMMAND_DELAY_MS) {
                    auctionOpened = true;
                    ChatMessage.brandmessage("[AutoMine Repair] Аукцион открыт, ищу пузырьки");
                    phase = RepairPhase.SCANNING_BUYING;
                    lastActionTime = now;
                } else if (elapsed > AUCTION_LOAD_DELAY) {
                    ChatMessage.brandmessage("[AutoMine Repair] Аукцион не открылся, повтор");
                    phase = RepairPhase.OPENING_AUCTION;
                    phaseStartTime = now;
                }
            }
            case SCANNING_BUYING -> {
                if (!(mc.currentScreen instanceof GenericContainerScreen container)) {
                    phase = RepairPhase.OPENING_AUCTION;
                    return false;
                }

                if (stacksBought >= XP_STACKS_TO_BUY) {
                    ChatMessage.brandmessage("[AutoMine Repair] Куплено стаков: " + stacksBought);
                    phase = RepairPhase.DONE_BUYING;
                    return false;
                }

                if (!canAct(now)) return false;
                if (lastPurchaseTime > 0 && now - lastPurchaseTime < POST_PURCHASE_DELAY_MS) return false;

                List<Slot> slots = container.getScreenHandler().slots;
                int syncId = container.getScreenHandler().syncId;

                int bestSlot = -1;
                int bestPrice = Integer.MAX_VALUE;

                int maxSlot = Math.min(45, slots.size());
                for (int i = 0; i < maxSlot; i++) {
                    Slot slot = slots.get(i);
                    if (slot == null) continue;
                    ItemStack stack = slot.getStack();
                    if (stack == null || stack.isEmpty()) continue;
                    if (stack.getItem() != Items.EXPERIENCE_BOTTLE) continue;
                    if (stack.getCount() < MIN_STACK_SIZE) continue;

                    int price = extractPrice(stack);
                    if (price <= 0 || price > MAX_XP_STACK_PRICE) continue;

                    if (price < bestPrice) {
                        bestPrice = price;
                        bestSlot = i;
                    }
                }

                if (bestSlot >= 0) {
                    mc.interactionManager.clickSlot(syncId, bestSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
                    stacksBought++;
                    lastPurchaseTime = now;
                    ChatMessage.brandmessage("[AutoMine Repair] Куплен стак " + stacksBought + "/" + XP_STACKS_TO_BUY + ", цена " + bestPrice + ", жду 1 сек");
                    lastActionTime = now;
                } else {
                    if (refreshCount < 5) {
                        int containerSize = slots.size() - 36;
                        int refreshSlot = containerSize > 0 ? containerSize - 5 : 49;
                        mc.interactionManager.clickSlot(syncId, refreshSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
                        refreshCount++;
                        ChatMessage.brandmessage("[AutoMine Repair] Обновляю аукцион, попытка " + refreshCount);
                        lastActionTime = now;
                    } else {
                        ChatMessage.brandmessage("[AutoMine Repair] Не найдено подходящих стаков, куплено " + stacksBought);
                        phase = RepairPhase.DONE_BUYING;
                    }
                }
            }
            case DONE_BUYING -> {
                if (mc.currentScreen != null) {
                    mc.player.closeHandledScreen();
                }
                ChatMessage.brandmessage("[AutoMine Repair] Покупка завершена, стаков: " + stacksBought + ", жду синхронизацию...");
                phase = RepairPhase.WAITING_AFTER_BUY;
                phaseStartTime = now;
                return false;
            }
            default -> {
                return true;
            }
        }

        return false;
    }

    /** Извлекает цену из лора предмета. */
    private int extractPrice(ItemStack stack) {
        int price = AuctionUtils.getPrice(stack);
        if (price > 0) return price;

        var lore = stack.get(DataComponentTypes.LORE);
        if (lore != null && !lore.lines().isEmpty()) {
            for (Text line : lore.lines()) {
                String text = line.getString();
                Matcher matcher = PRICE_PATTERN.matcher(text);
                if (matcher.find()) {
                    for (int g = 1; g <= 3; g++) {
                        String priceStr = matcher.group(g);
                        if (priceStr != null) {
                            try {
                                String clean = priceStr.replaceAll("[\\s,.]", "");
                                return Integer.parseInt(clean);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        }

        String name = stack.getName().getString();
        Matcher m = PRICE_PATTERN.matcher(name);
        if (m.find()) {
            for (int g = 1; g <= 3; g++) {
                String priceStr = m.group(g);
                if (priceStr != null) {
                    try {
                        String clean = priceStr.replaceAll("[\\s,.]", "");
                        return Integer.parseInt(clean);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        return -1;
    }

    // ============================
    // === КИРКА В ЛЕВУЮ, XP В ПРАВУЮ, БРОСОК ===
    // ============================

    private void tickSwappingPickaxeAndXp(long now) {
        if (mc.currentScreen != null) {
            mc.player.closeHandledScreen();
            lastActionTime = now;
            return;
        }
        if (!canAct(now)) return;

        boolean pickaxeInOffhand = mc.player.getOffHandStack().getItem() instanceof PickaxeItem;
        int pickaxeInv = findPickaxeInvIndex();
        int xpInv = findXpBottleSlot();
        Slot pickaxeSlot = pickaxeInv >= 0 ? getSlotFromInvIndex(pickaxeInv) : null;
        Slot xpSlot = xpInv >= 0 ? getSlotFromInvIndex(xpInv) : null;

        boolean hasPickaxe = pickaxeInOffhand || pickaxeSlot != null;
        if (!hasPickaxe || xpSlot == null) {
            if (noItemsRetryCount < 2 && stacksBought > 0) {
                noItemsRetryCount++;
                ChatMessage.brandmessage("[AutoMine Repair] Жду предметы, повтор " + noItemsRetryCount);
                phase = RepairPhase.WAITING_AFTER_BUY;
                phaseStartTime = now;
            } else {
                ChatMessage.brandmessage("[AutoMine Repair] Нет кирки или пузырьков");
                phase = RepairPhase.DONE_REPAIR;
            }
            return;
        }

        noItemsRetryCount = 0;

        boolean xpInMainhand = mc.player.getMainHandStack().getItem() == Items.EXPERIENCE_BOTTLE;

        if (pickaxeInOffhand && xpInMainhand) {
            ChatMessage.brandmessage("[AutoMine Repair] Кирка в левой, опыт в правой — кидаю");
            phase = RepairPhase.THROWING_XP;
            lastThrowTime = now;
            return;
        }

        if (!pickaxeInOffhand) {
            InventoryTask.swapHand(pickaxeSlot, Hand.OFF_HAND, false, true);
            lastActionTime = now;
            return;
        }

        if (!xpInMainhand) {
            if (xpInv < 9) {
                InventoryTask.switchTo(xpInv);
            } else {
                int sel = mc.player.getInventory().selectedSlot;
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, xpSlot.id, sel, SlotActionType.SWAP, mc.player);
            }
            lastActionTime = now;
        }
    }

    private void tickThrowingXp(long now) {
        if (needReleaseUseKey) {
            mc.options.useKey.setPressed(false);
            needReleaseUseKey = false;
        }

        if (isPickaxeFullyRepaired()) {
            ChatMessage.brandmessage("[AutoMine Repair] Кирка починена");
            phase = RepairPhase.RETURNING_PICKAXE;
            return;
        }

        if (mc.player.getMainHandStack().getItem() != Items.EXPERIENCE_BOTTLE) {
            int nextXp = findXpBottleSlot();
            if (nextXp == -1) {
                ChatMessage.brandmessage("[AutoMine Repair] Пузырьки закончились");
                phase = RepairPhase.RETURNING_PICKAXE;
                return;
            }
            phase = RepairPhase.SWAPPING_PICKAXE_AND_XP;
            return;
        }

        long delay = throwTickCounter == 0 ? POST_SWAP_DELAY_MS : THROW_DELAY;
        if (now - lastThrowTime >= delay) {
            mc.options.useKey.setPressed(true);
            needReleaseUseKey = true;
            lastThrowTime = now;
            throwTickCounter++;
        }
    }

    private void tickReturningPickaxe(long now) {
        if (!canAct(now)) return;

        if (mc.player.getOffHandStack().getItem() instanceof PickaxeItem) {
            var slots = mc.player.currentScreenHandler.slots;
            Slot offhandSlot = (slots.size() >= 46) ? slots.get(45) : null;
            if (offhandSlot != null) {
                InventoryTask.swapHand(offhandSlot, Hand.MAIN_HAND, false, true);
                ChatMessage.brandmessage("[AutoMine Repair] Кирка в основную руку, продолжаю майнить");
            }
        }
        phase = RepairPhase.DONE_REPAIR;
    }

    /**
     * Ищет слот с пузырьками опыта в инвентаре.
     * @return слот (0-35), -1 если не найдено
     */
    private int findXpBottleSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.EXPERIENCE_BOTTLE && !stack.isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /** Есть ли пузырьки опыта в инвентаре? */
    public boolean hasXpBottles() {
        return findXpBottleSlot() != -1;
    }

    /** Сброс всего состояния. */
    public void reset() {
        phase = RepairPhase.IDLE;
        stacksBought = 0;
        auctionOpened = false;
        refreshCount = 0;
        throwTickCounter = 0;
        lastThrowTime = 0;
    }

    private boolean canAct(long now) {
        return now - lastActionTime >= AUCTION_ACTION_DELAY;
    }

    private void sendCommand(String command) {
        if (mc.player != null && mc.getNetworkHandler() != null) {
            String cmd = command.startsWith("/") ? command.substring(1) : command;
            mc.getNetworkHandler().sendCommand(cmd);
        }
    }
}
