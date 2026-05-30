package code.essence.utils.customization;

import code.essence.features.module.setting.implement.ColorSetting;
import code.essence.utils.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class CustomizationManager {
    

    private static final List<ColorSetting> customizationSettings = new ArrayList<>();
    
    static {

        customizationSettings.add(ThemeManager.primaryColor);
        customizationSettings.add(ThemeManager.secondaryColor);
        customizationSettings.add(ThemeManager.BackgroundGui);
        customizationSettings.add(ThemeManager.offModuleColor);
        customizationSettings.add(ThemeManager.BackgroundSettings);
        customizationSettings.add(ThemeManager.textColor);
    }
    

    public static List<ColorSetting> getCustomizationSettings() {
        return new ArrayList<>(customizationSettings);
    }
    

    public static int getSettingsCount() {
        return customizationSettings.size();
    }
    

    public static boolean hasSettings() {
        return !customizationSettings.isEmpty();
    }
}













