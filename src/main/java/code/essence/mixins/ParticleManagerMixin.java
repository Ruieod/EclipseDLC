package code.essence.mixins;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import code.essence.features.impl.render.NoRender;
import code.essence.features.impl.render.Particles;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true)
    private void onAddParticle(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.isState()) {
            if (noRender.shouldHideSmoke()) {
                if (parameters.getType() == ParticleTypes.SMOKE || 
                    parameters.getType() == ParticleTypes.LARGE_SMOKE ||
                    parameters.getType() == ParticleTypes.CAMPFIRE_COSY_SMOKE ||
                    parameters.getType() == ParticleTypes.CAMPFIRE_SIGNAL_SMOKE) {
                    cir.setReturnValue(null);
                    return;
                }
            }

            if (noRender.shouldHideAirBubbles()) {
                if (parameters.getType() == ParticleTypes.BUBBLE ||
                    parameters.getType() == ParticleTypes.BUBBLE_POP ||
                    parameters.getType() == ParticleTypes.CURRENT_DOWN) {
                    cir.setReturnValue(null);
                    return;
                }
            }
        }

        Particles particles2 = Particles.getInstance();
        if (particles2 != null && particles2.isState() && particles2.typep.isSelected("Тотеме")) {
            if (parameters.getType() == ParticleTypes.TOTEM_OF_UNDYING) {
                cir.setReturnValue(null);
            }
        }
    }
}

