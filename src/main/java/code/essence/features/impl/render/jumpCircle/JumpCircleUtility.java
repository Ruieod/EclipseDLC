package code.essence.features.impl.render.jumpCircle;

public class JumpCircleUtility {
    public static boolean isExpired(long startTime, float lifeTime) {
        return System.currentTimeMillis() - startTime > lifeTime;
    }
}
