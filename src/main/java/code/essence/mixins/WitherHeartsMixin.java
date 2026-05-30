package code.essence.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import code.essence.features.impl.render.NoRender;

@Mixin(LivingEntity.class)
public class WitherHeartsMixin {

    @Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true)
    private void noRender$hideWitherEffect(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        
        LivingEntity self = (LivingEntity) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client != null && client.player != null && self == client.player) {
            
            if (effect != null && effect.value() == StatusEffects.WITHER.value()) {
                NoRender noRender = NoRender.getInstance();
                if (noRender != null && noRender.shouldHideWitherHearts()) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}

