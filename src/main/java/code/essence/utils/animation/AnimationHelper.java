package code.essence.utils.animation;

import java.util.HashMap;
import java.util.Map;

public class AnimationHelper {
    private static final Map<String, AnimationData> animations = new HashMap<>();
    
    public static class AnimationData {
        public float startValue;
        public float endValue;
        public long startTime;
        public long duration;
        public EasingType easingType;
        
        public AnimationData(float startValue, float endValue, long duration, EasingType easingType) {
            this.startValue = startValue;
            this.endValue = endValue;
            this.startTime = System.currentTimeMillis();
            this.duration = duration;
            this.easingType = easingType;
        }
    }
    
    public enum EasingType {
        LINEAR,
        EASE_OUT,
        EASE_IN,
        EASE_IN_OUT,
        BOUNCE_OUT
    }
    
    public static void startAnimation(String key, float startValue, float endValue, long duration, EasingType easingType) {
        animations.put(key, new AnimationData(startValue, endValue, duration, easingType));
    }
    
    public static float getAnimationValue(String key, float defaultValue) {
        AnimationData data = animations.get(key);
        if (data == null) {
            return defaultValue;
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - data.startTime;
        
        if (elapsed >= data.duration) {
            animations.remove(key);
            return data.endValue;
        }
        
        float progress = (float) elapsed / data.duration;
        float easedProgress = applyEasing(progress, data.easingType);
        
        return data.startValue + (data.endValue - data.startValue) * easedProgress;
    }
    
    public static boolean isAnimationActive(String key) {
        return animations.containsKey(key);
    }
    
    public static void stopAnimation(String key) {
        animations.remove(key);
    }
    
    private static float applyEasing(float t, EasingType easingType) {
        switch (easingType) {
            case LINEAR:
                return t;
            case EASE_OUT:
                return 1 - (1 - t) * (1 - t);
            case EASE_IN:
                return t * t;
            case EASE_IN_OUT:
                return t < 0.5f ? 2 * t * t : 1 - (2 - 2 * t) * (2 - 2 * t) / 2;
            case BOUNCE_OUT:
                if (t < 1 / 2.75f) {
                    return 7.5625f * t * t;
                } else if (t < 2 / 2.75f) {
                    return 7.5625f * (t -= 1.5f / 2.75f) * t + 0.75f;
                } else if (t < 2.5f / 2.75f) {
                    return 7.5625f * (t -= 2.25f / 2.75f) * t + 0.9375f;
                } else {
                    return 7.5625f * (t -= 2.625f / 2.75f) * t + 0.984375f;
                }
            default:
                return t;
        }
    }
    
    public static void fadeIn(String key, long duration) {
        startAnimation(key, 0f, 1f, duration, EasingType.EASE_OUT);
    }
    
    public static void fadeOut(String key, long duration) {
        startAnimation(key, 1f, 0f, duration, EasingType.EASE_IN);
    }
    
    public static void slideIn(String key, float startY, float endY, long duration) {
        startAnimation(key, startY, endY, duration, EasingType.BOUNCE_OUT);
    }
    
    public static void colorTransition(String key, int startColor, int endColor, long duration) {
        startAnimation(key, startColor, endColor, duration, EasingType.EASE_IN_OUT);
    }
}









