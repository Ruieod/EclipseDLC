package code.essence.common.animation.implement;

import code.essence.common.animation.Animation;

public class ElasticInOut extends Animation {

    @Override
    public double calculation(double value) {
        double x = value / ms;
        
        if (x == 0 || x == 1) return x;
        
        double c5 = 1.3962634015954636;
        
        return x < 0.5
                ? -(Math.pow(2, 20 * x - 10) * Math.sin((20 * x - 11.125) * c5)) / 2
                : (Math.pow(2, -20 * x + 10) * Math.sin((20 * x - 11.125) * c5)) / 2 + 1;
    }
}










