package code.essence.features.impl.render;

import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.ModuleRepository;
import code.essence.features.module.setting.implement.*;
import code.essence.Essence;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;

@Getter
public class ClickGui extends Module {
    private static ClickGui instance;
    
    public static ClickGui getInstance() {
        if (instance != null) {
            return instance;
        }
        
        try {
            Essence essence = Essence.getInstance();
            if (essence == null) {
                return null;
            }
            
            ModuleRepository moduleRepository = essence.getModuleRepository();
            if (moduleRepository == null) {
                return null;
            }
            
            instance = moduleRepository.modules().stream()
                .filter(m -> m instanceof ClickGui)
                .map(m -> (ClickGui) m)
                .findFirst()
                .orElse(null);
            
            return instance;
        } catch (Exception ignored) {
            return null;
        }
    }

    private final BooleanSetting showCustomization = new BooleanSetting("Показывать Customization", "Показывает панель Customization в GUI")
            .setValue(true);

    private final BooleanSetting showConfigs = new BooleanSetting("Показывать Configs", "Показывает панель Configs в GUI")
            .setValue(true);

    private final BooleanSetting showModuleDescriptions = new BooleanSetting("Описание модулей", "Показывает описание модуля при наведении курсора")
            .setValue(true);

    public static SliderSettings scrollSpeed = new SliderSettings("Скорость скролла", "Количество модулей за один скролл")
            .setValue(1f).range(1f, 4f).step(1);

    public ClickGui() {
        super("ClickGui", "ClickGui", ModuleCategory.RENDER);
        setup(showCustomization, showConfigs, showModuleDescriptions, scrollSpeed);
        setState(true);
        setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
        instance = this;
    }


    public boolean shouldShowModuleDescriptions() {
        return showModuleDescriptions.isValue();
    }
    @Override
    public void activate() {
        if (!isState()) {
            setState(true);
        }
    }

    @Override
    public void deactivate() {
        setState(true);
    }

    public boolean shouldRenderCustomization() {
        return showCustomization.isValue();
    }

    public boolean shouldRenderConfigs() {
        return showConfigs.isValue();
    }
    }

