package code.essence.features.impl.player.autoMine.controllers;

import code.essence.events.render.WorldRenderEvent;
import code.essence.features.impl.player.autoMine.region.states.RegionBlockState;
import code.essence.features.impl.player.autoMine.region.states.RegionComparators;
import code.essence.features.impl.player.autoMine.region.states.RegionState;
import code.essence.features.impl.player.autoMine.region.utils.WorldUtility;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.display.render.geometry.Render3D;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Comparator;
import java.util.List;

import static code.essence.features.impl.player.autoMine.region.states.RegionBlockMaps.IMMUTABLE_BLOCK_MAP;

/**
 * @author nikitavodolaz
 * @since 08.02.2026
 */

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegionController implements QuickImports {
    final RegionState regionState;
    long lastScanTime = -1;
    BlockPos targetBlock = null;

    static final int MAX_REACH_UP = 4;
    static final int MAX_REACH_DOWN = 3;

    public RegionController(RegionState regionState) {
        this.regionState = regionState;
    }

    public static RegionState createRegion(BlockPos min, BlockPos max) {
        return new RegionState(min, max);
    }

    public static boolean isValidBlockState(BlockState blockState) {
        return blockState != null && !blockState.isAir() && IMMUTABLE_BLOCK_MAP.containsKey(blockState.getBlock());
    }

    public void onTick() {
        targetBlock = null;

        BlockPos playerPos = mc.player.getBlockPos();

        if (System.currentTimeMillis() - lastScanTime > 100 || regionState.getRandStack().isEmpty()) {
            regionState.getRandStack().clear();
            List<BlockPos> sorted = regionState.findBlocksSortedByDistance(playerPos, RegionController::isValidBlockState);
            sorted.forEach(regionState.getRandStack()::add);
            lastScanTime = System.currentTimeMillis();
        }

        updateTargetBlock();
    }

    public void onWorldRender(WorldRenderEvent worldRenderEvent) {
        Render3D.drawBox(regionState.asBox(), 0x25FFFFFF, 2f);
        drawTargetBlock();
    }

    public void onWorldChange() {
        regionState.getRandStack().clear();
        targetBlock = null;
    }

    private void drawTargetBlock() {
        if (targetBlock != null) {
            BlockState blockState = WorldUtility.fromBlockPos(targetBlock);

            if (blockState != null && IMMUTABLE_BLOCK_MAP.containsKey(blockState.getBlock())) {
                Render3D.drawBox(new Box(targetBlock), ColorAssist.setAlpha(IMMUTABLE_BLOCK_MAP.get(blockState.getBlock()).color(), 255), 2f);
            }
        }
    }

    private void updateTargetBlock() {
        if (regionState.getRandStack().isEmpty()) return;

        BlockPos playerPos = mc.player.getBlockPos();

        List<BlockPos> reachable = regionState.getRandStack().stream()
                .filter(pos -> {
                    double vertDist = WorldUtility.verticalDistance(playerPos, pos);
                    return vertDist <= MAX_REACH_UP && vertDist >= -MAX_REACH_DOWN;
                })
                .toList();

        if (reachable.isEmpty()) return;

        targetBlock = reachable.stream()
                .min(Comparator.comparingDouble(RegionComparators::score))
                .map(BlockPos::toImmutable)
                .orElse(null);
    }
}
