package code.essence.utils.features.aura.rotations.impl;

import code.essence.utils.features.aura.rotations.constructor.RotateConstructor;
import code.essence.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.concurrent.ThreadLocalRandom;
import static code.essence.utils.display.interfaces.QuickImports.mc;

public class RWAngle extends RotateConstructor {

    private long rothead1 = System.currentTimeMillis();

    public RWAngle() {
        super("РилиВорлд");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        if (!(entity instanceof LivingEntity target) || mc.player == null) return currentAngle;

        long newheadtime = System.currentTimeMillis();

        Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.58, 0);
        Vec3d eyes = mc.player.getEyePos();
        Vec3d dir = targetPos.subtract(eyes);

        float newheadyaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float newheadpitch = (float) Math.toDegrees(Math.asin(-dir.y / dir.length()));

        float rotdiff1 = MathHelper.wrapDegrees(newheadyaw - currentAngle.getYaw());
        float rotdiff2 = MathHelper.wrapDegrees(newheadpitch - currentAngle.getPitch());

        float rotspeed = 82f + ThreadLocalRandom.current().nextFloat(22f);

        float headmove1 = MathHelper.clamp(rotdiff1, -rotspeed, rotspeed);
        float headmove2 = MathHelper.clamp(rotdiff2, -rotspeed * 0.78f, rotspeed * 0.78f);

        float finalyaw = currentAngle.getYaw() + headmove1;
        float finalpitch = currentAngle.getPitch() + headmove2;

        float handtremor = (float)(
                Math.sin(newheadtime / 94.0) * 0.10 +
                        Math.cos(newheadtime / 172.0) * 0.07 +
                        Math.sin(newheadtime / 311.0) * 0.04
        );

        handtremor += (ThreadLocalRandom.current().nextFloat() - 0.5f) * 5;

        float dist = mc.player.distanceTo(target);
        handtremor *= dist < 3.0f ? 0.55f : 1.0f;

        finalyaw += handtremor;
        finalpitch += handtremor * 0.68f;

        if (ThreadLocalRandom.current().nextFloat() < 0.07f) {
            finalyaw += (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.18f;
            finalpitch += (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.12f;
        }

        finalpitch = MathHelper.clamp(finalpitch, -89.9f, 89.9f);

        rothead1 = newheadtime;

        return new Turns(finalyaw, finalpitch);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }
}