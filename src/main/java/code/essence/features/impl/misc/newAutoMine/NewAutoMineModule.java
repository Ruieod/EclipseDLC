package code.essence.features.impl.misc.newAutoMine;

import code.essence.events.player.InputEvent;
import code.essence.events.player.RotationUpdateEvent;
import code.essence.events.player.TickEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.client.managers.event.EventHandler;

/**
 * @author nikitavodolaz
 * @since 12.02.2026
 */

public class NewAutoMineModule extends Module {
    AutoMineController controller = new AutoMineController();

    public NewAutoMineModule() {
        super("Test Auto Mine", ModuleCategory.MISC);
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        controller.onTick();
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent rotationUpdateEvent) {
        controller.onRotationUpdate(rotationUpdateEvent);
    }

    @EventHandler
    public void onInput(InputEvent inputEvent) {
        controller.onInput(inputEvent);
    }
}
