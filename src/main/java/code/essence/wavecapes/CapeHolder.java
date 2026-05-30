package code.essence.wavecapes;

import code.essence.wavecapes.math.Vector2;
import code.essence.wavecapes.math.Vector3;
import code.essence.wavecapes.sim.StickSimulation;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

public interface CapeHolder {
    StickSimulation essence$getSimulation();

    default void essence$updateSimulation(final AbstractClientPlayerEntity player, final int partCount) {
        StickSimulation simulation = this.essence$getSimulation();

        if (simulation == null) {
            return;
        }

        final boolean dirty = simulation.init(partCount);
        if (dirty) {
            simulation.applyMovement(new Vector3(1.0f, 1.0f, 0.0f));
            for (int i = 0; i < 5; ++i) {
                this.essence$simulate(player);
            }
        }
        
        this.essence$simulate(player);
    }

    default void essence$simulate(AbstractClientPlayerEntity player) {
        final StickSimulation simulation = this.essence$getSimulation();
        if (simulation == null || simulation.empty()) {
            return;
        }
        
        
        final double d = player.prevCapeX - player.getX();
        final double m = player.prevCapeZ - player.getZ();
        final float n = player.prevBodyYaw + (player.bodyYaw - player.prevBodyYaw);
        final double o = MathHelper.sin(n * 0.017453292f);
        final double p = -MathHelper.cos(n * 0.017453292f);
        float heightMul = (float) WaveyCapes.heightMultiplier;
        final float straveMul = (float) WaveyCapes.straveMultiplier;
        
        if (player.isSubmergedInWater()) {
            heightMul *= 2.0f;
        }
        
        final double fallHack = MathHelper.clamp((player.prevY - player.getY()) * 10.0, 0.0, 1.0);
        
        if (player.isSubmergedInWater()) {
            simulation.setGravity(WaveyCapes.gravity / 10.0f);
        } else {
            simulation.setGravity((float) WaveyCapes.gravity);
        }
        
        final Vector3 gravity = new Vector3(0.0f, -1.0f, 0.0f);
        final Vector2 strave = new Vector2(
                (float) (player.getX() - player.prevX), 
                (float) (player.getZ() - player.prevZ)
        );
        strave.rotateDegrees(-player.getYaw());
        
        final double changeX = d * o + m * p + fallHack + ((player.isSneaking() && !simulation.isSneaking()) ? 3 : 0);
        final double changeY = (player.getY() - player.prevY) * heightMul + ((player.isSneaking() && !simulation.isSneaking()) ? 1 : 0);
        final double changeZ = -strave.x * straveMul;
        
        simulation.setSneaking(player.isSneaking());
        final Vector3 change = new Vector3((float) changeX, (float) changeY, (float) changeZ);
        
        if (player.isSwimming()) {
            float rotation = player.getPitch();
            rotation += 90.0f;
            gravity.rotateDegrees(rotation);
            change.rotateDegrees(rotation);
        }
        
        simulation.setGravityDirection(gravity);
        simulation.applyMovement(change);
        simulation.simulate();
    }
}
