package code.essence.features.impl.misc;


import code.essence.utils.interactions.inv.InventoryTask;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.events.packet.PacketEvent;
import code.essence.events.player.TickEvent;
import code.essence.utils.client.Instance;
import code.essence.utils.client.packet.network.Network;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerJoiner extends Module {
    public static ServerJoiner getInstance() {
        return Instance.get(ServerJoiner.class);
    }
    SelectSetting serverSelection = new SelectSetting("Сервер", "Выберите целевой сервер")
            .value("РиллиВорлд", "СпукиТайм дуэли")
            .selected("РиллиВорлд");
    SliderSettings griefSelection = new SliderSettings("Номер грифа", "Выберите номер сервера для грифа")
            .setValue(1)
            .range(1, 54)
            .visible(() -> serverSelection.isSelected("РиллиВорлд"));

    @NonFinal long lastActionTime;
    @NonFinal boolean isToggling;
    @NonFinal boolean retryDuels;

    public ServerJoiner() {
        super("ServerJoiner", "ServerJoiner", ModuleCategory.MISC);
        setup(serverSelection, griefSelection);
    }

    @EventHandler
    
    public void onTick(TickEvent event) {
        if (!state) return;

        if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_INSERT) == GLFW.GLFW_PRESS) {
            deactivate();
            return;
        }

        if (mc.currentScreen == null && mc.player != null && mc.player.age < 5) {
            InventoryTask.selectCompass();
            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, mc.player.getYaw(), mc.player.getPitch()));
        } else if (mc.currentScreen instanceof GenericContainerScreen container) {
            for (int i = 0; i < container.getScreenHandler().slots.size(); i++) {
                String s = container.getScreenHandler().slots.get(i).getStack().getName().getString().toLowerCase();
                if (serverSelection.isSelected("РиллиВорлд") && Network.isReallyWorld()) {
                    if (s.contains("гриферское выживание")) {
                        if (System.currentTimeMillis() - lastActionTime > 50) {
                            InventoryTask.clickSlot(i, 0, SlotActionType.PICKUP, false);
                            lastActionTime = System.currentTimeMillis();
                        }
                    }
                    int numberGrief = (int) griefSelection.getValue();
                    if (s.contains("гриф #" + numberGrief)) {
                        if (System.currentTimeMillis() - lastActionTime > 50) {
                            InventoryTask.clickSlot(i, 0, SlotActionType.PICKUP, false);
                            lastActionTime = System.currentTimeMillis();
                        }
                    }
                } else if (serverSelection.isSelected("СпукиТайм дуэли")) {
                    String title = container.getTitle().getString().toLowerCase();
                    if (title.contains("выберите режим") && System.currentTimeMillis() - lastActionTime > 50) {
                        InventoryTask.clickSlot(14, 0, SlotActionType.QUICK_MOVE, false);
                        lastActionTime = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (!state || event.getType() != PacketEvent.Type.RECEIVE) return;

        if (event.getPacket() instanceof DisconnectS2CPacket packet) {
            String message = packet.reason().getString().toLowerCase();
            if (message.contains("к сожалению сервер переполнен") ||
                message.contains("подождите 20 секунд!") ||
                message.contains("Вы уже подключены на этот сервер!") ||
                message.contains("подождите несколько секунд перед повторным подключением!") ||
                message.contains("вы были кикнуты с сервера 1duels:") ||
                message.contains("Вы были кикнуты") ||
                message.contains("большой поток игроков") ||
                message.contains("сервер заполнен!")) {
                InventoryTask.selectCompass();
                mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, mc.player.getYaw(), mc.player.getPitch()));
            }
        }
    }

    @Override
    public void activate() {
        super.activate();
        InventoryTask.selectCompass();
        mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, mc.player.getYaw(), mc.player.getPitch()));
        isToggling = false;
        retryDuels = false;
        lastActionTime = 0;
    }

    @Override
    public void deactivate() {
        super.deactivate();
        lastActionTime = 0;
        isToggling = false;
        retryDuels = false;
    }
}