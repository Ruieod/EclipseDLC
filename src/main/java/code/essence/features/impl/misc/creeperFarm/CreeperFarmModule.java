package code.essence.features.impl.misc.creeperFarm;

import code.essence.events.player.RotationUpdateEvent;
import code.essence.events.player.TickEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.client.managers.event.EventHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * @author nikitavodolaz
 * @since 07.02.2026
 */

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreeperFarmModule extends Module {
    CreeperFarmController controller = new CreeperFarmController();

    public CreeperFarmModule() {
        super("Creeper Farm", ModuleCategory.MISC);
    }

    @EventHandler
    public void tickEvent(TickEvent tickEvent) {
        controller.onTick();
    }

    @EventHandler
    public void rotationUpdateEvent(RotationUpdateEvent rotationUpdateEvent) {
        controller.onRotationUpdate();
    }
}
