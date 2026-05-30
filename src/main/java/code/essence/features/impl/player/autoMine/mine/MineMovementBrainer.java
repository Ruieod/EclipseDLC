package code.essence.features.impl.player.autoMine.mine;

import code.essence.features.impl.player.autoMine.region.utils.WorldUtility;
import code.essence.utils.display.interfaces.QuickImports;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Мозг движения: определяет нужно ли прокапывать путь до targetBlock,
 * и если да — какой блок ломать прямо сейчас.
 * <p>
 * Учитывает кирку 3×3: один сломанный блок расчищает сетку вокруг,
 * поэтому достаточно ломать один «ключевой» блок на пути.
 *
 * @author nikitavodolaz
 * @since 09.02.2026
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MineMovementBrainer implements QuickImports {
    BlockPos obstacleBlock;
    BlockPos moveTarget;
    boolean shouldDigObstacle;
    boolean pathClear;

    Vec3d lastPos;
    long lastMovedAt;
    boolean stuck;

    static final double STUCK_MOVE_THRESHOLD = 0.08;
    static final long STUCK_TIME_MS = 1200;

    public MineMovementBrainer() {
        reset();
    }

    public void reset() {
        obstacleBlock = null;
        moveTarget = null;
        shouldDigObstacle = false;
        pathClear = true;
        lastPos = null;
        lastMovedAt = System.currentTimeMillis();
        stuck = false;
    }

    public void update(BlockPos targetBlock) {
        if (targetBlock == null || mc.player == null) {
            obstacleBlock = null;
            moveTarget = null;
            shouldDigObstacle = false;
            pathClear = true;
            stuck = false;
            return;
        }

        updateStuck();

        BlockPos playerFeet = mc.player.getBlockPos();

        int dx = Integer.compare(targetBlock.getX(), playerFeet.getX());
        int dz = Integer.compare(targetBlock.getZ(), playerFeet.getZ());
        int dy = Integer.compare(targetBlock.getY(), playerFeet.getY());

        double horizontalDist = WorldUtility.horizontalDistance(playerFeet, targetBlock);
        double verticalDist = WorldUtility.verticalDistance(playerFeet, targetBlock);

        // Даже если близко — проверяем есть ли блок между игроком и targetBlock
        if (horizontalDist < 4.5 && Math.abs(verticalDist) < 4.5) {
            // Проверяем блоки между игроком и targetBlock
            BlockPos blockingBlock = findBlockBetween(playerFeet, targetBlock);
            if (blockingBlock != null) {
                // Есть блок между нами и рудой (например камень над рудой)
                obstacleBlock = blockingBlock;
                shouldDigObstacle = isInDigRange(blockingBlock);
                pathClear = false;
                moveTarget = shouldDigObstacle ? null : blockingBlock;
                return;
            }
            
            obstacleBlock = null;
            moveTarget = targetBlock;
            shouldDigObstacle = false;
            pathClear = true;
            return;
        }

        BlockPos obstacle = findObstacleOnPath(playerFeet, dx, dy, dz, targetBlock);

        if (obstacle != null) {
            obstacleBlock = obstacle;
            shouldDigObstacle = isInDigRange(obstacle);
            pathClear = false;
            moveTarget = shouldDigObstacle ? null : obstacleBlock;
        } else {
            obstacleBlock = null;
            shouldDigObstacle = false;
            pathClear = true;
            moveTarget = targetBlock;
        }

        if (stuck && pathClear) {
            BlockPos forceObstacle = findImmediateObstacle(playerFeet, dx, dz);
            if (forceObstacle != null) {
                obstacleBlock = forceObstacle;
                shouldDigObstacle = true;
                pathClear = false;
            }
        }
    }

    /**
     * Ищет твёрдый блок между игроком и targetBlock.
     * Например: игрок стоит на камне, руда под камнем — вернёт камень.
     */
    private BlockPos findBlockBetween(BlockPos playerFeet, BlockPos target) {
        // Проверяем блоки по линии от игрока до target
        int dx = Integer.signum(target.getX() - playerFeet.getX());
        int dy = Integer.signum(target.getY() - playerFeet.getY());
        int dz = Integer.signum(target.getZ() - playerFeet.getZ());

        BlockPos current = playerFeet;
        int maxSteps = 6;

        for (int step = 0; step < maxSteps; step++) {
            // Двигаемся к target
            BlockPos next;
            if (dy < 0) {
                // Target ниже — проверяем блоки под ногами
                next = current.down();
            } else if (dy > 0) {
                // Target выше — проверяем блоки над головой
                next = current.up().up(); // Над головой
            } else {
                // Target на том же уровне — двигаемся горизонтально
                next = current.add(dx, 0, dz);
            }

            // Если дошли до target — препятствий нет
            if (next.equals(target)) {
                return null;
            }

            // Если блок твёрдый и это не target — это препятствие
            if (isSolid(next) && !next.equals(target)) {
                return next;
            }

            current = next;

            // Если прошли target по оси Y
            if (dy < 0 && current.getY() <= target.getY()) break;
            if (dy > 0 && current.getY() >= target.getY()) break;
            if (dy == 0 && current.getX() == target.getX() && current.getZ() == target.getZ()) break;
        }

        return null;
    }

    private BlockPos findObstacleOnPath(BlockPos playerFeet, int dx, int dy, int dz, BlockPos target) {
        BlockPos current = playerFeet;
        int maxSteps = 10;

        for (int step = 0; step < maxSteps; step++) {
            BlockPos next;
            if (dx != 0 || dz != 0) {
                next = current.add(dx, 0, dz);
            } else {
                next = current.add(0, dy, 0);
            }

            if (isSolid(next)) {
                return next;
            }
            if (isSolid(next.up())) {
                return next.up();
            }

            if (dy > 0) {
                BlockPos upNext = next.up().up();
                if (isSolid(upNext)) {
                    return upNext;
                }
            }

            if (dy < 0) {
                BlockPos downNext = current.add(dx, -1, dz);
            }

            current = next;

            if (Math.abs(current.getX() - target.getX()) <= 1 && Math.abs(current.getZ() - target.getZ()) <= 1) {
                if (dy != 0) {
                    BlockPos vertCheck = current.add(0, dy, 0);
                    if (isSolid(vertCheck)) return vertCheck;
                    if (isSolid(vertCheck.up())) return vertCheck.up();
                }
                break;
            }
        }

        return null;
    }

    private BlockPos findImmediateObstacle(BlockPos feet, int dx, int dz) {
        if (dx != 0 || dz != 0) {
            BlockPos front = feet.add(dx, 0, dz);
            if (isSolid(front)) return front;
            if (isSolid(front.up())) return front.up();
        }

        if (dx != 0) {
            BlockPos frontX = feet.add(dx, 0, 0);
            if (isSolid(frontX)) return frontX;
            if (isSolid(frontX.up())) return frontX.up();
        }

        if (dz != 0) {
            BlockPos frontZ = feet.add(0, 0, dz);
            if (isSolid(frontZ)) return frontZ;
            if (isSolid(frontZ.up())) return frontZ.up();
        }

        if (isSolid(feet.up().up())) return feet.up().up();
        if (isSolid(feet.down())) return feet.down();

        return null;
    }

    private boolean isSolid(BlockPos pos) {
        BlockState state = WorldUtility.fromBlockPos(pos);
        if (state == null || state.isAir()) return false;
        if (!state.blocksMovement()) return false;

        return !state.isOf(Blocks.BEDROCK) && !state.isOf(Blocks.BARRIER) && !state.isOf(Blocks.COMMAND_BLOCK)
                && !state.isOf(Blocks.STRUCTURE_BLOCK) && !state.isOf(Blocks.END_PORTAL_FRAME)
                && !state.isOf(Blocks.REINFORCED_DEEPSLATE);
    }

    private boolean isInDigRange(BlockPos block) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d blockCenter = Vec3d.ofCenter(block);
        return eyePos.distanceTo(blockCenter) <= 5.0;
    }

    private void updateStuck() {
        Vec3d pos = mc.player.getPos();
        long now = System.currentTimeMillis();

        if (lastPos != null && pos.distanceTo(lastPos) > STUCK_MOVE_THRESHOLD) {
            lastMovedAt = now;
            stuck = false;
        } else if (now - lastMovedAt > STUCK_TIME_MS) {
            stuck = true;
        }

        lastPos = pos;
    }
}
