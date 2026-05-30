package code.essence.features.impl.movement;

import code.essence.events.item.ClickSlotEvent;
import code.essence.features.impl.render.TargetESP;
import code.essence.utils.client.Instance;
import code.essence.utils.client.packet.network.Network;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.interactions.inv.InventoryFlowManager;
import lombok.Getter;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.interactions.simulate.Simulations;

import code.essence.utils.interactions.inv.InventoryTask;
import code.essence.events.container.CloseScreenEvent;
import code.essence.events.packet.PacketEvent;
import code.essence.events.player.TickEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import net.minecraft.screen.slot.SlotActionType;

import java.util.*;

@Getter
public class GuiMove extends Module {
    public static GuiMove getInstance() {
        return Instance.get(GuiMove.class);
    }
    private final List<Packet<?>> packets = new ArrayList<>();
    public static final SelectSetting mode = new SelectSetting("Режим", "Выберите режим передвижения в инвентаре")
            .value("Обычный", "ФанТайм", "СпукиТайм", "ХолиВорлд","РиллиВорлд","КопиТайм")
            .selected("СпукиТайм");

    public GuiMove() {
        super("GuiMove", "GuiMove", ModuleCategory.MOVEMENT);
        setup(mode);
    }



    @EventHandler
    public void onPacket(PacketEvent e) {
        if (!mode.isSelected("Обычный")) {
            switch (e.getPacket()) {
                case ClickSlotC2SPacket slot when (!packets.isEmpty() || Simulations.hasPlayerMovement()) && InventoryFlowManager.shouldSkipExecution() -> {
                    packets.add(slot);
                    e.cancel();
                }
                case CloseScreenS2CPacket screen when screen.getSyncId() == 0 -> e.cancel();
                default -> {
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (!InventoryTask.isServerScreen() && InventoryFlowManager.shouldSkipExecution() && (mode.isSelected("Обычный") || !packets.isEmpty() || mc.player.currentScreenHandler.getCursorStack().isEmpty())) {
            InventoryFlowManager.updateMoveKeys();
        }
    }

    @EventHandler
    public void onClickSlot(ClickSlotEvent e) {
        if (!mode.isSelected("Обычный")) {
            SlotActionType actionType = e.getActionType();
            if ((!packets.isEmpty() || Simulations.hasPlayerMovement()) && ((e.getButton() == 1 && !actionType.equals(SlotActionType.SWAP) && !actionType.equals(SlotActionType.THROW)) || actionType.equals(SlotActionType.PICKUP_ALL))) {
                e.cancel();
            }

        }
    }

    @EventHandler
    public void onCloseScreen(CloseScreenEvent e) {
        if (!mode.isSelected("Обычный") && !packets.isEmpty()) {
            InventoryFlowManager.addTask(() -> {
                packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
                packets.clear();
                InventoryTask.updateSlots();
            });
        }
    }
}