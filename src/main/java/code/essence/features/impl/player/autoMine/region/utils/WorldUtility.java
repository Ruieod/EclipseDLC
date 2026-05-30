package code.essence.features.impl.player.autoMine.region.utils;

import code.essence.utils.display.interfaces.QuickImports;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * @author nikitavodolaz
 * @since 08.02.2026
 */

public class WorldUtility implements QuickImports {

    public static BlockState fromBlockPos(BlockPos blockPos) {
        return mc.player.getWorld().getBlockState(blockPos);
    }

    public static double horizontalDistance(Vec3d from, Vec3d to) {
        return to.subtract(from).horizontalLength();
    }

    public static double horizontalDistance(BlockPos from, BlockPos to) {
        return horizontalDistance(Vec3d.ofCenter(from), Vec3d.ofCenter(to));
    }

    public static double verticalDistance(BlockPos from, BlockPos to) {
        return Vec3d.ofCenter(to).subtract(Vec3d.ofCenter(from)).getY();
    }
}
