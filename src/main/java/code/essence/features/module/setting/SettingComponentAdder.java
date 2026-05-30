package code.essence.features.module.setting;

import code.essence.features.module.setting.implement.*;
 import code.essence.display.screens.clickgui.components.implement.settings.*;
import code.essence.display.screens.clickgui.components.implement.settings.multiselect.MultiSelectComponent;
import code.essence.display.screens.clickgui.components.implement.settings.RadioComponent;

import java.util.List;

public class SettingComponentAdder {
    public void addSettingComponent(List<Setting> settings, List<AbstractSettingComponent> components, code.essence.features.module.Module module) {
        settings.forEach(setting -> {
            AbstractSettingComponent component = null;
            
            if (setting instanceof BooleanSetting booleanSetting) {
                component = new CheckboxComponent(booleanSetting);
            }

            if (setting instanceof BindSetting bindSetting) {
                component = new BindComponent(bindSetting);
            }

            if (setting instanceof ColorSetting colorSetting) {
                component = new ColorComponent(colorSetting);
            }

            if (setting instanceof TextSetting textSetting) {
                component = new TextComponent(textSetting);
            }

            if (setting instanceof SliderSettings valueSetting) {
                component = new SliderComponent(valueSetting);
            }

            if (setting instanceof GroupSetting groupSetting) {
                component = new GroupComponent(groupSetting);
            }

            if (setting instanceof ButtonSetting buttonSetting) {
                component = new SButtonComponent(buttonSetting);
            }

            if (setting instanceof SelectSetting selectSetting) {
                component = new RadioComponent(selectSetting);
            }

            if (setting instanceof MultiSelectSetting multiSelectSetting) {
                component = new MultiSelectComponent(multiSelectSetting);
            }

            if (setting instanceof RadioSetting radioSetting) {
                component = new RadioComponent(radioSetting);
            }
            
            if (component != null) {
                component.setModule(module);
                components.add(component);
            }
        });
    }
}
