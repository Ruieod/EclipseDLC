package code.essence.utils.math.time;

import lombok.Getter;

@Getter
public class TimerUtil {
    private long lastMS = System.currentTimeMillis();
    private long time;

    public TimerUtil() {
        this.resetCounter();
    }

    public static TimerUtil create() {
        return new TimerUtil();
    }

    public void resetCounter() {
        lastMS = System.currentTimeMillis();
    }

    public boolean isReached(long time) {
        return System.currentTimeMillis() - lastMS > time;
    }

    public void setLastMS(long newValue) {
        lastMS = System.currentTimeMillis() + newValue;
    }

    public void setTime(long time) {
        lastMS = time;
    }

    public long getTime() {
        return System.currentTimeMillis() - lastMS;
    }

    public long getPassedTimeMs() {
        return getMs(System.nanoTime() - time);
    }
    public long getMs(long time) {
        return time / 1000000L;
    }
    public boolean passedMs(long ms) {
        return getMs(System.nanoTime() - time) >= ms;
    }

    public boolean isRunning() {
        return System.currentTimeMillis() - lastMS <= 0;
    }

    public boolean hasTimeElapsed(long time) {
        return System.currentTimeMillis() - lastMS > time;
    }

    public boolean hasTimeElapsed() {
        return lastMS < System.currentTimeMillis();
    }
}