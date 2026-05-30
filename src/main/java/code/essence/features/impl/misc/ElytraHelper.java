package code.essence.features.impl.misc;

import code.essence.features.impl.movement.GuiMove;
import code.essence.utils.client.chat.ChatMessage;
import code.essence.utils.client.packet.network.Network;
import code.essence.utils.client.sound.SoundManager;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.interactions.inv.InventoryFlowManager;
import code.essence.utils.interactions.inv.InventoryTask;
import code.essence.utils.interactions.inv.InventoryToolkit;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;

import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.BindSetting;
import code.essence.features.module.setting.implement.BooleanSetting;
import code.essence.events.player.InputEvent;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.math.script.Script;
import code.essence.events.keyboard.KeyEvent;
import code.essence.events.player.TickEvent;
import net.minecraft.screen.slot.SlotActionType;
import code.essence.utils.interactions.item.ItemTask;
import code.essence.display.hud.Notifications;
import net.minecraft.util.Formatting;

import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ElytraHelper extends Module {
    BindSetting elytraSetting = new BindSetting("Свап элитр", "Меняет нагрудник на элитры");
    BindSetting fireworkSetting = new BindSetting("Фейерверк", "Меняет и использует фейерверки");
    BooleanSetting startSetting = new BooleanSetting("Быстрый старт", "При замене на элитры автоматически взлетает и использует фейерверки").setValue(false);
    BooleanSetting recast = new BooleanSetting("Авто взлет", "Автоматически начинает полет").setValue(false);
    BooleanSetting equipInCT = new BooleanSetting("Надевать в кт", "Использует обход надевания в кт").setValue(false);
    Script script = new Script();


    boolean isWaitingForSwap = false;
    int swapTargetSlot = -1;
    int swapTimeout = 0;
    boolean fireworkQueued = false;

    public static boolean start = false;

    public ElytraHelper() {
        super("ElytraHelper", "ElytraHelper", ModuleCategory.MISC);
        setup(elytraSetting, fireworkSetting, startSetting, recast, equipInCT);
    }

    @EventHandler
    public void onInput(InputEvent e) {
        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA) && recast.isValue()) {
            if (mc.player.isOnGround()) e.setJumping(true);
            else if (!mc.player.isGliding()) PlayerInteractionHelper.startFallFlying();
        }
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (!script.isFinished()) return;
        if (e.isKeyDown(elytraSetting.getKey())) {

            if (equipInCT.isValue()) {
                changeChestPlate();
            } else {
                Slot slot = chestPlate();
                if (slot != null) {
                    Slot fireWork = InventoryTask.getSlot(Items.FIREWORK_ROCKET);
                    boolean elytra = slot.getStack().getItem().equals(Items.ELYTRA);
                    InventoryTask.moveItem(slot, 6, GuiMove.mode.isSelected("РиллиВорлд") ? false : true , true);


                    if (startSetting.isValue() && fireWork != null && elytra) script.cleanup().addTickStep(4, () -> {
                        if (mc.player.isOnGround()) mc.player.jump();
                    }).addTickStep(3, () -> {
                        
                        float cooldownProgress = ItemTask.getCooldownProgress(Items.FIREWORK_ROCKET);
                        if (cooldownProgress > 0) {
                            String time = String.format("%.1f", cooldownProgress) + "с";
                            Notifications.getInstance().addList(Formatting.RED + "Фейерверк" + Formatting.RESET + " - в кд еще " + time, 2000);
                            return;
                        }
                        PlayerInteractionHelper.startFallFlying();
                        InventoryTask.swapAndUse(Items.FIREWORK_ROCKET,false);
                    });
                }
            }
        } else if (e.isKeyDown(fireworkSetting.getKey()) && mc.player.isGliding()) {
                float cooldownProgress = ItemTask.getCooldownProgress(Items.FIREWORK_ROCKET);
                if (cooldownProgress > 0) {
                    String time = String.format("%.1f", cooldownProgress) + "с";
                    SoundManager.playSound(SoundManager.DISABLE_MODULE);
                    Notifications.getInstance().addList(Formatting.RED + "Фейерверк" + Formatting.RESET + " - в кд еще " + time, 1500);
                    return;
                }
                InventoryTask.swapAndUse(Items.FIREWORK_ROCKET, GuiMove.mode.isSelected("РиллиВорлд") ? false : true);
        }
    }


    @EventHandler
    public void onTick(TickEvent e) {
        script.update();
        if (mc.player == null) return;
        if (isWaitingForSwap) {
            swapTimeout++;

            if (mc.player.getInventory().getStack(swapTargetSlot).isOf(Items.FIREWORK_ROCKET)) {
                
                float cooldownProgress = ItemTask.getCooldownProgress(Items.FIREWORK_ROCKET);
                if (cooldownProgress > 0) {
                    String time = String.format("%.1f", cooldownProgress) + "с";
                    Notifications.getInstance().addList(Formatting.RED + "Фейерверк" + Formatting.RESET + " - в кд еще " + time, 2000);
                    resetSwapState();
                    return;
                }
                InventoryTask.useItemFromSlotMOMENTALNO(swapTargetSlot);
                resetSwapState();
            } else if (swapTimeout > 5) {
                resetSwapState();
            }
            return;
        }



    }


    private Slot chestPlate() {
        if (Objects.requireNonNull(mc.player).getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA))
            return InventoryTask.getSlot(List.of(Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.LEATHER_CHESTPLATE));
        else return InventoryTask.getSlot(Items.ELYTRA);
    }

    public void changeChestPlate() {
        ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (chestStack == null || chestStack.getItem() != Items.ELYTRA) {
            int elytraSlot = getItemSlot(Items.ELYTRA);
            if (elytraSlot == -1) {
                ChatMessage.brandmessage("Нету: Элитра");
                return;
            }
            InventoryTask.clickSlot(elytraSlot, 38, SlotActionType.SWAP, false, true);
            ChatMessage.brandmessage("Свапнул на элитру");
        } else {
            int chestSlot = getChestPlateSlot();
            if (chestSlot == -1) {
                ChatMessage.brandmessage("Нет нагрудника!");
                return;
            }
            InventoryTask.clickSlot(chestSlot, 38, SlotActionType.SWAP, false, true);
            ChatMessage.brandmessage("Свапнул на нагрудник (обошёл запрет свапа в кт)");
        }
    }

    private int getItemSlot(Item input) {
        int slot = -1;
        for (int i = 0; i < 36; ++i) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (s.getItem() != input) continue;
            slot = i;
            break;
        }
        if (slot < 9 && slot != -1) {
            slot += 36;
        }
        return slot;
    }
    private void handleFirework() {
        if (mc.player == null || !mc.player.isGliding() || isWaitingForSwap) return;

        
        float cooldownProgress = ItemTask.getCooldownProgress(Items.FIREWORK_ROCKET);
        if (cooldownProgress > 0) {
            String time = String.format("%.1f", cooldownProgress) + "с";
            Notifications.getInstance().addList(Formatting.RED + "Фейерверк" + Formatting.RESET + " - в кд еще " + time, 2000);
            return;
        }

        int rocketHotbarSlot =  InventoryTask.findHotbarSlot(Items.FIREWORK_ROCKET);

        if (rocketHotbarSlot != -1) {
            InventoryTask.useItemFromSlotMOMENTALNO(rocketHotbarSlot);
            return;
        }
        int rocketInventorySlot = InventoryTask.findInventorySlot(Items.FIREWORK_ROCKET);
        if (rocketInventorySlot != -1) {
            int targetHotbarSlot = InventoryTask.findFreeHotbarSlot();
            if (targetHotbarSlot != -1) {
               InventoryTask.swapSlots(rocketInventorySlot, targetHotbarSlot);
                isWaitingForSwap = true;
                swapTargetSlot = targetHotbarSlot;
                swapTimeout = 0;
            }
        }
    }


    private void resetSwapState() {
        isWaitingForSwap = false;
        swapTargetSlot = -1;
        swapTimeout = 0;
    }


    public static int getChestPlateSlot() {
        Item[] items = new Item[]{Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.LEATHER_CHESTPLATE};
        for (Item item : items) {
            for (int i = 0; i < 36; ++i) {
                Item stack = mc.player.getInventory().getStack(i).getItem();
                if (stack != item) continue;
                if (i < 9) {
                    i += 36;
                }
                return i;
            }
        }
        return -1;
    }
}
