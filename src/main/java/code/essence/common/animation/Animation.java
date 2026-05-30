package code.essence.common.animation;

import lombok.Setter;
import lombok.experimental.Accessors;
import code.essence.utils.math.time.TimerUtil;

@Setter
@Accessors(chain = true)
public abstract class Animation implements AnimationCalculation {
    protected final TimerUtil counter = new TimerUtil();
    protected int ms;
    protected double value;
    protected Direction direction = Direction.FORWARDS;

    public void reset() {
        counter.resetCounter();
    }

    public boolean isDone() {
        return counter.isReached(ms);
    }

    public boolean isFinished(Direction direction) {
        return this.direction == direction && isDone();
    }

    public void setDirection(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
            adjustTimer();
        }
    }

    public boolean isDirection(Direction direction) {
        return this.direction == direction;
    }

    private void adjustTimer() {
        counter.setTime(
                System.currentTimeMillis() - ((long) ms - Math.min(ms, counter.getTime()))
        );
    }

    public Double getOutput() {
        double time = (calculation(ms - counter.getTime())) * value;

        return direction == Direction.FORWARDS
                ? endValue()
                : isDone() ? 0.0 : time;
    }

    protected double endValue() {
        return isDone()
                ? value
                : calculation(counter.getTime()) * value;
    }
}