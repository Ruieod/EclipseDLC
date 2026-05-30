package code.essence.features.impl.combat.ElytraTargetUtil;

import code.essence.utils.features.aura.rotations.constructor.RotateConstructor;
import code.essence.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;


public class ElytraOptimizedMode extends RotateConstructor {
    public ElytraOptimizedMode() {
        super("Elytra");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {

        return targetAngle;
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}