package code.essence.features.impl.misc.newAutoMine;

import code.essence.features.impl.misc.newAutoMine.api.MineParser;
import code.essence.utils.display.interfaces.QuickImports;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * @author nikitavodolaz
 * @since 12.02.2026
 */

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoMineStateSelector implements QuickImports {
    final AutoMineController autoMineController;

    public AutoMineStateSelector(AutoMineController autoMineController) {
        this.autoMineController = autoMineController;
    }

    public AutoMineState selectState() {
        RegionState regionState = autoMineController.getRegionState();
        MineParser mineParser = autoMineController.getMineParser();
        ConnectProvider connectProvider = autoMineController.getConnectProvider();

        return switch (autoMineController.getAutoMineState()) {
            default -> AutoMineState.ON_MINING;
        };
    }
}
