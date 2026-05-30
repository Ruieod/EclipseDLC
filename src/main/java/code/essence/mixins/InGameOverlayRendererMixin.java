package code.essence.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import code.essence.features.impl.render.NoRender;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void renderFireOverlayHook(CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.isState()) {
            if (noRender.modeSetting.isSelected("Огонь")) {
                ci.cancel();
                return;
            }
            
            if (noRender.shouldHideLavaOverlay()) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.player != null) {
                    FluidState fluidState = client.player.getWorld().getFluidState(client.player.getBlockPos());
                    if (fluidState.isIn(FluidTags.LAVA)) {
                        ci.cancel();
                    }
                }
            }
        }
    }

    @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true)
    private static void renderInWallOverlayHook(CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.isState() && noRender.modeSetting.isSelected("Оверлей блоков")) ci.cancel();
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void renderUnderwaterOverlayHook(CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.isState()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                FluidState fluidState = client.player.getWorld().getFluidState(client.player.getBlockPos().up());
                
                if (fluidState.isIn(FluidTags.WATER) && noRender.shouldHideWaterOverlay()) {
                    ci.cancel();
                    return;
                }
                if (fluidState.isIn(FluidTags.LAVA) && noRender.shouldHideLavaOverlay()) {
                    ci.cancel();
                }
            }
        }
    }
}