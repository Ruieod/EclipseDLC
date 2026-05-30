package code.essence.features.impl.misc;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.BindSetting;
import code.essence.events.keyboard.HotBarScrollEvent;
import code.essence.events.keyboard.KeyEvent;
import code.essence.events.player.HotBarUpdateEvent;
import code.essence.events.player.TickEvent;
import code.essence.events.render.WorldRenderEvent;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.interactions.inv.InventoryTask;
import code.essence.utils.math.time.StopWatch;
import code.essence.utils.math.script.Script;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.features.impl.render.Prediction;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WindJump extends Module {

    BindSetting windChargeBind = new BindSetting("Заряд ветра", "Бросить заряд ветра");
    StopWatch stopWatch = new StopWatch();
    Script script = new Script();

    public WindJump() {
        super("WindJump", "WindJump", ModuleCategory.MISC);
        setup(windChargeBind);
    }

    @EventHandler
    public void onHotBarUpdate(HotBarUpdateEvent e) {
        if (!script.isFinished()) e.cancel();
    }

    @EventHandler
    public void onHotBarScroll(HotBarScrollEvent e) {
        if (!script.isFinished()) e.cancel();
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (e.isKeyReleased(windChargeBind.getKey())) {
            InventoryTask.swapAndUse(Items.WIND_CHARGE,false);
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (PlayerInteractionHelper.isKey(windChargeBind)) {
            ItemStack stack = Items.WIND_CHARGE.getDefaultStack();
            Prediction.getInstance().drawPredictionInHand(e.getStack(), List.of(stack), MathAngle.cameraAngle());
        }
    }

    @EventHandler
    
    public void onTick(TickEvent e) {
        if (!script.isFinished() && stopWatch.every(250)) {
            script.update();
        }
    }
}