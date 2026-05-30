package code.essence.features.impl.misc.newAutoMine;

import code.essence.features.impl.player.autoMine.api.MineType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * @author nikitavodolaz
 * @since 12.02.2026
 */

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MiningAnarchyState {
    int anarchy;
    MineType mineType;
    long startTime;
    int time;

    public MiningAnarchyState(int anarchy, MineType mineType, int time) {
        this.anarchy = anarchy;
        this.mineType = mineType;
        this.startTime = System.currentTimeMillis();
        this.time = time;
    }

    public MiningAnarchyState(String anarchy, MineType mineType, int time) {
        this(Integer.parseInt(anarchy.replaceAll("\\D", "")), mineType, time);
    }

    public long getRemainingTime() {
        return time - ((System.currentTimeMillis() - startTime) / 1000);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof MiningAnarchyState state && state.anarchy == this.anarchy);
    }
}
