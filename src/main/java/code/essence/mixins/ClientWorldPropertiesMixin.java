package code.essence.mixins;

import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.features.impl.render.WorldTweaks;

@Mixin(ClientWorld.Properties.class)
public class ClientWorldPropertiesMixin implements QuickImports {

    @Shadow private long timeOfDay;

    @Inject(method = "setTimeOfDay", at = @At("HEAD"), cancellable = true)
    public void setTimeOfDayHook(long timeOfDay, CallbackInfo ci) {
        WorldTweaks tweaks = WorldTweaks.getInstance();
        if (tweaks.state && tweaks.modeSetting.isSelected("Время")) {
            this.timeOfDay = tweaks.timeSetting.getInt() * 1000L;
            ci.cancel();
        }
    }
}
