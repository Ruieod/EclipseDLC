package code.essence.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import code.essence.features.impl.render.Hud;
import code.essence.features.impl.render.NoRender;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(DrawContext context, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.shouldHideBossbar()) {
            ci.cancel();
            return;
        }
        Hud hud = Hud.getInstance();
        if (hud != null && hud.isState() && hud.interfaceSettings.isSelected("Boss Bars")) {
            ci.cancel();
        }
    }
}
