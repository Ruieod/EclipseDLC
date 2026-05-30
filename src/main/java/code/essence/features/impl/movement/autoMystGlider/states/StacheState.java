package code.essence.features.impl.movement.autoMystGlider.states;

import code.essence.utils.display.interfaces.QuickImports;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * @author nikitavodolaz
 * @since 07.02.2026
 */

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StacheState implements QuickImports {
    final int anarchy;
    final String homeName;
    boolean teleported = false, connected = false;

    public StacheState(int anarchy, String homeName) {
        this.anarchy = anarchy;
        this.homeName = homeName;
    }

    public void connect() {
        mc.getNetworkHandler().sendCommand("an" + anarchy);
        connected = true;
    }

    public void teleport() {
        mc.getNetworkHandler().sendCommand("home " + homeName);
        teleported = true;
    }

    public void reload() {
        teleported = false;
        connected = false;
    }
}
