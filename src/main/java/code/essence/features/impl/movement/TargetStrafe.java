package code.essence.features.impl.movement;

import code.essence.events.player.InputEvent;
import code.essence.events.player.TickEvent;
import code.essence.features.impl.combat.Aura;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.MultiSelectSetting;
import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.client.Instance;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.features.aura.warp.TurnsConnection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class TargetStrafe extends Module {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public SelectSetting mode = new SelectSetting("Режим", "Тип стрейфа")
            .value("Матрикс", "Грим")
            .selected("Матрикс");

    SliderSettings distance = new SliderSettings("Дистанция", "Дистанция от цели")
            .setValue(0.87F).range(0F, 3F).step(0.1F)
            .visible(() -> mode.getSelected().equals("Грим"));

    MultiSelectSetting setting = new MultiSelectSetting("Настройки", "Позволяет настроить работу стрейфов")
            .value("Авто прыжок", "Только при нажатии", "Перед целью")
            .selected("Авто прыжок");

    SliderSettings radius = new SliderSettings("Радиус", "Радиус обхода вокруг цели")
            .setValue(2.5F).range(0.1F, 7F).visible(() -> mode.getSelected().equals("Матрикс"));
    SliderSettings speed = new SliderSettings("Скорость", "Скорость стрейфа")
            .setValue(0.3F).range(0.1F, 1F).visible(() -> mode.getSelected().equals("Матрикс"));

    private int grimPointIndex = 0;

    public TargetStrafe() {
        super("TargetStrafe", "TargetStrafe", ModuleCategory.MOVEMENT);
        setup(mode, distance, radius, speed, setting);
    }

    public static TargetStrafe getInstance() {
        return Instance.get(TargetStrafe.class);
    }

    @EventHandler
    public void onInput(InputEvent event) {
        if (mc.player == null || mc.world == null) return;

        LivingEntity target = Aura.getInstance().getTarget();
        if (target == null || !target.isAlive()) return;

        String modeValue = mode.getSelected();
        if (!modeValue.equals("Грим")) return;

        if (!mc.player.isSprinting() && mc.player.isOnGround()) {
            mc.player.setSprinting(true);
        }

        if (setting.isSelected("Только при нажатии")) {
            if (!mc.options.forwardKey.isPressed() &&
                    !mc.options.backKey.isPressed() &&
                    !mc.options.leftKey.isPressed() &&
                    !mc.options.rightKey.isPressed()) {
                return;
            }
        }

        Vec3d playerPos = mc.player.getPos();
        Vec3d targetPos = target.getPos();
        double r = distance.getValue();

        Vec3d nextPoint;

        if (setting.isSelected("Перед целью")) {
            float targetYaw = target.getYaw();
            double offset = Math.cos(System.currentTimeMillis() / 500.0) * 1;
            nextPoint = targetPos.add(
                    -Math.sin(Math.toRadians(targetYaw)) * r + Math.cos(Math.toRadians(targetYaw)) * offset,
                    0,
                    Math.cos(Math.toRadians(targetYaw)) * r + Math.sin(Math.toRadians(targetYaw)) * offset
            );
        } else {
            Vec3d[] points = new Vec3d[]{
                    new Vec3d(targetPos.x - r, playerPos.y, targetPos.z - r),
                    new Vec3d(targetPos.x - r, playerPos.y, targetPos.z + r),
                    new Vec3d(targetPos.x + r, playerPos.y, targetPos.z + r),
                    new Vec3d(targetPos.x + r, playerPos.y, targetPos.z - r)
            };

            nextPoint = points[grimPointIndex];
            if (playerPos.distanceTo(nextPoint) < 0.5) {
                grimPointIndex = (grimPointIndex + 1) % points.length;
            }
        }

        Vec3d direction = nextPoint.subtract(playerPos).normalize();

        float yaw = TurnsConnection.INSTANCE.getRotation().getYaw();
        float movementAngle = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90F;
        float angleDiff = MathHelper.wrapDegrees(movementAngle - yaw);

        boolean forward = false, back = false, left = false, right = false;

        if (angleDiff >= -22.5 && angleDiff < 22.5) {
            forward = true;
        } else if (angleDiff >= 22.5 && angleDiff < 67.5) {
            forward = true; right = true;
        } else if (angleDiff >= 67.5 && angleDiff < 112.5) {
            right = true;
        } else if (angleDiff >= 112.5 && angleDiff < 157.5) {
            back = true; right = true;
        } else if (angleDiff >= -67.5 && angleDiff < -22.5) {
            forward = true; left = true;
        } else if (angleDiff >= -112.5 && angleDiff < -67.5) {
            left = true;
        } else if (angleDiff >= -157.5 && angleDiff < -112.5) {
            back = true; left = true;
        } else {
            back = true;
        }

        event.setDirectional(forward, back, left, right);

        if (setting.isSelected("Авто прыжок") && mc.player.isOnGround()) {
            event.setJumping(true);
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        LivingEntity target = Aura.getInstance().getTarget();
        if (target == null || !target.isAlive()) return;

        String modeValue = mode.getSelected();
        if (!modeValue.equals("Матрикс")) return;
        
        if (!mc.player.isSprinting() && mc.player.isOnGround()) {
            mc.player.setSprinting(true);
        }

        Vec3d playerPos = mc.player.getPos();
        Vec3d targetPos = target.getPos();
        double r = radius.getValue();

        if (setting.isSelected("Только при нажатии")) {
            if (!mc.options.forwardKey.isPressed() &&
                    !mc.options.backKey.isPressed() &&
                    !mc.options.leftKey.isPressed() &&
                    !mc.options.rightKey.isPressed()) {
                return;
            }
        }

        if (setting.isSelected("Авто прыжок") && mc.player.isOnGround()) {
            mc.player.jump();
        }

        if (setting.isSelected("Перед целью")) {
            float targetYaw = target.getYaw();
            double x = targetPos.x - Math.sin(Math.toRadians(targetYaw)) * r;
            double z = targetPos.z + Math.cos(Math.toRadians(targetYaw)) * r;

            float yaw = (float) Math.toDegrees(Math.atan2(z - playerPos.z, x - playerPos.x)) - 90F;
            double motionSpeed = speed.getValue();
            mc.player.setVelocity(-Math.sin(Math.toRadians(yaw)) * motionSpeed,
                    mc.player.getVelocity().y,
                    Math.cos(Math.toRadians(yaw)) * motionSpeed);
            return;
        }

        double angle = Math.atan2(playerPos.z - targetPos.z, playerPos.x - targetPos.x);
        angle += speed.getValue() / Math.max(playerPos.distanceTo(targetPos), r);

        double x = targetPos.x + r * Math.cos(angle);
        double z = targetPos.z + r * Math.sin(angle);

        float yaw = (float) Math.toDegrees(Math.atan2(z - playerPos.z, x - playerPos.x)) - 90F;
        double motionSpeed = speed.getValue();
        mc.player.setVelocity(-Math.sin(Math.toRadians(yaw)) * motionSpeed,
                mc.player.getVelocity().y,
                Math.cos(Math.toRadians(yaw)) * motionSpeed);
    }
    @Override
    public void activate() {
        super.activate();
        grimPointIndex = 0;
    }
}
