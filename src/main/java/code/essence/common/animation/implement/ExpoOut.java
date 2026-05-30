package code.essence.common.animation.implement;

import code.essence.common.animation.Animation;

public class ExpoOut extends Animation {

    @Override
    public double calculation(double value) {
        double x = value / ms;
        return x == 1 ? 1 : 1 - Math.pow(2, -10 * x);
    }
}










