package code.essence.features.impl.misc.newAutoMine;

import code.essence.utils.display.interfaces.QuickImports;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * @author nikitavodolaz
 * @since 12.02.2026
 */

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConnectProvider implements QuickImports {
    long lastConnectTime = -1;
    MiningAnarchyState currentState = null;

    public void connect(MiningAnarchyState miningAnarchyState) {
        if (miningAnarchyState != null && !miningAnarchyState.equals(currentState) && System.currentTimeMillis() - lastConnectTime > 2000) {
            mc.getNetworkHandler().sendCommand("an" + miningAnarchyState.getAnarchy());
            lastConnectTime = System.currentTimeMillis();
            currentState = miningAnarchyState;
        }
    }
}
