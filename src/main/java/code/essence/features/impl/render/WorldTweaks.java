package code.essence.features.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.MultiSelectSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.client.Instance;
import code.essence.events.render.FogEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorldTweaks extends Module {
    public static WorldTweaks getInstance() {
        return Instance.get(WorldTweaks.class);
    }

    public final MultiSelectSetting modeSetting = new MultiSelectSetting("Настройки мира", "Позволяет настроить мир")
            .value("Яркость", "Время", "Туман");

    public final SliderSettings brightSetting = new SliderSettings("Яркость", "Устанавливает значение максимальной яркости")
            .setValue(1.0F).range(0.0F, 1.0F).visible(() -> modeSetting.isSelected("Яркость"));

    public final SliderSettings timeSetting = new SliderSettings("Время", "Устанавливает значение времени")
            .setValue(12).range(0, 24).visible(() -> modeSetting.isSelected("Время"));

    public final SliderSettings distanceSetting = new SliderSettings("Дистанция тумана", "Устанавливает расстояние тумана")
            .setValue(100).range(20, 200).visible(() -> modeSetting.isSelected("Туман"));

    public WorldTweaks() {
        super("WorldTweaks", "WorldTweaks", ModuleCategory.RENDER);
        setup(modeSetting, brightSetting, timeSetting, distanceSetting);
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @EventHandler
    public void onFog(FogEvent e) {
        if (modeSetting.isSelected("Туман")) {
            e.setDistance(distanceSetting.getValue());
            e.setColor(ColorAssist.getClientColor());
            e.cancel();
        }
    }
}
