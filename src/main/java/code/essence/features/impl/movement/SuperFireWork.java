package code.essence.features.impl.movement;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.Vec3d;

import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.events.player.FireworkEvent;
import code.essence.utils.features.aura.warp.TurnsConnection;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SuperFireWork extends Module {
    SelectSetting modeSetting = new SelectSetting("Режим", "Выберите тип режима")
            .value("Грим", "Кастом");

    SliderSettings speedSetting = new SliderSettings("Скорость", "Скорость полета фейерверка")
            .range(1f, 50f)
            .setValue(20f)
            .visible(() -> modeSetting.isSelected("Кастом"));

    public SuperFireWork() {
        super("SuperFireWork", "SuperFireWork", ModuleCategory.MOVEMENT);
        setup(modeSetting, speedSetting);
    }


    @EventHandler
    public void onFirework(FireworkEvent e) {
        if (modeSetting.isSelected("Грим")) {
            int ff = TurnsConnection.INSTANCE.getRotation().getYaw() > 0F ? 45 : -45;
            double acceleration = Math.abs((TurnsConnection.INSTANCE.getRotation().getYaw() + ff) % 90 - ff) / 45, boost = 1 + (0.3 * acceleration * acceleration);
            boolean yAcceleration = Math.abs(TurnsConnection.INSTANCE.getMoveRotation().getPitch()) > 60;
            Vec3d vec3d = e.getVector();
            e.setVector(new Vec3d(vec3d.x * boost, yAcceleration ? vec3d.y * boost : vec3d.y, vec3d.z * boost));
        } else if (modeSetting.isSelected("Кастом")) {
            int ff = TurnsConnection.INSTANCE.getRotation().getYaw() > 0F ? 45 : -45;
            double acceleration = Math.abs((TurnsConnection.INSTANCE.getRotation().getYaw() + ff) % 90 - ff) / 45;
            double rotationBoost = 1 + (0.3 * acceleration * acceleration);
            boolean yAcceleration = Math.abs(TurnsConnection.INSTANCE.getMoveRotation().getPitch()) > 60;

            Vec3d direction = TurnsConnection.INSTANCE.getMoveRotation().toVector();
            float speed = speedSetting.getValue() / 20f;

            double finalSpeed = speed * rotationBoost;
            e.setVector(new Vec3d(
                    direction.x * finalSpeed,
                    yAcceleration ? direction.y * finalSpeed : direction.y * speed,
                    direction.z * finalSpeed
            ));
        }
    }
}