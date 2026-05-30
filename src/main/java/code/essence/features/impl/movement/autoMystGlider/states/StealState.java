package code.essence.features.impl.movement.autoMystGlider.states;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.BlockPos;

/**
 * @author nikitavodolaz
 * @since 06.02.2026
 */

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StealState {
    final BlockPos blockPos;
    boolean empty = false;

    public StealState(BlockPos blockPos) {
        this.blockPos = blockPos;
    }
}

