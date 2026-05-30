package code.essence.mixins;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import code.essence.utils.features.aura.warp.TurnsConnection;

@Mixin(Item.class)
public class ItemMixin {

    @Redirect(method = "raycast", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getRotationVector(FF)Lnet/minecraft/util/math/Vec3d;"))
    private static Vec3d raycastHook(PlayerEntity player, float pitch, float yaw) {
        if (player == null) return new Vec3d(0, 0, 1);
        return TurnsConnection.INSTANCE.getRotation().toVector();
    }
}
