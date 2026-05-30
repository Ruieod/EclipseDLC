package code.essence.features.impl.movement.autoMystGlider.states;

import code.essence.features.impl.movement.autoMystGlider.handers.StealHandler;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.math.time.StopWatch;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.BlockPos;

/**
 * @author nikitavodolaz
 * @since 04.02.2026
 */

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConnectionState implements QuickImports {
    final GlideState glideState;
    final StealHandler stealHandler;

    boolean connected = true;
    int anarchy = -1;
    long leaveTime, remainingTime;
    long phaseStartTime = System.currentTimeMillis();
    GlidePhase phase = GlidePhase.FLYING;
    StopWatch timer = new StopWatch();

    public ConnectionState(GlideState glideState, StealHandler stealHandler) {
        this.glideState = glideState;
        this.stealHandler = stealHandler;
    }

    /** Обратная совместимость: reconnected = фаза LOOTING */
    public boolean isReconnected() {
        return phase == GlidePhase.LOOTING;
    }

    /** Устанавливает фазу и запоминает время начала */
    public void transitionTo(GlidePhase newPhase) {
        this.phase = newPhase;
        this.phaseStartTime = System.currentTimeMillis();
    }

    /** Отправляет /hub и переходит в указанную фазу */
    public void hub(GlidePhase nextPhase) {
        if (timer.finished(1000)) {
            timer.reset();
            connected = false;
            leaveTime = System.currentTimeMillis();
            mc.getNetworkHandler().sendCommand("hub");
            transitionTo(nextPhase);
        }
    }

    /** Заходит на анархию и переходит в указанную фазу */
    public void joinAnarchy(int num, GlidePhase nextPhase) {
        if (timer.finished(1000)) {
            timer.reset();
            connected = true;
            anarchy = num;
            mc.getNetworkHandler().sendCommand("an" + num);
            transitionTo(nextPhase);
        }
    }

    public void reload() {
        connected = true;
        leaveTime = -1;
        remainingTime = -1;
        phase = GlidePhase.FLYING;
        phaseStartTime = System.currentTimeMillis();
        timer.reset();
    }

    public boolean successfully() {
        return mc.world.getBlockState(BlockPos.ofFloored(mc.player.getPos())).isSolid();
    }
}
