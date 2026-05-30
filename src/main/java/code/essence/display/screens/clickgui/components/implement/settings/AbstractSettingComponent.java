package code.essence.display.screens.clickgui.components.implement.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import code.essence.features.module.setting.Setting;
import code.essence.features.module.Module;
import code.essence.display.screens.clickgui.components.AbstractComponent;

@Getter
@RequiredArgsConstructor
public abstract class AbstractSettingComponent extends AbstractComponent {
    private final Setting setting;
    protected Module module;
    
    public void setModule(Module module) {
        this.module = module;
    }
}
