package code.essence.utils.features.aura.rotations.constructor;

import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.features.aura.warp.Turns;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ReturnConstructor extends RotateConstructor {
    private final float returnSpeed;

    public ReturnConstructor(float returnSpeed) {
        super("Return");
        this.returnSpeed = returnSpeed;
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        Turns angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();

        
        
        
        float smoothFactor = Math.max(returnSpeed * 2F, 2F); 
        float maxChange = 360.0F / smoothFactor;

        float limitedYawDelta = MathHelper.clamp(yawDelta, -maxChange, maxChange);
        float limitedPitchDelta = MathHelper.clamp(pitchDelta, -maxChange, maxChange);

        float newYaw = currentAngle.getYaw() + limitedYawDelta;
        float newPitch = MathHelper.clamp(currentAngle.getPitch() + limitedPitchDelta, -90F, 90F);

        return new Turns(newYaw, newPitch);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }
}

