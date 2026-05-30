package code.essence.features.impl.movement.autoMystGlider.states;

import code.essence.features.impl.misc.creeperFarm.WalkDirectionUtil;
import code.essence.utils.display.interfaces.QuickImports;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * @author nikitavodolaz
 * @since 01.02.2026
 */

public record GlideState(Vec3d vec3d) implements QuickImports {

    public boolean done() {
        if (mc.player == null) return false;
        Vec3d curPos = mc.player.getPos();
        Vec3d delta = vec3d.subtract(curPos);
        return delta.horizontalLengthSquared() < 3;
    }

    public boolean horizontalGlide() {
        if (mc.player == null) return false;
        Vec3d curPos = mc.player.getPos();
        Vec3d delta = vec3d.subtract(curPos);
        return !verticalGlide() && delta.horizontalLengthSquared() > 3;
    }

    public boolean verticalGlide() {
        if (mc.player == null) return false;
        Vec3d curPos = mc.player.getPos();
        Vec3d delta = vec3d.subtract(curPos);
        return delta.getY() > -130 && !done();
    }

    public boolean solidBlockInRadius(int radius) {
        BlockPos blockPos = BlockPos.ofFloored(mc.player.getPos());

        for (int i = 0; i < radius; i++) {
            BlockPos blockPos1 = blockPos.withY(blockPos.down().getY() - i);
            if (!mc.player.getWorld().getBlockState(blockPos1).isAir()) {
                return true;
            }
        }

        return false;
    }

    public boolean shouldLand() {
        if (mc.player == null || mc.world == null) return false;
        return !solidBlockInRadius(2);
    }

    public boolean fullyDone() {
        return vec3d.getY() - mc.player.getPos().getY() > 3;
    }

    public PlayerInput selectInput() {
//        Vec3d direction = vec3d.subtract(mc.player.getPos()).normalize();
//        float yaw = mc.player.getYaw();
//        float movementAngle = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90F;
//        float angleDiff = MathHelper.wrapDegrees(movementAngle - yaw);

//        return MoveUtil.getPlayerInput(angleDiff);

        return WalkDirectionUtil.fromVec3d(vec3d.subtract(mc.player.getPos()), mc.player.getYaw());
    }
}