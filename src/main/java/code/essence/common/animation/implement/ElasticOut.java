package code.essence.common.animation.implement;

import code.essence.common.animation.Animation;

public class ElasticOut extends Animation {

    @Override
    public double calculation(double value) {
        double x = value / ms;
        
        if (x == 0) return 0;
        if (x == 1) return 1;
        
        double c4 = 2.0943951023931953;
        return Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * c4) + 1;
    }
}










