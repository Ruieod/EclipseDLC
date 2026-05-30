package code.essence.features.impl.movement;

import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.events.packet.PacketEvent;
import code.essence.utils.math.projection.Projection;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.client.managers.event.EventHandler;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoFallDamage extends Module {

    SelectSetting mode = new SelectSetting("Режим", "Выберите тип")
            .value("SpookyTime")
            .selected("SpookyTime");

    public NoFallDamage() {
        super("NoFallDamage", "NoFallDamage", ModuleCategory.MOVEMENT);
        setup(mode);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.fallDistance > 0 && Projection.getDistanceToGround() >4) {
            mc.player.setVelocity(0, 0, 0);
        }
    }
}
