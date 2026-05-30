package code.essence.utils.client;

import net.minecraft.client.gui.hud.InGameHud;
import code.essence.features.impl.render.NoRender;


public class TitleHider {
    private static java.lang.reflect.Field titleTicksField;
    private static java.lang.reflect.Field subtitleTicksField;
    private static boolean fieldsInitialized = false;

    public static void hideTitles(InGameHud hud) {
        NoRender noRender = NoRender.getInstance();
        if (noRender == null || !noRender.shouldHideTitles()) {
            return;
        }

        try {
            if (!fieldsInitialized) {
                initializeFields();
                fieldsInitialized = true;
            }

            if (titleTicksField != null) {
                titleTicksField.setAccessible(true);
                titleTicksField.setInt(hud, 0);
            }

            if (subtitleTicksField != null) {
                subtitleTicksField.setAccessible(true);
                subtitleTicksField.setInt(hud, 0);
            }
        } catch (Exception ignored) {
            
        }
    }

    private static void initializeFields() {
        try {
            for (java.lang.reflect.Field field : InGameHud.class.getDeclaredFields()) {
                String fieldName = field.getName().toLowerCase();
                if (fieldName.contains("title") && fieldName.contains("remain") && field.getType() == int.class) {
                    titleTicksField = field;
                } else if (fieldName.contains("subtitle") && fieldName.contains("remain") && field.getType() == int.class) {
                    subtitleTicksField = field;
                }
            }
        } catch (Exception ignored) {
            
        }
    }
}

