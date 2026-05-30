package code.essence.features.impl.misc;

import code.essence.display.hud.Notifications;
import code.essence.events.keyboard.KeyEvent;
import code.essence.features.impl.movement.GuiMove;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.BindSetting;
import code.essence.features.module.setting.implement.BooleanSetting;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.client.chat.ChatMessage;
import code.essence.utils.interactions.inv.InventoryFlowManager;
import code.essence.utils.interactions.inv.InventoryTask;
import code.essence.utils.interactions.inv.InventoryTick;
import code.essence.utils.interactions.item.ItemTask;
import code.essence.utils.interactions.simulate.Simulations;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.math.calc.Calculate;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import static code.essence.utils.interactions.inv.InventoryTask.getSlot;
import static code.essence.utils.interactions.inv.InventoryTask.swapAndUse;
import code.essence.Essence;
import code.essence.features.module.ModuleRepository;
import lombok.Getter;

public class ClickPearl extends Module {
    private static ClickPearl instance;
    
    public static ClickPearl getInstance() {
        if (instance != null) {
            return instance;
        }
        
        try {
            Essence essence = Essence.getInstance();
            if (essence == null) {
                return null;
            }
            
            ModuleRepository moduleRepository = essence.getModuleRepository();
            if (moduleRepository == null) {
                return null;
            }
            
            instance = moduleRepository.modules().stream()
                .filter(m -> m instanceof ClickPearl)
                .map(m -> (ClickPearl) m)
                .findFirst()
                .orElse(null);
            
            return instance;
        } catch (Exception ignored) {
            return null;
        }
    }
    
    private final BindSetting keySetting = new BindSetting("Кнопка", "Кнопка для использования");
    @Getter
    private final BooleanSetting logSetting = new BooleanSetting("Логировать", "Логировать действия ClickPearl в чат")
            .setValue(false);

    public ClickPearl() {
        super("ClickPearl", "ClickPearl", ModuleCategory.MISC);
        setup(keySetting, logSetting);
        instance = this;
    }

    private void log(String message) {
        if (logSetting.isValue()) {
            ChatMessage.brandmessage(message);
        }
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (e.isKeyDown(keySetting.getKey())) {
            log("§e[ClickPearl] Нажата клавиша для броска перла");

            float cooldownProgress = ItemTask.getCooldownProgress(Items.ENDER_PEARL);
            log("§e[ClickPearl] Кулдаун перла: " + cooldownProgress);

            if (cooldownProgress > 0) {
                String time = Calculate.round(cooldownProgress, 0.1) + "с";
                log("§c[ClickPearl] Перл в кулдауне еще " + time);
                Notifications.getInstance().addList(Formatting.RED + Items.ENDER_PEARL.getName().getString() + Formatting.RESET + " - в кд еще " + time, 2000);
                return;
            }

            
            int hotbarSlot = InventoryTask.findHotbarSlot(Items.ENDER_PEARL);
            
            if (hotbarSlot != -1) {
                
                log("§a[ClickPearl] Перла найдена в хотбаре, слот: " + hotbarSlot);
                
                int previousSlot = mc.player.getInventory().selectedSlot;
                log("§e[ClickPearl] Сохраняем текущий слот: " + previousSlot);
                
                
             
                if (mc.getNetworkHandler() != null) {
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
                }
                log("§a[ClickPearl] Переключились на слот с перлой");
                
                
                InventoryTick.schelude(() -> {
                    PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    log("§a[ClickPearl] Перла брошена");
                    
                    
                  
                    if (mc.getNetworkHandler() != null) {
                        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
                    }
                    log("§a[ClickPearl] Вернулись к предыдущему слоту: " + previousSlot);
                }, (GuiMove.mode.isSelected("ХолиВорлд") ? 3 : 2)); 
            } else {
                
                Slot slot = getSlot(Items.ENDER_PEARL);
                log("§e[ClickPearl] Слот перла: " + (slot != null ? "найден, id=" + slot.id : "не найден"));

                if (slot == null) {
                    log("§c[ClickPearl] Перл не найден в инвентаре!");
                    Notifications.getInstance().addList(Formatting.RED + Items.ENDER_PEARL.getName().getString() + Formatting.RESET + " - не найден!", 2000);
                    return;
                }

                boolean isReallyWorld = GuiMove.mode.isSelected("РиллиВорлд");
                log("§e[ClickPearl] Режим GuiMove: " + (isReallyWorld ? "РиллиВорлд" : "другой"));

                if (isReallyWorld) {
                    log("§a[ClickPearl] Вызываю swapAndUse напрямую (режим=РиллиВорлд)");
                    swapAndUse(Items.ENDER_PEARL, false);
                } else {
                    log("§a[ClickPearl] Добавляю задачу в InventoryFlowManager");
              
                        log("§a[ClickPearl] Выполняю swapAndUse в задаче");
                        swapAndUse(Items.ENDER_PEARL, true);
                 
                }
            }

            log("§a[ClickPearl] Обработка клика завершена");
        }
    }
}
