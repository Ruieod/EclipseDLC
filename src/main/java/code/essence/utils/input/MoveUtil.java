package code.essence.utils.input;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;

public class MoveUtil {

    private static final MinecraftClient mc = MinecraftClient.getInstance();


    public static double getMotion() {
        if (mc.player == null) return 0;
        Vec3d velocity = mc.player.getVelocity();
        return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }


    public static void setMotion(double speed) {
        if (mc.player == null) return;

        float forward = mc.player.input.movementForward;
        float sideways = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();

        if (forward == 0 && sideways == 0) {
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
            return;
        }

        if (forward != 0) {
            if (sideways > 0) {
                yaw -= (forward > 0 ? 45 : -45);
            } else if (sideways < 0) {
                yaw += (forward > 0 ? 45 : -45);
            }
            sideways = 0;
            forward = forward > 0 ? 1 : -1;
        }

        double rad = Math.toRadians(yaw + 90);
        double motionX = forward * speed * Math.cos(rad) + sideways * speed * Math.sin(rad);
        double motionZ = forward * speed * Math.sin(rad) - sideways * speed * Math.cos(rad);

        mc.player.setVelocity(motionX, mc.player.getVelocity().y, motionZ);
    }


    public static void strafe() {
        strafe(getMotion());
    }


    public static void strafe(double speed) {
        if (mc.player == null) return;

        float forward = mc.player.input.movementForward;
        float sideways = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();

        if (forward == 0 && sideways == 0) {
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
            return;
        }

        if (forward != 0) {
            if (sideways > 0) {
                yaw -= (forward > 0 ? 45 : -45);
            } else if (sideways < 0) {
                yaw += (forward > 0 ? 45 : -45);
            }
            sideways = 0;
            forward = forward > 0 ? 1 : -1;
        }

        double rad = Math.toRadians(yaw + 90);
        mc.player.setVelocity(
                forward * speed * Math.cos(rad) + sideways * speed * Math.sin(rad),
                mc.player.getVelocity().y,
                forward * speed * Math.sin(rad) - sideways * speed * Math.cos(rad)
        );
    }


    public static boolean isMoving() {
        if (mc.player == null) return false;
        return mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
    }


    public static double getDirection() {
        if (mc.player == null) return 0;

        float forward = mc.player.input.movementForward;
        float sideways = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();

        if (forward < 0) {
            yaw += 180;
        }

        float strafe = 1;
        if (forward < 0) {
            strafe = -0.5f;
        } else if (forward > 0) {
            strafe = 0.5f;
        }

        if (sideways > 0) {
            yaw -= 90 * strafe;
        }
        if (sideways < 0) {
            yaw += 90 * strafe;
        }

        return Math.toRadians(yaw);
    }


    public static double getBaseSpeed() {
        if (mc.player == null) return 0.2873;

        double baseSpeed = 0.2873;

        if (mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SPEED)) {
            int amplifier = mc.player.getStatusEffect(net.minecraft.entity.effect.StatusEffects.SPEED).getAmplifier();
            baseSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }

        if (mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS)) {
            int amplifier = mc.player.getStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS).getAmplifier();
            baseSpeed *= 1.0 - 0.15 * (amplifier + 1);
        }

        return baseSpeed;
    }


    public static void jump() {
        if (mc.player == null) return;
        mc.player.jump();
    }


    public static void setSpeed(double speed, float yaw) {
        if (mc.player == null) return;

        double rad = Math.toRadians(yaw + 90);
        mc.player.setVelocity(
                speed * Math.cos(rad),
                mc.player.getVelocity().y,
                speed * Math.sin(rad)
        );
    }

    public static void addMotion(double speed) {
        if (mc.player == null) return;

        float yaw = mc.player.getYaw();
        double rad = Math.toRadians(yaw + 90);

        Vec3d velocity = mc.player.getVelocity();
        mc.player.setVelocity(
                velocity.x + speed * Math.cos(rad),
                velocity.y,
                velocity.z + speed * Math.sin(rad)
        );
    }

    public static double getSpeedBPS() {
        return getMotion() * 20;
    }


    public static boolean isOnGround() {
        return mc.player != null && mc.player.isOnGround();
    }

    
    public static Vec3d predictPosition(int ticks) {
        if (mc.player == null) return Vec3d.ZERO;

        Vec3d pos = mc.player.getPos();
        Vec3d velocity = mc.player.getVelocity();

        for (int i = 0; i < ticks; i++) {
            pos = pos.add(velocity);
            velocity = velocity.multiply(0.91, 0.98, 0.91); 
        }

        return pos;
    }

    public static PlayerInput getPlayerInput(float angleDiff) {
        boolean forward = false, back = false, left = false, right = false;

        if (angleDiff >= -22.5 && angleDiff < 22.5) {
            forward = true;
        } else if (angleDiff >= 22.5 && angleDiff < 67.5) {
            forward = true;
            right = true;
        } else if (angleDiff >= 67.5 && angleDiff < 112.5) {
            right = true;
        } else if (angleDiff >= 112.5 && angleDiff < 157.5) {
            back = true;
            right = true;
        } else if (angleDiff >= -67.5 && angleDiff < -22.5) {
            forward = true;
            left = true;
        } else if (angleDiff >= -112.5 && angleDiff < -67.5) {
            left = true;
        } else if (angleDiff >= -157.5 && angleDiff < -112.5) {
            back = true;
            left = true;
        } else {
            back = true;
        }

        return new PlayerInput(forward, back, left, right, false, false, false);
    }
}