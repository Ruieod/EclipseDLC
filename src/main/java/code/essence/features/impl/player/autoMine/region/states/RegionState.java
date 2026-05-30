package code.essence.features.impl.player.autoMine.region.states;

import code.essence.features.impl.player.autoMine.region.utils.RandStack;
import code.essence.utils.display.interfaces.QuickImports;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static code.essence.features.impl.player.autoMine.region.utils.WorldUtility.fromBlockPos;

/**
 * @author nikitavodolaz
 * @since 08.02.2026
 */

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegionState implements QuickImports {
    final BlockPos min, max;
    final RandStack<BlockPos> randStack = new RandStack<>();

    public RegionState(BlockPos min, BlockPos max) {
        this.min = min;
        this.max = max;
    }

    public void eachBlocks(Predicate<BlockState> predicate, Consumer<BlockPos> consumer) {
        for (BlockPos blockPos : BlockPos.iterate(min, max)) {
            BlockState blockState = fromBlockPos(blockPos);

            if (predicate.test(blockState)) {
                consumer.accept(blockPos.toImmutable());
            }
        }
    }

    /**
     * Итерирует блоки от позиции игрока расширяющимися кубами.
     * Сначала ищет в радиусе 1, потом 2, потом 3 и т.д.
     * Это гарантирует что ближайшие блоки будут найдены первыми.
     */
    public void eachBlocksFromPlayer(BlockPos playerPos, Predicate<BlockState> predicate, Consumer<BlockPos> consumer) {
        int maxRadius = Math.max(
                Math.max(Math.abs(max.getX() - min.getX()), Math.abs(max.getY() - min.getY())),
                Math.abs(max.getZ() - min.getZ())
        ) + 1;

        for (int radius = 0; radius <= maxRadius; radius++) {
            // Итерируем по "оболочке" куба с данным радиусом
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        // Пропускаем внутренние блоки (только оболочка)
                        if (Math.abs(dx) != radius && Math.abs(dy) != radius && Math.abs(dz) != radius) {
                            continue;
                        }

                        int x = playerPos.getX() + dx;
                        int y = playerPos.getY() + dy;
                        int z = playerPos.getZ() + dz;

                        // Проверяем что блок в пределах региона
                        if (x < min.getX() || x > max.getX() ||
                            y < min.getY() || y > max.getY() ||
                            z < min.getZ() || z > max.getZ()) {
                            continue;
                        }

                        BlockPos blockPos = new BlockPos(x, y, z);
                        BlockState blockState = fromBlockPos(blockPos);

                        if (predicate.test(blockState)) {
                            consumer.accept(blockPos.toImmutable());
                        }
                    }
                }
            }
        }
    }

    /**
     * Находит все валидные блоки, отсортированные по расстоянию от игрока.
     */
    public List<BlockPos> findBlocksSortedByDistance(BlockPos playerPos, Predicate<BlockState> predicate) {
        List<BlockPos> blocks = new ArrayList<>();

        for (BlockPos blockPos : BlockPos.iterate(min, max)) {
            BlockState blockState = fromBlockPos(blockPos);
            if (predicate.test(blockState)) {
                blocks.add(blockPos.toImmutable());
            }
        }

        blocks.sort(Comparator.comparingDouble(playerPos::getSquaredDistance));

        return blocks;
    }

    public boolean inState(BlockPos blockPos) {
        return blockPos.getX() >= min.getX() && blockPos.getX() <= max.getX() &&
               blockPos.getY() >= min.getY() && blockPos.getY() <= max.getY() &&
               blockPos.getZ() >= min.getZ() && blockPos.getZ() <= max.getZ();
    }

    public Box asBox() {
        return new Box(new Vec3d(min.getX(), min.getY(), min.getZ()), new Vec3d(max.getX(), max.getY(), max.getZ()));
    }

    /**
     * Возвращает центр региона (для бега к нему).
     */
    public BlockPos getCenter() {
        return new BlockPos(
                (min.getX() + max.getX()) / 2,
                (min.getY() + max.getY()) / 2,
                (min.getZ() + max.getZ()) / 2
        );
    }

    /**
     * Возвращает центр региона как Vec3d.
     */
    public Vec3d getCenterVec() {
        return new Vec3d(
                (min.getX() + max.getX()) / 2.0,
                (min.getY() + max.getY()) / 2.0,
                (min.getZ() + max.getZ()) / 2.0
        );
    }
}
