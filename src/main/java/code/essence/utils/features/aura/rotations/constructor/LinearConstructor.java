package code.essence.utils.features.aura.rotations.constructor;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import code.essence.utils.features.aura.warp.Turns;
import code.essence.utils.features.aura.utils.MathAngle;

public class LinearConstructor extends RotateConstructor {
    public LinearConstructor() {
        super("Linear");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        Turns angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();

        float rotationDifference = (float) Math.hypot(yawDelta, pitchDelta);

        float straightLineYaw = Math.abs(yawDelta / rotationDifference) * 360.0F;
        float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * 360.0F;

        float newYaw = currentAngle.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw);
        float newPitch = currentAngle.getPitch() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch);

        return new Turns(MathHelper.lerp(0.1f, currentAngle.getYaw(), newYaw), MathHelper.lerp(0.1f, currentAngle.getPitch(), newPitch));
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }
}
