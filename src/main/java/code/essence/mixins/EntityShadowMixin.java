package code.essence.mixins;

import net.minecraft.client.render.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import code.essence.features.impl.render.NoRender;

@Mixin(EntityRenderer.class)
public class EntityShadowMixin {

    @Inject(method = "getShadowRadius", at = @At("HEAD"), cancellable = true)
    private void onGetShadowRadius(CallbackInfoReturnable<Float> cir) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.shouldHideShadows()) {
            cir.setReturnValue(0.0f);
        }
    }
}

