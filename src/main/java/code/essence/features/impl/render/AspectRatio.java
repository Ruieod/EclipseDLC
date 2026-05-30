package code.essence.features.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.events.render.AspectRatioEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AspectRatio extends Module {

    SliderSettings ratioSetting = new SliderSettings("Соотношение", "Настройка значения соотношения сторон")
            .setValue(1.0F).range(0.1F, 2.0F);


    public AspectRatio() {
        super("AspectRatio", "AspectRatio", ModuleCategory.RENDER);
        setup(ratioSetting);
    }

    @EventHandler
    public void onAspectRatio(AspectRatioEvent e) {
            e.setRatio(ratioSetting.getValue());
            e.cancel();
    }
}