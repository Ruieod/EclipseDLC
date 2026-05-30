package code.essence.common.animation.implement;

import code.essence.common.animation.Animation;

public class CircOut extends Animation {

    @Override
    public double calculation(double value) {
        double x = value / ms;
        return Math.sqrt(1 - Math.pow(x - 1, 2));
    }
}










