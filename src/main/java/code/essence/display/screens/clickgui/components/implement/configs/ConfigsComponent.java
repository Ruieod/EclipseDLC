package code.essence.display.screens.clickgui.components.implement.configs;

import code.essence.display.screens.clickgui.components.AbstractComponent;
import code.essence.utils.config.ConfigManager;
import code.essence.utils.animation.AnimationHelper;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.math.calc.Calculate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;

import static code.essence.utils.display.render.font.Fonts.Type.*;

public class ConfigsComponent extends AbstractComponent {
    private List<String> configs;
    
    public final ConfigInputComponent configInput;

    public ConfigsComponent() {
        this.configs = new ArrayList<>();
        this.configInput = new ConfigInputComponent();
        updateConfigs();
    }

    private void updateConfigs() {
        configs.clear();
        String[] availableConfigs = ConfigManager.getAvailableConfigs();
        for (String configName : availableConfigs) {
            configs.add(configName);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        updateConfigs();
        
        for (int i = 0; i < configs.size(); i++) {
            String configName = configs.get(i);
            renderConfigItem(context, matrix, configName, i, mouseX, mouseY, delta);
        }
    }

    private void renderConfigItem(DrawContext context, MatrixStack matrix, String configName, int index, int mouseX, int mouseY, float delta) {
        float itemY = y + index * 22;
        
        String deleteAnimationKey = "config_delete_" + configName;
        float deleteScale = AnimationHelper.getAnimationValue(deleteAnimationKey, 1f);
        float deleteAlpha = AnimationHelper.getAnimationValue(deleteAnimationKey + "_alpha", 1f);
        
        String addAnimationKey = "config_add_" + configName;
        float addScale = AnimationHelper.getAnimationValue(addAnimationKey, 1f);
        float addAlpha = AnimationHelper.getAnimationValue(addAnimationKey + "_alpha", 1f);
        
        float finalScale = AnimationHelper.isAnimationActive(addAnimationKey) ? addScale : deleteScale;
        float finalAlpha = AnimationHelper.isAnimationActive(addAnimationKey) ? addAlpha : deleteAlpha;
        
        int backgroundColor = new Color(32, 32, 35, (int)(255 * finalAlpha)).getRGB();
        
        rectangle.render(ShapeProperties.create(matrix, x, itemY, width * finalScale, 20)
                .round(5)
                .color(backgroundColor)
                .build());
        
        context.getMatrices().push();
        context.getMatrices().scale(finalScale, finalScale, 1f);
        
        Fonts.getSize(15, SuisseIntlSemiBold).drawString(context.getMatrices(), configName, (x + 5) / finalScale, (itemY + 16.5f - Fonts.getSize(15, SuisseIntlSemiBold).getStringHeight(configName) / 2f) / finalScale, new Color(255, 255, 255, (int)(255 * finalAlpha)).getRGB());
        
        String icon = "m";
        float iconX = (x + width - 15) / finalScale;
        float iconY = (itemY + 16.5f - Fonts.getSize(13, Fonts.Type.ESSENCE).getStringHeight(icon) / 2f) / finalScale;
        Fonts.getSize(13, Fonts.Type.ESSENCE).drawString(context.getMatrices(), icon, iconX, iconY, new Color(255, 255, 255, (int)(255 * finalAlpha)).getRGB());
        
        context.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < configs.size(); i++) {
            String configName = configs.get(i);
            float itemY = y + i * 22;
            
            if (Calculate.isHovered(mouseX, mouseY, x, itemY, width, 20)) {
                float iconX = x + width - 15;
                float iconY = itemY + 16.5f - Fonts.getSize(13, Fonts.Type.ESSENCE).getStringHeight("m") / 2f;
                
                if (Calculate.isHovered(mouseX, mouseY, iconX + 3 , iconY - 5, 5, 10)) {
                    String deleteAnimationKey = "config_delete_" + configName;
                    AnimationHelper.startAnimation(deleteAnimationKey, 1f, 0f, 200, AnimationHelper.EasingType.EASE_IN);
                    AnimationHelper.startAnimation(deleteAnimationKey + "_alpha", 1f, 0f, 200, AnimationHelper.EasingType.EASE_IN);
                    
                    new Thread(() -> {
                        try {
                            Thread.sleep(200);
                            ConfigManager.deleteConfig(configName);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                    
                    return true;
                } else {
                    ConfigManager.loadConfig(configName);
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public int calculateHeight() {
        return configs.size() * 22;
    }
}
