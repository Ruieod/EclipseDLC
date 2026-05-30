package code.essence.utils.features.aura.context;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

@Getter
@Setter
public class AutoRegressionContext {
    private static final AutoRegressionContext INSTANCE = new AutoRegressionContext();
    private final LinkedList<Float> lastDeltas = new LinkedList<>();
    private final Random random = new Random();
    private static long hitContent;
    private long attTime = 0;
    private long cdMinecraft = 500;
    public long ftState = 0;

    public static AutoRegressionContext getInstance() {
        return INSTANCE;
    }

    public Queue<Float> getLastDeltas(int count) {
        while (lastDeltas.size() < count) lastDeltas.add(0f);
        return new LinkedList<>(lastDeltas);
    }

    public void addDelta(float delta) {
        lastDeltas.addFirst(delta);
        if (lastDeltas.size() > 10) {
            lastDeltas.removeLast();
        }
    }

    public static void hitContentQueue() {
        hitContent++;
    }

    public static void hitContentClear() {
        hitContent = 0;
    }

    public boolean allowTriggerContent() {
        return hitContent % 7 == 3;
    }

    public boolean isReadyContent(long ready) {
        return System.currentTimeMillis() - attTime >= getRandomizedCooldown() + ready;
    }

    public void updateLastAttackTime() {
        attTime = System.currentTimeMillis();
        ftState = attTime;
        addDelta((float) (random.nextGaussian() * 0.1 + 1.0));
    }

    private long getRandomizedCooldown() {
        float variation = allowTriggerContent() ? 50 : 0;
        float randomFactor = (float) (random.nextGaussian() * 0.1 + 1.0);
        return (long) (cdMinecraft * randomFactor + variation);
    }

    public void setCdMinecraft(long ms) {
        this.cdMinecraft = Math.max(ms, 450);
    }

    public boolean hasCooldownPassed() {
        return System.currentTimeMillis() - ftState >= 1099;
    }
}








































