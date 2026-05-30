package code.essence.utils.features.aura.rotations.impl;

import code.essence.utils.features.aura.rotations.constructor.RotateConstructor;
import code.essence.Essence;
import code.essence.features.impl.combat.Aura;
import code.essence.utils.features.aura.striking.StrikeManager;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.features.aura.warp.Turns;
import code.essence.utils.math.time.StopWatch;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.security.SecureRandom;

public class FTAngle extends RotateConstructor {

    public FTAngle() {
        super("FunTime");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Essence.getInstance().getAttackPerpetrator().getAttackHandler();
        int count = attackHandler.getCount();
        StopWatch attackTimer = attackHandler.getAttackTimer();

        Turns angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw(), pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        if (entity != null) {
            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);
            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);
            return new Turns( MathHelper.lerp(rand(attackHandler.canAttack(Aura.getInstance().getConfig(), 0) ? 1 : new SecureRandom().nextBoolean() ? 0.4F : 0.2F, attackHandler.canAttack(Aura.getInstance().getConfig(), 0) ? 1 : new SecureRandom().nextBoolean() ? 0.4F : 0.2F + 0.2F), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw),MathHelper.lerp(rand(attackHandler.canAttack(Aura.getInstance().getConfig(), 0) ? 1 : new SecureRandom().nextBoolean() ? 0.4F : 0.2F, attackHandler.canAttack(Aura.getInstance().getConfig(), 0) ? 1 : new SecureRandom().nextBoolean() ? 0.4F : 0.2F + 0.2F), currentAngle.getPitch(), currentAngle.getPitch() + movePitch));
        }
        else {
            Turns lerp = switch(count % 3) {
                case 0 -> new Turns((float) Math.cos(attackTimer.elapsedTime() / 40F + (count % 6)), (float) Math.sin(attackTimer.elapsedTime() / 40F + (count % 6)));
                case 1 -> new Turns((float) Math.sin(attackTimer.elapsedTime() / 40F + (count % 6)), (float) Math.cos(attackTimer.elapsedTime() / 40F + (count % 6)));
                case 2 -> new Turns((float) Math.sin(attackTimer.elapsedTime() / 40F + (count % 6)), (float) -Math.cos(attackTimer.elapsedTime() / 40F + (count % 6)));
                default -> new Turns((float) -Math.cos(attackTimer.elapsedTime() / 40F + (count % 6)), (float) Math.sin(attackTimer.elapsedTime() / 40F + (count % 6)));
            };

            float yaw = !attackTimer.finished(2000) ? rand(12, 24) * lerp.getYaw() : 0;
            float pitch2 = rand(0,2)  * (float) Math.cos((double) System.currentTimeMillis() / 5000);
            float pitch = !attackTimer.finished(2000) ? rand(2, 6) * lerp.getPitch() + pitch2 : 0;

            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            return new Turns(MathHelper.lerp(Math.clamp(rand(attackTimer.finished(400) ? new SecureRandom().nextBoolean() ? 0.4F : 0.2F : -0.2F, attackTimer.finished(400) ? new SecureRandom().nextBoolean() ? 0.4F : 0.2F : -0.2F + 0.2F), 0, 1), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + yaw, MathHelper.lerp(Math.clamp(rand(attackTimer.finished(400) ? new SecureRandom().nextBoolean() ? 0.4F : 0.2F : -0.2F, attackTimer.finished(400) ? new SecureRandom().nextBoolean() ? 0.4F : 0.2F : -0.2F + 0.2F), 0, 1), currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + pitch);
        }
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.05, 0.1, 0.02);
    }

    private float rand(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }
}