package code.essence.mixins;

import code.essence.wavecapes.CapeHolder;
import code.essence.wavecapes.sim.StickSimulation;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin implements CapeHolder {
    
    @Unique
    private final StickSimulation essence$simulation = new StickSimulation();

    @Override
    public StickSimulation essence$getSimulation() {
        return essence$simulation;
    }
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        
        if ((Object) this instanceof AbstractClientPlayerEntity player && player.getWorld() != null) {
            essence$updateSimulation(player, 16);
        }
    }
}
