package code.essence.features.impl.misc;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.text.Text;

import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.MultiSelectSetting;
import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.common.repository.friend.FriendUtils;
import code.essence.utils.client.packet.network.Network;
import code.essence.events.player.TickEvent;
import code.essence.display.hud.Notifications;
import code.essence.display.hud.StaffList;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoLeave extends Module {
    SelectSetting leaveType = new SelectSetting("Режим", "Позволяет выбрать тип выхода")
            .value("/hub", "Выход с сервера","/clan home","/spawn");

    MultiSelectSetting triggerSetting = new MultiSelectSetting("Срабатывать на", "Выберите, в каких случаях произойдет выход")
            .value("Игроков", "Админов");

    SliderSettings distanceSetting = new SliderSettings("Дистанция", "Максимальная дистанция для активации авто-выхода")
            .setValue(10).range(5, 40).visible(() -> triggerSetting.isSelected("Игроков"));

    public AutoLeave() {
        super("AutoLeave", "AutoLeave", ModuleCategory.MISC);
        setup(leaveType, triggerSetting, distanceSetting);
    }

    
    @EventHandler
    
    public void onTick(TickEvent e) {
        if (Network.isPvp()) return;

        if (triggerSetting.isSelected("Игроков"))
            mc.world.getPlayers().stream().filter(p -> mc.player.distanceTo(p) < distanceSetting.getValue() && mc.player != p && !FriendUtils.isFriend(p))
                    .findFirst().ifPresent(p -> leave(p.getName().copy().append(" - Появился рядом " + mc.player.distanceTo(p) + "м")));
        if (triggerSetting.isSelected("Админов") && !StaffList.getInstance().list.isEmpty())
            leave(Text.of("Стафф на сервере"));
    }

    
    public void leave(Text text) {
        switch (leaveType.getSelected()) {
            case "/hub" -> {
                Notifications.getInstance().addList(Text.of("[AutoLeave] ").copy().append(text), 10000);
                mc.getNetworkHandler().sendChatCommand("hub");
            }
            case "выход с сервера" ->
                    mc.getNetworkHandler().getConnection().disconnect(Text.of("[Auto Leave] \n").copy().append(text));
            case "/spawn" -> {
                assert mc.player != null;
                mc.player.networkHandler.sendChatMessage("/spawn");
            }
            case "/clan home" -> {
                assert mc.player != null;
                mc.player.networkHandler.sendChatMessage("/clan home");
            }
        }
        setState(false);
    }

}
