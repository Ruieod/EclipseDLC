package code.essence.features.impl.misc.creeperFarm;

import code.essence.utils.input.MoveUtil;
import lombok.NonNull;
import net.minecraft.entity.Entity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * @author nikitavodolaz
 * @since 07.02.2026
 */

public class WalkDirectionUtil {
    public static PlayerInput fromVec3d(Vec3d vec3d, float yaw) {
        Vec3d direction = vec3d.normalize();
        float movementAngle = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90F;
        float angleDiff = MathHelper.wrapDegrees(movementAngle - yaw);
        return MoveUtil.getPlayerInput(angleDiff);
    }

    public static PlayerInput fromEntity(@NonNull Entity entity, float yaw) {
        return fromVec3d(entity.getPos(), yaw);
    }
}
