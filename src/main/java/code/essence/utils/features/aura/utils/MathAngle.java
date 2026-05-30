package code.essence.utils.features.aura.utils;

import code.essence.utils.client.chat.ChatMessage;
import code.essence.utils.features.aura.warp.Turns;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.features.impl.combat.Aura;

import static java.lang.Math.hypot;
import static java.lang.Math.toDegrees;
import static net.minecraft.util.math.MathHelper.wrapDegrees;

@UtilityClass
public class MathAngle implements QuickImports {
    public Turns fromVec2f(Vec2f vector2f) {
        return new Turns(vector2f.y, vector2f.x);
    }
    public static float computeAngleDifference(float a, float b) {
        return MathHelper.wrapDegrees(a - b);
    }
    public Turns fromVec3d(Vec3d vector) {
        return new Turns((float) wrapDegrees(toDegrees(Math.atan2(vector.z, vector.x)) - 90), (float) wrapDegrees(toDegrees(-Math.atan2(vector.y, hypot(vector.x, vector.z)))));
    }

    public Turns calculateDelta(Turns start, Turns end) {
        float deltaYaw = MathHelper.wrapDegrees(end.getYaw() - start.getYaw());
        float deltaPitch = MathHelper.wrapDegrees(end.getPitch() - start.getPitch());
        return new Turns(deltaYaw, deltaPitch);
    }

    public Turns calculateAngle(Vec3d to) {
        if (mc.player == null) return new Turns(0, 0);
        return fromVec3d(to.subtract(mc.player.getEyePos()));
    }

    public Turns pitch(float pitch) {
        if (mc.player == null) return new Turns(0, pitch);
        return new Turns(mc.player.getYaw(), pitch);
    }

    public Turns cameraAngle() {
        if (mc.player == null) return new Turns(0, 0);
        return new Turns(mc.player.getYaw(), mc.player.getPitch());
    }


    public static boolean rayTrace(float yaw, float pitch, float distance, float wallDistance, Entity entity) {
        if (mc.player == null || mc.world == null) return false;
        HitResult result = rayTrace(distance, yaw, pitch);
        Vec3d startPoint = mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        double distancePow2 = Math.pow(distance, 2);
        Aura attackAuraModule1 = Aura.getInstance();
        if (result != null) distancePow2 = startPoint.squaredDistanceTo(result.getPos());
        Vec3d rotationVector = getRotationVector(pitch, yaw).multiply(distance);
        Vec3d endPoint = startPoint.add(rotationVector);
        Box entityArea = mc.player.getBoundingBox().stretch(rotationVector).expand(1.0, 1.0, 1.0);
        EntityHitResult ehr;
        double maxDistance = Math.max(distancePow2, Math.pow(wallDistance, 2));
        ehr = ProjectileUtil.raycast(mc.player, startPoint, endPoint, entityArea, e -> !e.isSpectator() && e.canHit() && e == entity, maxDistance);
        if (ehr != null) {
            boolean allowedWallDistance = startPoint.squaredDistanceTo(ehr.getPos()) <= Math.pow(wallDistance, 2);
            boolean wallMissing = result == null;
            boolean wallBehindEntity = startPoint.squaredDistanceTo(ehr.getPos()) < distancePow2;
            if (startPoint.squaredDistanceTo(ehr.getPos()) <= Math.pow(distance, 2)) {
                double minY = entity.getY();
                double targetHeight = entity.getHeight();
                double minHitY = minY + targetHeight * 0.3;
                if (ehr.getPos().y >= minHitY) {
                    return ehr.getEntity() == entity;
                }
            }
        }
        return false;
    }

    public float getGCDValue() {
        double sensitivity = mc.options.getMouseSensitivity().getValue();
        double value = sensitivity * 0.6 + 0.2;
        double result = Math.pow(value, 3) * 0.8;
        return ((float) result) * 0.15f;
    }


    public static HitResult rayTrace(double dst, float yaw, float pitch) {
        if (mc.player == null || mc.world == null) return null;
        Vec3d vec3d = mc.player.getCameraPosVec(1f);
        Vec3d vec3d2 = getRotationVector(pitch, yaw);
        Vec3d vec3d3 = vec3d.add(vec3d2.x * dst, vec3d2.y * dst, vec3d2.z * dst);
        return mc.world.raycast(new RaycastContext(vec3d, vec3d3, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
    }

    public static @NotNull Vec3d getRotationVector(float yaw, float pitch) {
        return new Vec3d(MathHelper.sin(-pitch * 0.017453292F) * MathHelper.cos(yaw * 0.017453292F), -MathHelper.sin(yaw * 0.017453292F), MathHelper.cos(-pitch * 0.017453292F) * MathHelper.cos(yaw * 0.017453292F));
    }


    public static Vec3d getClosestVec(Entity entity) {
        if (mc.player == null || entity == null) {
            return Vec3d.ZERO;
        }
        Vec3d eyePosVec = mc.player.getEyePos();
        Box boundingBox = entity.getBoundingBox();
        return new Vec3d(
            MathHelper.clamp(eyePosVec.x, boundingBox.minX, boundingBox.maxX),
            MathHelper.clamp(eyePosVec.y, boundingBox.minY, boundingBox.maxY),
            MathHelper.clamp(eyePosVec.z, boundingBox.minZ, boundingBox.maxZ)
        );
    }


    public static double getDistanceEyePos(Entity target) {
        if (mc.player == null || target == null) {
            return 0;
        }
        Vec3d closestHitboxPoint = getClosestVec(target);
        return mc.player.getEyePos().distanceTo(closestHitboxPoint);
    }


    public static double getStrictDistance(Entity entity) {
        return getDistanceEyePos(entity);
    }

}
