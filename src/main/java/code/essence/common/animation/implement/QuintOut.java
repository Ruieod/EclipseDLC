package code.essence.common.animation.implement;

import code.essence.common.animation.Animation;

public class QuintOut extends Animation {

    @Override
    public double calculation(double value) {
        double x = value / ms;
        return 1 - Math.pow(1 - x, 5);
    }
}










