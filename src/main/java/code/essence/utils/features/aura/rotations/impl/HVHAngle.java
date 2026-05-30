package code.essence.utils.features.aura.rotations.impl;

import code.essence.Essence;
import code.essence.features.impl.combat.Aura;
import code.essence.utils.features.aura.point.Vector;
import code.essence.utils.features.aura.rotations.constructor.RotateConstructor;
import code.essence.utils.features.aura.striking.StrikeManager;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.features.aura.warp.Turns;
import code.essence.utils.math.time.StopWatch;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

public class HVHAngle extends RotateConstructor {
    public HVHAngle() {
        super("HvH");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Essence.getInstance().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        StopWatch attackTimer = attackHandler.getAttackTimer();
        if (entity !=null) {
            Vec3d aimPoint = Vector.hitbox(entity, 1, 1.2F, 1, 6);
            targetAngle = MathAngle.calculateAngle(aimPoint);
        }
        Turns angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw(), pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);

        float speed = 1.1F;
        float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
        float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);
        float jitterYaw = canAttack ? 0 : (float) (6 * Math.sin(System.currentTimeMillis() / 45D));
        float jitterPitch = canAttack ? 0 : (float) (3 * Math.sin(System.currentTimeMillis() / 45D));
        if (!aura.isState()) { jitterYaw = 0; jitterPitch = 0; }
        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);
        Turns moveAngle = new Turns(currentAngle.getYaw(), currentAngle.getPitch());
        moveAngle.setYaw(lerp(speed, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + jitterYaw);
        moveAngle.setPitch(lerp(speed, currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + jitterPitch);

        return moveAngle;
    }

    public static float lerp(float delta, float start, float end) {
        return end;
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }
}