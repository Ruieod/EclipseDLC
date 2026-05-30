package code.essence.common.animation.implement;

import code.essence.common.animation.Animation;

public class ExpoInOut extends Animation {

    @Override
    public double calculation(double value) {
        double x = value / ms;
        
        if (x == 0) return 0;
        if (x == 1) return 1;
        
        return x < 0.5
                ? Math.pow(2, 20 * x - 10) / 2
                : (2 - Math.pow(2, -20 * x + 10)) / 2;
    }
}










