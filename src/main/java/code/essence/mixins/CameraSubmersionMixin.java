package code.essence.mixins;

import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import code.essence.features.impl.render.NoRender;

@Mixin(Camera.class)
public class CameraSubmersionMixin {

    @Inject(method = "getSubmersionType", at = @At("RETURN"), cancellable = true)
    private void onGetSubmersionType(CallbackInfoReturnable<CameraSubmersionType> cir) {
        NoRender noRender = NoRender.getInstance();
        
        if (noRender == null || !noRender.isState()) {
            return;
        }

        CameraSubmersionType type = cir.getReturnValue();
        if (type == null) {
            return;
        }

        
        if (type == CameraSubmersionType.WATER && noRender.shouldHideWaterOverlay()) {
            cir.setReturnValue(CameraSubmersionType.NONE);
            return;
        }

        
        if (type == CameraSubmersionType.LAVA && noRender.shouldHideLavaOverlay()) {
            cir.setReturnValue(CameraSubmersionType.NONE);
        }
    }
}

