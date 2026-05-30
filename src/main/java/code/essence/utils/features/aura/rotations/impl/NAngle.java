package code.essence.utils.features.aura.rotations.impl;

import code.essence.utils.features.aura.rotations.constructor.RotateConstructor;
import code.essence.utils.features.aura.warp.Turns;
import code.essence.utils.features.aura.utils.MathAngle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class NAngle extends RotateConstructor {
    private static final float RETURN_SPEED = 35.0F;
    private static final float MAX_YAW_SPEED = 55.03F;
    private static final float MIN_YAW_SPEED = 42.2F;
    private static final float MAX_PITCH_SPEED = 32.2F;
    private static final float MIN_PITCH_SPEED = 9.2F;
    private static final float RANDOM_SPEED_FACTOR = 0.3F;
    private static final float YAW_RANDOM_JITTER = 3.0F;
    private static final float PITCH_RANDOM_JITTER = 0.0F;
    private static final float YAW_PITCH_COUPLING = 1.0F;
    private static final float COOLDOWN_SLOWDOWN = 1.0F;

    private final Random random = new Random();
    private float lastYawJitter;
    private float lastPitchJitter;

    public NAngle() {
        super("NAngle");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        if (mc.player == null) {
            return currentAngle;
        }

        
        if (entity == null || !(entity instanceof LivingEntity target)) {
            return currentAngle;
        }

        
        Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.58, 0);
        Vec3d eyes = mc.player.getEyePos();
        Vec3d dir = targetPos.subtract(eyes);
        
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float targetPitch = (float) Math.toDegrees(Math.asin(-dir.y / dir.length()));

        
        float yawDelta = MathHelper.wrapDegrees(targetYaw - currentAngle.getYaw());
        float pitchDelta = MathHelper.wrapDegrees(targetPitch - currentAngle.getPitch());

        
        if (Math.abs(yawDelta) < 0.01F && Math.abs(pitchDelta) < 0.01F) {
            return currentAngle;
        }

        float yawAbs = Math.abs(yawDelta);
        float pitchAbs = Math.abs(pitchDelta);

        float yawFraction = MathHelper.clamp(yawAbs / 180.0F, 0.0F, 1.0F);
        float pitchFraction = MathHelper.clamp(pitchAbs / 90.0F, 0.0F, 1.0F);

        float yawSpeed = MathHelper.lerp(yawFraction, MIN_YAW_SPEED, MAX_YAW_SPEED);
        float pitchSpeed = MathHelper.lerp(pitchFraction, MIN_PITCH_SPEED, MAX_PITCH_SPEED);

        
        float cooldown = 1.0F - MathHelper.clamp(mc.player.getAttackCooldownProgress(1), 0.0F, 1.0F);
        float slowdown = MathHelper.lerp(cooldown, 1.0F, COOLDOWN_SLOWDOWN);
        yawSpeed *= slowdown;
        pitchSpeed *= slowdown;

        
        float randomScaleYaw = 1.0F + ((random.nextFloat() * 2.0F - 1.0F) * RANDOM_SPEED_FACTOR);
        float randomScalePitch = 1.0F + ((random.nextFloat() * 2.0F - 1.0F) * RANDOM_SPEED_FACTOR * YAW_PITCH_COUPLING);

        yawSpeed = MathHelper.clamp(yawSpeed * randomScaleYaw, MIN_YAW_SPEED, MAX_YAW_SPEED);
        pitchSpeed = MathHelper.clamp(pitchSpeed * randomScalePitch, MIN_PITCH_SPEED, MAX_PITCH_SPEED);

        
        float yawStep = MathHelper.clamp(yawDelta, -yawSpeed, yawSpeed);
        float pitchStep = MathHelper.clamp(pitchDelta, -pitchSpeed, pitchSpeed);

        
        lastYawJitter = randomJitter(YAW_RANDOM_JITTER, lastYawJitter);
        lastPitchJitter = randomJitter(PITCH_RANDOM_JITTER, lastPitchJitter);

        
        float newYaw = currentAngle.getYaw() + yawStep + lastYawJitter;
        float newPitch = MathHelper.clamp(currentAngle.getPitch() + pitchStep + lastPitchJitter, -89.0F, 90.0F);

        Turns moveAngle = new Turns(newYaw, newPitch);

        
        return moveAngle.adjustSensitivity();
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.1, 0.1, 0.1);
    }

    private float randomJitter(float bound, float previous) {
        if (bound <= 0.0F) return 0.0F;
        float next = (random.nextFloat() * 2.0F - 1.0F) * bound;
        return MathHelper.lerp(0.35F, previous, next);
    }
}
