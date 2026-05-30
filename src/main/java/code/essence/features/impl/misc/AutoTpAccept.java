package code.essence.features.impl.misc;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.BooleanSetting;
import code.essence.common.repository.friend.FriendUtils;
import code.essence.utils.client.packet.network.Network;
import code.essence.events.packet.PacketEvent;
import code.essence.events.player.TickEvent;

import java.util.Arrays;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoTpAccept extends Module {
    private final String[] teleportMessages = new String[]{
            "has requested teleport",
            "просит телепортироваться",
            "хочет телепортироваться к вам",
            "просит к вам телепортироваться"
    };
    private boolean canAccept;

    private final BooleanSetting friendSetting = new BooleanSetting("Только друзья", "Будет принимать запросы только от друзей").setValue(false);

    public AutoTpAccept() {
        super("AutoTpAccept", "AutoTpAccept", ModuleCategory.MISC);
        setup(friendSetting);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof GameMessageS2CPacket m) {
            String message = m.content().getString();
            boolean validPlayer = !friendSetting.isValue() || FriendUtils.getFriends().stream().anyMatch(s -> message.contains(s.getName()));
            if (isTeleportMessage(message)) {
                canAccept = validPlayer;
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (!Network.isPvp() && canAccept) {
            mc.player.networkHandler.sendChatCommand("tpaccept");
            canAccept = false;
        }
    }

    
    private boolean isTeleportMessage(String message) {
        return Arrays.stream(this.teleportMessages).map(String::toLowerCase).anyMatch(message::contains);
    }
}