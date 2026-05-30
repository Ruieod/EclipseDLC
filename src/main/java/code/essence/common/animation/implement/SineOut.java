package code.essence.common.animation.implement;

import code.essence.common.animation.Animation;

public class SineOut extends Animation {

    @Override
    public double calculation(double value) {
        double x = value / ms;
        return Math.sin(x * Math.PI / 2);
    }
}










