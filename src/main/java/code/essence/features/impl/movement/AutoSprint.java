package code.essence.features.impl.movement;


import code.essence.main.listener.impl.EventListener;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.effect.StatusEffects;
import code.essence.features.module.setting.implement.MultiSelectSetting;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.client.Instance;
import code.essence.events.player.TickEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoSprint extends Module {
    public static AutoSprint getInstance() {
        return Instance.get(AutoSprint.class);
    }

    public static int tickStop;

    MultiSelectSetting settings = new MultiSelectSetting("Игнорировать эффект", "Игнорировать эффекты игрока")
            .value("Замедление", "Слепота");

    public AutoSprint() {
        super("AutoSprint", "AutoSprint", ModuleCategory.MOVEMENT);
    }

    @EventHandler
    
    public void onTick(TickEvent e) {
        boolean horizontal = mc.player.horizontalCollision && !mc.player.collidedSoftly;
        boolean sneaking = mc.player.isSneaking() && !mc.player.isSwimming();
        if (!(settings.isSelected("Слепота") && mc.player.hasStatusEffect(StatusEffects.BLINDNESS))
                && !(settings.isSelected("Замедление") && mc.player.hasStatusEffect(StatusEffects.SLOWNESS))
                && tickStop > 0 || sneaking) {
            mc.player.setSprinting(false);
        } else if (!horizontal && mc.player.forwardSpeed > 0) {
            mc.player.setSprinting(true);
        }

        tickStop--;
    }

}