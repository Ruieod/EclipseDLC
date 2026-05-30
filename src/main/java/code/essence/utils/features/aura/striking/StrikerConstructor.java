package code.essence.utils.features.aura.striking;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import code.essence.features.module.setting.implement.RadioSetting;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.events.item.UsingItemEvent;
import code.essence.events.packet.PacketEvent;
import code.essence.utils.features.aura.warp.Turns;

import java.util.List;

@Getter
public class StrikerConstructor implements QuickImports {
    StrikeManager attackHandler = new StrikeManager();

    public void tick() {
        attackHandler.tick();
    }

    public void onPacket(PacketEvent e) {
        attackHandler.onPacket(e);
    }

    public void onUsingItem(UsingItemEvent e) {
        attackHandler.onUsingItem(e);
    }

    public void performAttack(AttackPerpetratorConfigurable configurable) {
        attackHandler.handleAttack(configurable);
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class AttackPerpetratorConfigurable {
        LivingEntity target;
        Turns angle;
        float maximumRange;
        boolean onlyCritical, shouldBreakShield, shouldUnPressShield, eatAndAttack;
        Box box;
        RadioSetting aimMode;
        String sprintResetOverride;
        Boolean smartCritsOverride;

        public AttackPerpetratorConfigurable(LivingEntity target, Turns angle, float maximumRange, List<String> options, RadioSetting aimMode, Box box) {
            this(target, angle, maximumRange, options, aimMode, box, null, null);
        }

        public AttackPerpetratorConfigurable(LivingEntity target, Turns angle, float maximumRange, List<String> options, RadioSetting aimMode, Box box, String sprintResetOverride) {
            this(target, angle, maximumRange, options, aimMode, box, sprintResetOverride, null);
        }

        public AttackPerpetratorConfigurable(LivingEntity target, Turns angle, float maximumRange, List<String> options, RadioSetting aimMode, Box box, String sprintResetOverride, Boolean smartCritsOverride) {
            this.target = target;
            this.angle = angle;
            this.maximumRange = maximumRange;
            this.onlyCritical = options.contains("Только криты");
            this.shouldBreakShield = options.contains("Ломать щит");
            this.shouldUnPressShield = options.contains("Отжимать щит");
            this.eatAndAttack = options.contains("Не бить если ешь");
            this.box = box;
            this.aimMode = aimMode;
            this.sprintResetOverride = sprintResetOverride;
            this.smartCritsOverride = smartCritsOverride;
        }
    }
}
