package code.essence.features.impl.player;

import code.essence.events.player.TickEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.BooleanSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.client.managers.event.EventHandler;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

public class LevitationControl extends Module{

    SliderSettings up = new SliderSettings("Скорость вверх","").setValue(0.5f).range(0,2);
    SliderSettings down = new SliderSettings("Скорость вниз","").setValue(0.5f).range(0,2);
    BooleanSetting stuck=new BooleanSetting("Зависать","Стопит вас на месте");

    public LevitationControl() {
        super("LevitationControl", ModuleCategory.PLAYER);
        setup(up, down, stuck);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.hasStatusEffect(StatusEffects.LEVITATION)) return;

        double test1214 = switch(0){
            default -> {
                if (mc.options.jumpKey.isPressed())   yield  up.getValue();
                if (mc.options.sneakKey.isPressed())  yield -down.getValue();
                if (stuck.isValue())                  yield  0.0;
                yield mc.player.getVelocity().y;
            }
        };

        if (Math.abs(mc.player.getVelocity().y - test1214) > 0.001) {
            mc.player.setVelocity(mc.player.getVelocity().x, test1214, mc.player.getVelocity().z);
        }
    }
}
