package code.essence.features.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

/**
 * @author nikitavodolaz
 * @since 30.01.2026
 */

public interface TargetRenderProvider {

    void render(MatrixStack matrixStack, LivingEntity living, Vec3d vec3d, float delta);

}
