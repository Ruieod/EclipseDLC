package code.essence.features.impl.render;

import code.essence.utils.features.price.PriceParser;
import code.essence.utils.client.logs.Logger;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.BindSetting;
import code.essence.features.module.setting.implement.BooleanSetting;
import code.essence.features.module.setting.implement.ColorSetting;
import code.essence.features.module.setting.implement.RadioSetting;
import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.events.keyboard.KeyEvent;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.math.script.Script;
import code.essence.events.container.HandledScreenEvent;
import code.essence.events.packet.PacketEvent;
import code.essence.events.player.TickEvent;
import code.essence.display.screens.clickgui.components.implement.autobuy.util.AuctionUtils;
import code.essence.utils.client.packet.network.Network;

import java.awt.*;
import java.util.*;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuctionHelper extends Module implements QuickImports {
    PriceParser auctionPriceParser = new PriceParser();
    Script script = new Script();
    @NonFinal
    Slot cheapestSlot, costEffectiveSlot;
    int[] RED_GREEN_COLORS = {0xFF4BFF4B, 0xFFFF4B4B};

    SelectSetting modeSetting = new SelectSetting("Мод", "Выбор мода сервера")
            .value("ФанТайм", "СпукиТайм")
            .selected("ФанТайм");

    BooleanSetting showPricePerItem = new BooleanSetting("Цена за 1 предмет", "Показывать цену за 1 штуку в слотах с количеством > 1");

    BooleanSetting filterSetting = new BooleanSetting("Фильтр", "Фильтрация предметов по зачарованиям");

    RadioSetting filterTypeSetting = new RadioSetting("Настройки фильтра", "Тип предмета для фильтрации",
            new String[]{"Броня", "Меч", "Кирка"}, "Броня")
            .visible(() -> filterSetting.isValue());

    
    BooleanSetting armorProtectionSetting = new BooleanSetting("Защита", "Требовать зачарование защиты")
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Броня"));
    SliderSettings armorProtectionLevelSetting = new SliderSettings("Уровень защиты", "Минимальный уровень защиты")
            .setValue(5F).range(1F, 5F).step(1F)
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Броня") && armorProtectionSetting.isValue());

    BooleanSetting armorUnbreakingSetting = new BooleanSetting("Прочность", "Требовать зачарование прочности")
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Броня"));
    SliderSettings armorUnbreakingLevelSetting = new SliderSettings("Уровень прочности", "Минимальный уровень прочности")
            .setValue(5F).range(1F, 5F).step(1F)
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Броня") && armorUnbreakingSetting.isValue());

    BooleanSetting armorMendingSetting = new BooleanSetting("Починка", "Требовать зачарование починки")
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Броня"));

    BooleanSetting armorDepthStriderSetting = new BooleanSetting("Подводная ходьба", "Требовать зачарование подводной ходьбы (только для ботинок)")
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Броня"));
    SliderSettings armorDepthStriderLevelSetting = new SliderSettings("Уровень подводной ходьбы", "Минимальный уровень подводной ходьбы")
            .setValue(3F).range(1F, 3F).step(1F)
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Броня") && armorDepthStriderSetting.isValue());

    BooleanSetting armorThornsSetting = new BooleanSetting("Шипы", "Требовать зачарование шипов")
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Броня"));

    
    BooleanSetting swordSharpnessSetting = new BooleanSetting("Острота", "Требовать зачарование остроты")
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Меч"));
    SliderSettings swordSharpnessLevelSetting = new SliderSettings("Уровень остроты", "Минимальный уровень остроты")
            .setValue(7F).range(1F, 7F).step(1F)
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Меч") && swordSharpnessSetting.isValue());

    BooleanSetting swordKnockbackSetting = new BooleanSetting("Отдача", "Требовать зачарование отдачи")
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Меч"));
    SliderSettings swordKnockbackLevelSetting = new SliderSettings("Уровень отдачи", "Минимальный уровень отдачи")
            .setValue(2F).range(1F, 2F).step(1F)
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Меч") && swordKnockbackSetting.isValue());

    BooleanSetting swordLootingSetting = new BooleanSetting("Добыча", "Требовать зачарование добычи")
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Меч"));
    SliderSettings swordLootingLevelSetting = new SliderSettings("Уровень добычи", "Минимальный уровень добычи")
            .setValue(5F).range(1F, 5F).step(1F)
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Меч") && swordLootingSetting.isValue());

    
    BooleanSetting pickaxeEfficiencySetting = new BooleanSetting("Эффективность", "Требовать зачарование эффективности")
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Кирка"));
    SliderSettings pickaxeEfficiencyLevelSetting = new SliderSettings("Уровень эффективности", "Минимальный уровень эффективности")
            .setValue(7F).range(1F, 7F).step(1F)
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Кирка") && pickaxeEfficiencySetting.isValue());

    BooleanSetting pickaxeFortuneSetting = new BooleanSetting("Удача", "Требовать зачарование удачи")
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Кирка"));
    SliderSettings pickaxeFortuneLevelSetting = new SliderSettings("Уровень удачи", "Минимальный уровень удачи")
            .setValue(5F).range(1F, 5F).step(1F)
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Кирка") && pickaxeFortuneSetting.isValue());

    BooleanSetting pickaxeUnbreakingSetting = new BooleanSetting("Прочность", "Требовать зачарование прочности")
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Кирка"));
    SliderSettings pickaxeUnbreakingLevelSetting = new SliderSettings("Уровень прочности", "Минимальный уровень прочности")
            .setValue(5F).range(1F, 5F).step(1F)
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Кирка") && pickaxeUnbreakingSetting.isValue());

    BooleanSetting pickaxeSilkTouchSetting = new BooleanSetting("Шелковое касание", "Требовать зачарование шелкового касания")
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Кирка"));

    BooleanSetting pickaxeMendingSetting = new BooleanSetting("Починка", "Требовать зачарование починки")
            .visible(() -> filterSetting.isValue() && filterTypeSetting.get().equals("Кирка"));

    ColorSetting cheapestItemColorSetting = new ColorSetting("Дешевый предмет", "Цвет подсветки для предмета с наименьшей ценой.")
            .setColor(0xFF4BFF4B).presets(RED_GREEN_COLORS);

    ColorSetting costEffectiveItemColorSetting = new ColorSetting("Выгодный предмет", "Цвет подсветки для лучшего предмета.")
            .setColor((new Color(0, 0, 255,255).getRGB()));

    BindSetting ahSearchBind = new BindSetting("Поиск предмета по '/ah'", "Поиск предмета в руке через /ah search");

    BooleanSetting logNbtSetting = new BooleanSetting("NBT", "Логировать NBT предметов");

    BooleanSetting itemReListingSetting = new BooleanSetting("Перевыставление предметов", "Автоматически перевыставляет предметы из хранилища")
            .setValue(false);

    @NonFinal
    Map<Integer, Integer> pricePerItemCache = new HashMap<>();
    @NonFinal
    GenericContainerScreen lastScreen = null;

    @NonFinal
    private long lastStorageClick = -1;
    private static final long STORAGE_CLICK_DELAY = 60100;
    @NonFinal
    private boolean waitingForStorage = false;

    public AuctionHelper() {
        super("AuctionHelper", "AuctionHelper", ModuleCategory.RENDER);
        setup(modeSetting, showPricePerItem, filterSetting, filterTypeSetting,
                armorProtectionSetting, armorProtectionLevelSetting,
                armorUnbreakingSetting, armorUnbreakingLevelSetting,
                armorDepthStriderSetting, armorDepthStriderLevelSetting,
                armorMendingSetting,
                armorThornsSetting,
                swordSharpnessSetting, swordSharpnessLevelSetting,
                swordKnockbackSetting, swordKnockbackLevelSetting,
                swordLootingSetting, swordLootingLevelSetting,
                pickaxeEfficiencySetting, pickaxeEfficiencyLevelSetting,
                pickaxeFortuneSetting, pickaxeFortuneLevelSetting,
                pickaxeUnbreakingSetting, pickaxeUnbreakingLevelSetting,
                pickaxeSilkTouchSetting,
                pickaxeMendingSetting,
                cheapestItemColorSetting, costEffectiveItemColorSetting,
                ahSearchBind, logNbtSetting, itemReListingSetting);
    }


    @EventHandler
    public void onPacket(PacketEvent e) {
        if (!isState()) return; 
        if (e.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket packet) {
            script.cleanup().addTickStep(0, () -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    String title = screen.getTitle().getString();

                    
                    if (title.contains("Аукцион") || title.contains("Поиск")) {
                        if (logNbtSetting.isValue()) {
                            int containerSlotCount = 0;
                            int loggedCount = 0;

                            for (int i = 0; i <= 53 && i < screen.getScreenHandler().slots.size(); i++) {
                                Slot slot = screen.getScreenHandler().slots.get(i);
                                if (slot != null) {
                                    ItemStack stack = slot.getStack();
                                    if (stack != null && !stack.isEmpty()) {
                                        containerSlotCount++;

                                        
                                        boolean isPlayerInv = mc.player != null && slot.inventory.equals(mc.player.getInventory());
                                        if (!isPlayerInv) {
                                            Logger.info("[AuctionHelper] DEBUG: Логирую слот контейнера ID: " + i);
                                            logItemData(stack);
                                            loggedCount++;
                                        }
                                    }
                                }
                            }

                            Logger.info("[AuctionHelper] DEBUG: Всего предметов в контейнере (0-53): " + containerSlotCount + ", залогировано: " + loggedCount);
                        }
                    }

                    cheapestSlot = findSlotWithLowestPrice(screen.getScreenHandler().slots);
                    costEffectiveSlot = findSlotWithBestPricePerItem(screen.getScreenHandler().slots);
                }
            });
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        script.update();

        if (!itemReListingSetting.isValue()) return;

        
        if (!Network.isFunTime() && !Network.isSpookyTime()) return;

        if (mc.currentScreen instanceof GenericContainerScreen screen) {
            if (screen == null) return;
            String title = screen.getTitle().getString().toLowerCase();

            if (!title.contains("хранилище")) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            if (lastStorageClick == -1 || currentTime - lastStorageClick >= STORAGE_CLICK_DELAY) {
                if (screen.getScreenHandler() != null && screen.getScreenHandler().slots.size() > 52) {
                    try {
                        Thread.sleep(70);
                        mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, 52, 0, SlotActionType.QUICK_MOVE, mc.player);
                        lastStorageClick = currentTime;
                        waitingForStorage = false;
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (e.isKeyDown(ahSearchBind.getKey()) && ahSearchBind.getKey() != 0) {
            if (mc.player == null) return;

            ItemStack itemStack = mc.player.getMainHandStack();
            if (itemStack == null || itemStack.isEmpty()) {
                Logger.info("[AuctionHelper] Возьмите предмет в руку");
                return;
            }

            String searchCommand = getSearchCommand(itemStack);
            if (searchCommand != null) {
                Logger.info("[AuctionHelper] Поиск предмета: " + searchCommand);
            } else {
                Logger.info("[AuctionHelper] Не удалось получить название предмета");
            }
        }
    }


    @EventHandler
    public void onHandledScreen(HandledScreenEvent e) {
        DrawContext context = e.getDrawContext();
        MatrixStack matrix = context.getMatrices();

        if (mc.currentScreen instanceof GenericContainerScreen screen) {
            String title = screen.getTitle().getString();
            if (!title.contains("Аукцион") && !title.contains("Поиск")) {
                if (lastScreen != null) {
                    pricePerItemCache.clear();
                    lastScreen = null;
                }
                return;
            }

            if (lastScreen != screen) {
                pricePerItemCache.clear();
                lastScreen = screen;
            }

            int offsetX = (screen.width - e.getBackgroundWidth()) / 2;
            int offsetY = (screen.height - e.getBackgroundHeight()) / 2;

            int cheapItemColor = getBlinkingColor(cheapestItemColorSetting.getColor());
            int cheapestQuantityColor = getBlinkingColor(costEffectiveItemColorSetting.getColor());

            matrix.push();
            matrix.translate(offsetX, offsetY, 0);
            if (cheapestSlot != null) {
                
                if (cheapestSlot.getStack().getCount() == 1) {
                    highlightSlot(context, cheapestSlot, cheapItemColor);
                } else {
                    
                    if (cheapestSlot != costEffectiveSlot && costEffectiveSlot != null) {
                        highlightSlot(context, cheapestSlot, cheapItemColor);
                    }
                    
                    if (costEffectiveSlot != null) {
                        highlightSlot(context, costEffectiveSlot, cheapestQuantityColor);
                    }
                }
            } else if (costEffectiveSlot != null) {
                
                highlightSlot(context, costEffectiveSlot, cheapestQuantityColor);
            }
            matrix.pop();

            
            if (showPricePerItem.isValue()) {
                for (Slot slot : screen.getScreenHandler().slots) {
                    if (slot.getStack().isEmpty() || slot.getStack().getCount() <= 1) continue;
                    addPricePerItemToLore(slot.getStack());
                }
            }
        } else {
            if (lastScreen != null) {
                pricePerItemCache.clear();
                lastScreen = null;
            }
        }
    }

    private void addPricePerItemToLore(ItemStack stack) {
        int totalPrice = getPrice(stack);
        if (totalPrice <= 0) return;

        int count = stack.getCount();
        int pricePerItem = totalPrice / count;

        String pricePerItemText = "§a$ §fЦена за 1 шт: §a$" + formatPriceWithDots(pricePerItem);

        var existingLore = stack.get(DataComponentTypes.LORE);
        List<Text> loreLines = new ArrayList<>();

        if (existingLore != null) {
            
            for (Text line : existingLore.lines()) {
                String lineStr = line.getString();
                if (lineStr.contains("Цена за 1 шт:")) {
                    return; 
                }
                loreLines.add(line);
            }

            
            boolean foundPrice = false;
            List<Text> newLoreLines = new ArrayList<>();
            for (Text line : loreLines) {
                newLoreLines.add(line);
                String lineStr = line.getString();
                if (!foundPrice && (lineStr.contains("Цена") || lineStr.contains("$") && lineStr.matches(".*\\$\\s*[\\d,.]+.*"))) {
                    newLoreLines.add(Text.literal(pricePerItemText));
                    foundPrice = true;
                }
            }

            
            if (!foundPrice) {
                newLoreLines.add(Text.literal(pricePerItemText));
            }

            stack.set(DataComponentTypes.LORE, new LoreComponent(newLoreLines));
        } else {
            loreLines.add(Text.literal(pricePerItemText));
            stack.set(DataComponentTypes.LORE, new LoreComponent(loreLines));
        }
    }

    private String formatPriceWithDots(int price) {
        StringBuilder result = new StringBuilder();
        String priceStr = String.valueOf(price);
        int count = 0;
        for (int i = priceStr.length() - 1; i >= 0; i--) {
            if (count > 0 && count % 3 == 0) {
                result.insert(0, '.');
            }
            result.insert(0, priceStr.charAt(i));
            count++;
        }
        return result.toString();
    }


    private int getBlinkingColor(int color) {
        float alpha = (float) Math.abs(Math.sin((double) System.currentTimeMillis() / 3 * Math.PI / 180));
        return ColorAssist.multAlpha(color, alpha);
    }


    private Slot findSlotWithLowestPrice(List<Slot> slots) {
        
        return slots.stream()
                .filter(this::hasValidPrice)
                .min(Comparator.comparingDouble(slot -> {
                    int price = getPrice(slot.getStack());
                    int count = slot.getStack().getCount();
                    return count > 0 ? (double) price / count : Double.MAX_VALUE;
                }))
                .orElse(null);
    }


    private Slot findSlotWithBestPricePerItem(List<Slot> slots) {
        
        if (filterSetting.isValue()) {
            Slot filteredSlot = slots.stream()
                    .filter(this::hasValidPrice)
                    .filter(this::matchesFilter)
                    .min(Comparator.comparingDouble(slot -> {
                        int price = getPrice(slot.getStack());
                        int count = slot.getStack().getCount();
                        return count > 0 ? (double) price / count : Double.MAX_VALUE;
                    }))
                    .orElse(null);

            if (filteredSlot != null) {
                return filteredSlot;
            }
        }

        
        
        Slot cheapest = findSlotWithLowestPrice(slots);
        if (cheapest != null && cheapest.getStack().getCount() == 1) {
            return null; 
        }

        return slots.stream()
                .filter(this::hasValidPrice)
                .min(Comparator.comparingDouble(slot -> {
                    int price = getPrice(slot.getStack());
                    int count = slot.getStack().getCount();
                    return count > 0 ? (double) price / count : Double.MAX_VALUE;
                }))
                .orElse(null);
    }


    private boolean hasValidPrice(Slot slot) {
        return getPrice(slot.getStack()) >= 0;
    }

    private boolean matchesFilter(Slot slot) {
        if (!filterSetting.isValue()) return true;

        ItemStack stack = slot.getStack();
        if (stack.isEmpty()) return false;

        Item item = stack.getItem();
        String filterType = filterTypeSetting.get();

        
        if (item == Items.SPLASH_POTION) {
            return matchesPotionEffects(stack);
        }

        
        if (filterType.equals("Броня")) {
            return isArmor(item) && matchesArmorEnchants(stack);
        } else if (filterType.equals("Меч")) {
            return isSword(item) && matchesSwordEnchants(stack);
        } else if (filterType.equals("Кирка")) {
            return isPickaxe(item) && matchesPickaxeEnchants(stack);
        }

        return false;
    }

    private boolean isArmor(Item item) {
        return item == Items.NETHERITE_HELMET || item == Items.DIAMOND_HELMET ||
                item == Items.NETHERITE_CHESTPLATE || item == Items.DIAMOND_CHESTPLATE ||
                item == Items.NETHERITE_LEGGINGS || item == Items.DIAMOND_LEGGINGS ||
                item == Items.NETHERITE_BOOTS || item == Items.DIAMOND_BOOTS;
    }

    private boolean isSword(Item item) {
        return item == Items.NETHERITE_SWORD || item == Items.DIAMOND_SWORD;
    }

    private boolean isPickaxe(Item item) {
        return item == Items.NETHERITE_PICKAXE || item == Items.DIAMOND_PICKAXE;
    }

    private boolean matchesArmorEnchants(ItemStack stack) {
        if (mc.world == null) return false;

        Registry<Enchantment> enchantRegistry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        if (armorProtectionSetting.isValue()) {
            RegistryEntry<Enchantment> protectionEntry = enchantRegistry.getEntry(Enchantments.PROTECTION.getValue()).orElse(null);
            if (protectionEntry == null || EnchantmentHelper.getLevel(protectionEntry, stack) < armorProtectionLevelSetting.getValue()) return false;
        }

        if (armorUnbreakingSetting.isValue()) {
            RegistryEntry<Enchantment> unbreakingEntry = enchantRegistry.getEntry(Enchantments.UNBREAKING.getValue()).orElse(null);
            if (unbreakingEntry == null || EnchantmentHelper.getLevel(unbreakingEntry, stack) < armorUnbreakingLevelSetting.getValue()) return false;
        }

        if (armorMendingSetting.isValue()) {
            RegistryEntry<Enchantment> mendingEntry = enchantRegistry.getEntry(Enchantments.MENDING.getValue()).orElse(null);
            if (mendingEntry == null || EnchantmentHelper.getLevel(mendingEntry, stack) < 1) return false;
        }

        if (armorDepthStriderSetting.isValue()) {
            Item item = stack.getItem();
            if (item == Items.NETHERITE_BOOTS || item == Items.DIAMOND_BOOTS) {
                RegistryEntry<Enchantment> depthStriderEntry = enchantRegistry.getEntry(Enchantments.DEPTH_STRIDER.getValue()).orElse(null);
                if (depthStriderEntry == null || EnchantmentHelper.getLevel(depthStriderEntry, stack) < armorDepthStriderLevelSetting.getValue()) return false;
            }
        }

        if (armorThornsSetting.isValue()) {
            RegistryEntry<Enchantment> thornsEntry = enchantRegistry.getEntry(Enchantments.THORNS.getValue()).orElse(null);
            if (thornsEntry == null || EnchantmentHelper.getLevel(thornsEntry, stack) < 1) return false;
        } else {
            RegistryEntry<Enchantment> thornsEntry = enchantRegistry.getEntry(Enchantments.THORNS.getValue()).orElse(null);
            if (thornsEntry != null && EnchantmentHelper.getLevel(thornsEntry, stack) > 0) return false;
        }

        return true;
    }

    private boolean matchesSwordEnchants(ItemStack stack) {
        if (mc.world == null) return false;

        Registry<Enchantment> enchantRegistry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        if (swordSharpnessSetting.isValue()) {
            RegistryEntry<Enchantment> sharpnessEntry = enchantRegistry.getEntry(Enchantments.SHARPNESS.getValue()).orElse(null);
            if (sharpnessEntry == null || EnchantmentHelper.getLevel(sharpnessEntry, stack) < swordSharpnessLevelSetting.getValue()) return false;
        }

        if (swordKnockbackSetting.isValue()) {
            RegistryEntry<Enchantment> knockbackEntry = enchantRegistry.getEntry(Enchantments.KNOCKBACK.getValue()).orElse(null);
            if (knockbackEntry == null || EnchantmentHelper.getLevel(knockbackEntry, stack) < swordKnockbackLevelSetting.getValue()) return false;
        }

        if (swordLootingSetting.isValue()) {
            RegistryEntry<Enchantment> lootingEntry = enchantRegistry.getEntry(Enchantments.LOOTING.getValue()).orElse(null);
            if (lootingEntry == null || EnchantmentHelper.getLevel(lootingEntry, stack) < swordLootingLevelSetting.getValue()) return false;
        }

        return true;
    }

    private boolean matchesPickaxeEnchants(ItemStack stack) {
        if (mc.world == null) return false;

        Registry<Enchantment> enchantRegistry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        if (pickaxeEfficiencySetting.isValue()) {
            RegistryEntry<Enchantment> efficiencyEntry = enchantRegistry.getEntry(Enchantments.EFFICIENCY.getValue()).orElse(null);
            if (efficiencyEntry == null || EnchantmentHelper.getLevel(efficiencyEntry, stack) < pickaxeEfficiencyLevelSetting.getValue()) return false;
        }

        if (pickaxeFortuneSetting.isValue()) {
            RegistryEntry<Enchantment> fortuneEntry = enchantRegistry.getEntry(Enchantments.FORTUNE.getValue()).orElse(null);
            if (fortuneEntry == null || EnchantmentHelper.getLevel(fortuneEntry, stack) < pickaxeFortuneLevelSetting.getValue()) return false;
        }

        if (pickaxeUnbreakingSetting.isValue()) {
            RegistryEntry<Enchantment> unbreakingEntry = enchantRegistry.getEntry(Enchantments.UNBREAKING.getValue()).orElse(null);
            if (unbreakingEntry == null || EnchantmentHelper.getLevel(unbreakingEntry, stack) < pickaxeUnbreakingLevelSetting.getValue()) return false;
        }

        if (pickaxeSilkTouchSetting.isValue()) {
            RegistryEntry<Enchantment> silkTouchEntry = enchantRegistry.getEntry(Enchantments.SILK_TOUCH.getValue()).orElse(null);
            if (silkTouchEntry == null || EnchantmentHelper.getLevel(silkTouchEntry, stack) < 1) return false;
        }

        if (pickaxeMendingSetting.isValue()) {
            RegistryEntry<Enchantment> mendingEntry = enchantRegistry.getEntry(Enchantments.MENDING.getValue()).orElse(null);
            if (mendingEntry == null || EnchantmentHelper.getLevel(mendingEntry, stack) < 1) return false;
        }

        return true;
    }

    private int getPrice(ItemStack stack) {
        if (modeSetting.isSelected("ФанТайм")) {
            int price = auctionPriceParser.getPrice(stack);
            return price;
        } else if (modeSetting.isSelected("СпукиТайм")) {
            if (mc.currentScreen instanceof GenericContainerScreen screen) {
                return auctionPriceParser.getSpookyTimePrice(stack, screen);
            }
            return -1;
        }
        return -1;
    }

    
    private void logItemData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        String itemName = stack.getName().getString();
        Logger.info("[AuctionHelper] ===== Предмет: " + itemName + " =====");

        
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore != null && !lore.lines().isEmpty()) {
            StringBuilder loreBuilder = new StringBuilder();
            int lineNum = 0;
            for (Text line : lore.lines()) {
                loreBuilder.append("\n  [").append(lineNum++).append("] ").append(line.getString());
            }
            Logger.info("[AuctionHelper] Лор предмета:" + loreBuilder.toString());
        } else {
            Logger.info("[AuctionHelper] Лор отсутствует");
        }

        
        var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            var nbt = customData.copyNbt();
            if (nbt != null && !nbt.isEmpty()) {
                Logger.info("[AuctionHelper] NBT: " + nbt.toString());
            } else {
                Logger.info("[AuctionHelper] NBT отсутствует");
            }
        } else {
            Logger.info("[AuctionHelper] NBT отсутствует");
        }

        
        var components = stack.getComponents();
        if (components != null) {
            Logger.info("[AuctionHelper] Все компоненты: " + components.toString());
        } else {
            Logger.info("[AuctionHelper] Компоненты отсутствуют");
        }

        Logger.info("[AuctionHelper] ========================================");
    }


    private boolean isValidMultiItemSlot(Slot slot) {
        return hasValidPrice(slot) && slot.getStack().getCount() > 1;
    }


    private void highlightSlot(DrawContext context, Slot slot, int color) {
        if (slot != null) rectangle.render(ShapeProperties.create(context.getMatrices(), slot.x, slot.y, 16, 16).color(color).build());
    }

    private String getItemName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        try {
            return stack.getName().getString().replaceAll("§[0-9a-fk-or]", "");
        } catch (Exception e) {
            return null;
        }
    }

    private String getSearchCommand(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return null;

        
        if (Network.isHolyWorld()) {
            return "/ah search";
        }

        String itemName = getItemName(itemStack);
        if (itemName == null || itemName.isEmpty()) return null;

        
        String cleanItemName = itemName.replaceAll("^\\[★\\]\\s*", "")
                .replaceAll("\\[★\\]", "")  
                .replaceAll("xxx", "")
                .replaceAll("123", "")
                .trim()
                .replaceAll("\\s+", " ");

        
        var enchants = itemStack.get(DataComponentTypes.ENCHANTMENTS);
        boolean hasEnchantments = enchants != null && !enchants.isEmpty();

        
        if (!hasEnchantments && itemStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
            hasEnchantments = true;
        }

        if (itemStack.getItem() == Items.ENCHANTED_BOOK) {
            if (hasEnchantments) {
                for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
                    String enchantId = entry.getIdAsString().replace("minecraft:", "");
                    String enchantName = AuctionUtils.getEnchantmentName(enchantId);
                    if (enchantName != null) {
                        return "/ah search книга " + enchantName;
                    }
                    break; 
                }
            }

            return "/ah search Зачарованная книга";
        }


        
        String searchName = findSearchName(cleanItemName);
        if (searchName != null && !searchName.isEmpty()) {
            
            searchName = searchName.replaceAll("^\\[★\\]\\s*", "").replaceAll("\\[★\\]", "").trim();

            return "/ah search " + searchName;
        }
        return "/ah search " + cleanItemName;

    }

    
    private String findSearchName(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            return null;
        }

        
        return itemName.replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("\\[★\\]\\s*", "")
                .replaceAll("\\[★\\]", "")
                .replaceAll("^\\[.*?\\]\\s*", "")
                .trim();
    }

    

    
    private boolean matchesPotionEffects(ItemStack stack) {
        PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContents == null) {
            
            return true;
        }

        
        List<StatusEffectInstance> itemEffects = new ArrayList<>();
        potionContents.getEffects().forEach(itemEffects::add);

        if (itemEffects.isEmpty()) {
            
            return true;
        }

        
        List<List<StatusEffectInstance>> validPotions = List.of(
                
                List.of(new StatusEffectInstance(StatusEffects.STRENGTH, 180 * 20, 3)),
                List.of(new StatusEffectInstance(StatusEffects.STRENGTH, 90 * 20, 3)),
                
                List.of(new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 45 * 20, 2),
                        new StatusEffectInstance(StatusEffects.REGENERATION, 45 * 20, 2)),
                
                List.of(new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 180 * 20, 1),
                        new StatusEffectInstance(StatusEffects.REGENERATION, 60 * 20, 1)),
                
                List.of(new StatusEffectInstance(StatusEffects.SLOWNESS, 90 * 20, 3),
                        new StatusEffectInstance(StatusEffects.WEAKNESS, 90 * 20, 2)),
                
                List.of(new StatusEffectInstance(StatusEffects.SLOWNESS, 180 * 20, 2)),
                
                List.of(new StatusEffectInstance(StatusEffects.STRENGTH, 300 * 20, 2),
                        new StatusEffectInstance(StatusEffects.SPEED, 900 * 20, 2),
                        new StatusEffectInstance(StatusEffects.INVISIBILITY, 900 * 20, 0)),
                
                List.of(new StatusEffectInstance(StatusEffects.STRENGTH, 300 * 20, 2),
                        new StatusEffectInstance(StatusEffects.SPEED, 900 * 20, 2),
                        new StatusEffectInstance(StatusEffects.INVISIBILITY, 900 * 20, 1)),
                
                List.of(new StatusEffectInstance(StatusEffects.BLINDNESS, 20 * 20, 0),
                        new StatusEffectInstance(StatusEffects.GLOWING, 240 * 20, 0)),
                
                List.of(new StatusEffectInstance(StatusEffects.SPEED, 180 * 20, 2),
                        new StatusEffectInstance(StatusEffects.JUMP_BOOST, 180 * 20, 0)),
                
                List.of(new StatusEffectInstance(StatusEffects.INVISIBILITY, 900 * 20, 0),
                        new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 900 * 20, 0),
                        new StatusEffectInstance(StatusEffects.SPEED, 900 * 20, 2),
                        new StatusEffectInstance(StatusEffects.HASTE, 180 * 20, 0),
                        new StatusEffectInstance(StatusEffects.STRENGTH, 300 * 20, 2))
        );

        
        boolean matchesAny = validPotions.stream()
                .filter(potionEffects -> potionEffects.size() <= itemEffects.size())
                .anyMatch(potionEffects -> {
                    for (StatusEffectInstance requiredEffect : potionEffects) {
                        boolean found = false;
                        for (StatusEffectInstance itemEffect : itemEffects) {
                            if (itemEffect.getEffectType().equals(requiredEffect.getEffectType()) &&
                                    itemEffect.getAmplifier() == requiredEffect.getAmplifier() &&
                                    itemEffect.getDuration() == requiredEffect.getDuration()) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            return false;
                        }
                    }
                    return true;
                });

        
        
        if (matchesAny) {
            return true; 
        } else {
            
            return true;
        }
    }

}