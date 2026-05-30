package code.essence.features.impl.player;

import code.essence.events.player.TickEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.mixins.IMinecraftClient;
import code.essence.utils.client.Instance;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.client.managers.event.impl.EventUpdate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoFall extends Module {
    public static NoFall getInstance() {
        return Instance.get(NoFall.class);
    }

    public NoFall() {
        super("NoFall", ModuleCategory.PLAYER);
    }


    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player.fallDistance > 2.4) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY() + 1e-6, mc.player.getZ(), mc.player.getYaw(), ( mc.player).getPitch(), false,false));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY() + 1e-6, mc.player.getZ(), mc.player.getYaw(), (mc.player).getPitch(), false,false));
            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.OFF_HAND, 0,mc.player.getYaw(),mc.player.getPitch()));
            mc.player.fallDistance = 0;
        }
    }
}
