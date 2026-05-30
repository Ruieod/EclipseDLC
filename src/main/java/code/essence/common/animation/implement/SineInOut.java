package code.essence.common.animation.implement;

import code.essence.common.animation.Animation;

public class SineInOut extends Animation {

    @Override
    public double calculation(double value) {
        double x = value / ms;
        return -(Math.cos(Math.PI * x) - 1) / 2;
    }
}










