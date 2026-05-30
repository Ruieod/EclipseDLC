package code.essence.features.impl.combat;

import code.essence.features.impl.combat.autoswapUtil.AutoSwapWheelScreen;
import code.essence.features.impl.movement.GuiMove;
import code.essence.features.module.setting.implement.RadioSetting;
import code.essence.features.module.setting.implement.BooleanSetting;
import code.essence.features.module.setting.implement.MultiSelectSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.client.logs.Logger;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.BindSetting;
import code.essence.utils.interactions.inv.InventoryTask;
import code.essence.events.item.ClickSlotEvent;
import code.essence.events.keyboard.KeyEvent;
import code.essence.events.render.DrawEvent;
import code.essence.common.animation.Animation;
import code.essence.common.animation.Direction;
import code.essence.common.animation.implement.Decelerate;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.math.script.Script;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoSwap extends Module implements QuickImports {
    BindSetting bind = new BindSetting("Кнопка свапа", "Использует элемент при нажатии");

    RadioSetting mode = new RadioSetting("Режим", "Выберите режим работы.",
            new String[]{"Двойной", "Тройной"}, "Двойной");

    RadioSetting firstItem = new RadioSetting("Основной предмет", "Выберите первый предмет для обмена.",
             new String[]{"Тотем", "Сфера", "Гепл", "Щит"},"Тотем");

    RadioSetting secondItem = new RadioSetting("Вторичный предмет", "Выберите второй предмет для обмена.",
            new String[]{"Тотем", "Сфера", "Гепл", "Щит"},"Тотем");

    BooleanSetting autoDamage = new BooleanSetting("Авто урон", "Автоматически свапает на предмет с макс уроном");
    
    MultiSelectSetting autoDamageConditions = new MultiSelectSetting("Условия", "Условия для авто урона")
            .value("Таргет с малым HP", "У таргета нет меча")
            .selected("Таргет с малым HP")
            .visible(() -> autoDamage.isValue());
    
    SliderSettings lowHpThreshold = new SliderSettings("Порог HP", "Порог низкого HP таргета")
            .setValue(10).range(1F, 20F).step(0.5f)
            .visible(() -> autoDamage.isValue() && autoDamageConditions.isSelected("Таргет с малым HP"));

    @NonFinal
    ItemStack savedOffhandItem = ItemStack.EMPTY;
    @NonFinal
    boolean autoDamageActive = false;

    @NonFinal
    Text lastSwapDisplayName = null;
    @NonFinal
    long lastSwapTimeMs = 0L;

    Animation swapAnim = new Decelerate().setMs(250).setValue(1);
    
    final Script script = new Script();
    static final long SWAP_TOAST_DURATION_MS = 1800L;
    static final long SWAP_FADE_MS = 250L;
    
    
    private static class WheelSlotItem {
        final Item item;
        final String itemName;
        
        WheelSlotItem(Item item, String itemName) {
            this.item = item;
            this.itemName = itemName;
        }
    }
    
    @NonFinal
    WheelSlotItem[] wheelSlots = new WheelSlotItem[3];
    
    @NonFinal
    Integer selectingSlotIndex = null; 

    public AutoSwap() {
        super("AutoSwap", "AutoSwap", ModuleCategory.COMBAT);
        setup(mode, firstItem, secondItem, bind, autoDamage, autoDamageConditions, lowHpThreshold);
    }

    @EventHandler
    public void onTick(code.essence.events.player.TickEvent e) {
        script.update();
        
        if (autoDamage.isValue()) {
            handleAutoDamage();
        }
    }
    
    private void handleAutoDamage() {
        if (mc.player == null || mc.world == null) return;
        
        LivingEntity target = Aura.getInstance() != null ? Aura.getInstance().getTarget() : null;
        
        boolean shouldSwapToDamage = false;
        
        if (target != null && target.isAlive()) {
            boolean lowHpCondition = false;
            boolean noSwordCondition = false;
            
            if (autoDamageConditions.isSelected("Таргет с малым HP")) {
                boolean hpBelowThreshold = target.getHealth() <= lowHpThreshold.getValue();
                boolean hpLessThanMine = target.getHealth() < mc.player.getHealth();
                lowHpCondition = hpBelowThreshold && hpLessThanMine;
            }
            
            if (autoDamageConditions.isSelected("У таргета нет меча")) {
                if (target instanceof PlayerEntity player) {
                    ItemStack mainHand = player.getMainHandStack();
                    noSwordCondition = !(mainHand.getItem() instanceof SwordItem);
                }
            }
            
            if (autoDamageConditions.getSelected().isEmpty()) {
                shouldSwapToDamage = true;
            } else {
                shouldSwapToDamage = lowHpCondition || noSwordCondition;
            }
        }
        
        if (shouldSwapToDamage && !autoDamageActive) {
            savedOffhandItem = mc.player.getOffHandStack().copy();
            
            ItemStack bestDamageItem = findBestDamageItem();
            if (!bestDamageItem.isEmpty()) {
                swapToItemStack(bestDamageItem);
                autoDamageActive = true;
            }
        } else if (!shouldSwapToDamage && autoDamageActive) {
            if (!savedOffhandItem.isEmpty()) {
                swapToItemStack(savedOffhandItem);
            }
            autoDamageActive = false;
            savedOffhandItem = ItemStack.EMPTY;
        }
    }
    
    private ItemStack findBestDamageItem() {
        if (mc.player == null) return ItemStack.EMPTY;
        
        List<ItemStack> damageItems = new ArrayList<>();
        
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            
            if (stack.getItem() == Items.PLAYER_HEAD || stack.getItem() == Items.TOTEM_OF_UNDYING) {
                double damage = getAttackDamage(stack);
                if (damage > 0) {
                    damageItems.add(stack);
                }
            }
        }
        
        if (damageItems.isEmpty()) return ItemStack.EMPTY;
        
        return damageItems.stream()
                .max(Comparator.comparingDouble(this::getAttackDamage))
                .orElse(ItemStack.EMPTY);
    }
    
    private double getAttackDamage(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        
        AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) return 0;
        
        final double[] totalDamage = {0};
        
        modifiers.modifiers().forEach(entry -> {
            if (entry.attribute().value() == EntityAttributes.ATTACK_DAMAGE.value()) {
                totalDamage[0] += entry.modifier().value();
            }
        });
        
        return totalDamage[0];
    }
    
    private void swapToItemStack(ItemStack targetStack) {
        if (targetStack == null || targetStack.isEmpty()) return;
        
        Item item = targetStack.getItem();
        String itemName = targetStack.getName().getString();
        
        Slot foundSlot = null;
        for (Slot slot : InventoryTask.slots().filter(s -> s.id != 46 && s.id != 45).toList()) {
            ItemStack slotStack = slot.getStack();
            if (!slotStack.isEmpty() && slotStack.getItem() == item) {
                String slotName = slotStack.getName().getString();
                if (slotName.equals(itemName)) {
                    foundSlot = slot;
                    break;
                }
            }
        }
        
        if (foundSlot != null) {
            final Slot slotToSwap = foundSlot;
            InventoryTask.swapHand(slotToSwap, Hand.OFF_HAND, !GuiMove.mode.isSelected("РиллиВорлд"), true);
            script.cleanup().addTickStep(1, () -> triggerSwapToast(slotToSwap.getStack()));
        }
    }
    
    @EventHandler
    public void onClickSlot(ClickSlotEvent e) {
        
        if (selectingSlotIndex != null && mc.currentScreen instanceof InventoryScreen) {
            if (mc.player == null || mc.player.currentScreenHandler == null) return;
            
            Slot slot = null;
            for (Slot s : mc.player.currentScreenHandler.slots) {
                if (s.id == e.getSlotId()) {
                    slot = s;
                    break;
                }
            }
            
            if (slot != null && slot.hasStack() && slot.inventory instanceof PlayerInventory) {
                ItemStack stack = slot.getStack();
                if (!stack.isEmpty()) {
                    String itemName = stack.getName().getString();
                    setWheelSlotItem(selectingSlotIndex, stack.getItem(), itemName);
                    Logger.info("[AutoSwap] Предмет выбран для слота " + selectingSlotIndex + ": " + stack.getItem() + " (" + itemName + ")");
                    selectingSlotIndex = null; 
                    e.setCancelled(true); 
                    if (mc.currentScreen != null) {
                        mc.currentScreen.close();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (e.isKeyDown(bind.getKey())) {
            if (mode.get().equals("Тройной")) {
                
                if (mc != null && mc.currentScreen == null) {
                    mc.setScreen(new AutoSwapWheelScreen(this));
                }
            } else {
                
                Slot first = InventoryTask.getSlot(getItemByType(firstItem.get()), Comparator.comparing(s -> s.getStack().hasEnchantments()), s -> s.id != 46 && s.id != 45);
                Slot second = InventoryTask.getSlot(getItemByType(secondItem.get()), Comparator.comparing(s -> s.getStack().hasEnchantments()), s -> s.id != 46 && s.id != 45);
                Slot validSlot = first != null && mc.player.getOffHandStack().getItem() != first.getStack().getItem() ? first : second;
                
                if (validSlot != null) {
                    ItemStack stackToSwap = validSlot.getStack();
                    InventoryTask.swapHand(validSlot, Hand.OFF_HAND, GuiMove.mode.isSelected("РиллиВорлд") ? false : true, true);
                    script.cleanup().addTickStep(1, () -> triggerSwapToast(stackToSwap));
                }
            }
        }
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (lastSwapTimeMs == 0) return;
        if (mc == null || mc.getWindow() == null || mc.textRenderer == null) return;
        
        long elapsed = System.currentTimeMillis() - lastSwapTimeMs;
        if (elapsed > SWAP_TOAST_DURATION_MS) {
            lastSwapTimeMs = 0;
            lastSwapDisplayName = null;
            return;
        }

        boolean fadingIn = elapsed < SWAP_FADE_MS;
        boolean fadingOut = elapsed > (SWAP_TOAST_DURATION_MS - SWAP_FADE_MS);
        float anim = 1f;
        
        if (fadingIn) {
            anim = Math.min(1f, (float) elapsed / SWAP_FADE_MS);
        } else if (fadingOut) {
            anim = Math.max(0f, (float) (SWAP_TOAST_DURATION_MS - elapsed) / SWAP_FADE_MS);
        }
        
        if (anim <= 0.01f) return;

        DrawContext context = e.getDrawContext();
        if (context == null) return;
        
        MatrixStack matrix = context.getMatrices();
        
        final String prefixStr = "Свапнул на ";
        final int screenW = mc.getWindow().getScaledWidth();
        final int screenH = mc.getWindow().getScaledHeight();
        final float y = screenH - 80;

        final float prefixW = mc.textRenderer.getWidth(prefixStr);
        float nameW = 0f;
        if (lastSwapDisplayName != null) {
            nameW = mc.textRenderer.getWidth(lastSwapDisplayName);
        }
        final float nameWFinal = nameW;
        final float totalW = prefixW + nameWFinal;
        final float x = (screenW - totalW) / 2f;

        final float animFinal = anim;
        final int alpha = (int) (255 * animFinal);
        final int prefixColor = ColorAssist.rgba(255, 255, 255, alpha);
        final Text lastSwapDisplayNameFinal = lastSwapDisplayName;

        Calculate.setAlpha(animFinal, () -> {
            Calculate.scale(matrix, x + totalW / 2f, y + 6, animFinal, () -> {
                context.drawText(mc.textRenderer, prefixStr, (int)x, (int)y, prefixColor, false);
                if (lastSwapDisplayNameFinal != null) {
                    context.drawText(mc.textRenderer, lastSwapDisplayNameFinal, (int)(x + prefixW), (int)y, 0xFFFFFF | (alpha << 24), false);
                }
            });
        });
    }

    private void triggerSwapToast(ItemStack swappedTo) {
        try {
            lastSwapTimeMs = System.currentTimeMillis();
            lastSwapDisplayName = swappedTo.getName();
            swapAnim.setDirection(Direction.FORWARDS);
            swapAnim.reset();
        } catch (Exception ignored) {}
    }

    public Item getItemByType(String itemType) {
        return switch (itemType) {
            case "Тотем" -> Items.TOTEM_OF_UNDYING;
            case "Сфера" -> Items.PLAYER_HEAD;
            case "Гепл" -> Items.GOLDEN_APPLE;
            case "Щит" -> Items.SHIELD;
            default -> Items.AIR;
        };
    }
    
    public void setWheelSlotItem(int index, Item item, String itemName) {
        if (index >= 0 && index < wheelSlots.length) {
            wheelSlots[index] = new WheelSlotItem(item, itemName);
            Logger.info("[AutoSwap] setWheelSlotItem: index=" + index + ", item=" + (item != null ? item.toString() : "null") + ", name=" + itemName);
        }
    }
    
    public void startSelectingItem(int wheelSlotIndex) {
        selectingSlotIndex = wheelSlotIndex;
        if (mc != null && mc.player != null) {
            mc.setScreen(new InventoryScreen(mc.player));
            Logger.info("[AutoSwap] Открыт инвентарь для выбора предмета в слот " + wheelSlotIndex);
        }
    }
    
    public Item getWheelSlotItem(int index) {
        if (index >= 0 && index < wheelSlots.length && wheelSlots[index] != null) {
            return wheelSlots[index].item;
        }
        return null;
    }
    
    public ItemStack getWheelSlotStack(int index) {
        if (index < 0 || index >= wheelSlots.length) {
            Logger.warn("[AutoSwap] getWheelSlotStack: невалидный индекс " + index);
            return ItemStack.EMPTY;
        }
        WheelSlotItem slotItem = wheelSlots[index];
        if (slotItem == null || slotItem.item == null || slotItem.item == Items.AIR) {
            Logger.info("[AutoSwap] getWheelSlotStack: слот " + index + " пуст");
            return ItemStack.EMPTY;
        }
        if (mc == null || mc.player == null) {
            Logger.warn("[AutoSwap] getWheelSlotStack: mc или player null");
            return ItemStack.EMPTY;
        }
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.getItem() == slotItem.item) {
                
                String stackName = stack.getName().getString();
                if (stackName.equals(slotItem.itemName)) {
                    Logger.info("[AutoSwap] getWheelSlotStack: найден стек для слота " + index + ", item=" + slotItem.item + ", name=" + slotItem.itemName + ", в инвентаре слот " + i);
                    return stack;
                }
            }
        }
        Logger.warn("[AutoSwap] getWheelSlotStack: предмет " + slotItem.item + " с названием '" + slotItem.itemName + "' не найден в инвентаре для слота " + index);
        return ItemStack.EMPTY;
    }
    
    public void startSwapToItem(Item item) {
        Logger.info("[AutoSwap] startSwapToItem вызван: item=" + (item != null ? item.toString() : "null"));
        
        if (mc == null || mc.player == null || item == null || item == Items.AIR) {
            Logger.warn("[AutoSwap] startSwapToItem отменен: mc=" + (mc != null) + ", player=" + (mc != null && mc.player != null) + ", item=" + item);
            return;
        }
        
        Logger.info("[AutoSwap] Ищем слот для предмета: " + item);
        Slot slot = InventoryTask.getSlot(item, Comparator.comparing(s -> s.getStack().hasEnchantments()), s -> s.id != 46 && s.id != 45);
        
        if (slot != null) {
            ItemStack stackToSwap = slot.getStack();
            Logger.info("[AutoSwap] Найден слот: id=" + slot.id + ", stack=" + stackToSwap.getItem());
            InventoryTask.swapHand(slot, Hand.OFF_HAND, GuiMove.mode.isSelected("РиллиВорлд") ? false : true, true);
            Logger.info("[AutoSwap] Выполнен swapHand");
            script.cleanup().addTickStep(1, () -> triggerSwapToast(stackToSwap));
        } else {
            Logger.warn("[AutoSwap] Слот не найден для предмета: " + item);
        }
    }
    
    public void startSwapToItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            Logger.warn("[AutoSwap] startSwapToItemStack: стек пуст");
            return;
        }
        
        Item item = stack.getItem();
        String itemName = stack.getName().getString();
        Logger.info("[AutoSwap] startSwapToItemStack: item=" + item + ", name=" + itemName);
        
        if (mc == null || mc.player == null || item == null || item == Items.AIR) {
            Logger.warn("[AutoSwap] startSwapToItemStack отменен");
            return;
        }
        
        
        Slot foundSlot = null;
        for (Slot slot : InventoryTask.slots().filter(s -> s.id != 46 && s.id != 45).toList()) {
            ItemStack slotStack = slot.getStack();
            if (!slotStack.isEmpty() && slotStack.getItem() == item) {
                String slotName = slotStack.getName().getString();
                if (slotName.equals(itemName)) {
                    foundSlot = slot;
                    break;
                }
            }
        }
        
        if (foundSlot != null) {
            ItemStack stackToSwap = foundSlot.getStack();
            Logger.info("[AutoSwap] Найден слот: id=" + foundSlot.id + ", stack=" + stackToSwap.getItem() + ", name=" + stackToSwap.getName().getString());
            InventoryTask.swapHand(foundSlot, Hand.OFF_HAND, GuiMove.mode.isSelected("РиллиВорлд") ? false : true, true);
            Logger.info("[AutoSwap] Выполнен swapHand");
            script.cleanup().addTickStep(1, () -> triggerSwapToast(stackToSwap));
        } else {
            Logger.warn("[AutoSwap] Слот не найден для предмета: " + item + " с названием '" + itemName + "'");
        }
    }
}

