package code.essence.features.impl.misc.newAutoMine;

import code.essence.events.player.InputEvent;
import code.essence.events.player.RotationUpdateEvent;
import code.essence.features.impl.misc.newAutoMine.api.MineParser;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.BlockPos;

/**
 * @author nikitavodolaz
 * @since 12.02.2026
 */

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoMineController {
    @Setter
    AutoMineState autoMineState = AutoMineState.STAY;
    final AutoMineStateSelector autoMineStateSelector;
    final RegionState regionState;
    final MineParser mineParser;
    final ConnectProvider connectProvider;

    public AutoMineController() {
        this.autoMineStateSelector = new AutoMineStateSelector(this);
        this.regionState = new RegionState(new BlockPos(-33, 76, -7), new BlockPos(-54, 85, -28));
        this.mineParser = new MineParser();
        this.connectProvider = new ConnectProvider();
    }

    public void onTick() {
        autoMineState = autoMineStateSelector.selectState();

        update();
    }

    public void onRotationUpdate(RotationUpdateEvent rotationUpdateEvent) {

    }

    public void onInput(InputEvent inputEvent) {
    }

    private void update() {
        switch (autoMineState) {
            default -> {
            }
        }
    }

    private void miningState() {

    }

    private void walkToRegionState() {

    }

    private void idlingWaitWarpState() {

    }

    private void idlingWaitAnarchyState() {

    }
}
