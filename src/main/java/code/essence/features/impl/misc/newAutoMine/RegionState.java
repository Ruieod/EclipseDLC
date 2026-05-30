package code.essence.features.impl.misc.newAutoMine;

import code.essence.features.impl.player.autoMine.region.utils.WorldUtility;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author nikitavodolaz
 * @since 12.02.2026
 */

public class RegionState {
    final BlockPos min, max;

    public RegionState(BlockPos min, BlockPos max) {
        this.min = min;
        this.max = max;
    }

    public Box getBox() {
        return new Box(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
    }

    public boolean inRegion(BlockPos blockPos) {
        float minX = min.getX();
        float minY = min.getY();
        float minZ = min.getZ();

        float maxX = max.getX();
        float maxY = max.getY();
        float maxZ = max.getZ();

        return blockPos.getX() >= minX && blockPos.getY() >= minY && blockPos.getZ() >= minZ && blockPos.getX() <= maxX && blockPos.getY() <= maxY && blockPos.getZ() <= maxZ;
    }

    public List<BlockPos> eachBlocks(Predicate<BlockPos> predicate) {
        List<BlockPos> list = new ArrayList<>();
        BlockPos.iterate(min, max).forEach(blockPos -> {
            if (predicate.test(blockPos)) {
                list.add(blockPos);
            }
        });

        return list;
    }

    public double distanceTo(BlockPos blockPos) {
        return Math.min(WorldUtility.horizontalDistance(blockPos, min), WorldUtility.horizontalDistance(blockPos, max));
    }
}
