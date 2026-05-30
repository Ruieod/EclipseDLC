package code.essence.features.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.display.color.ColorAssist;
import code.essence.events.render.EntityColorEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeeInvisible extends Module {
    SliderSettings alphaSetting = new SliderSettings("Прозрачность", "Прозрачность игрока").setValue(0.5f).range(0.1F, 1);
    public SeeInvisible() {
        super("SeeInvisible", "SeeInvisible", ModuleCategory.RENDER);
        setup(alphaSetting);
    }

    @EventHandler
    public void onEntityColor(EntityColorEvent e) {
        e.setColor(ColorAssist.multAlpha(e.getColor(), alphaSetting.getValue()));
        e.cancel();
    }

}
