package code.essence.display.screens.clickgui.components.implement.configs;

import code.essence.display.screens.clickgui.components.AbstractComponent;
import code.essence.utils.display.render.shape.implement.Rectangle;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.config.ConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;

public class ConfigDisplayComponent extends AbstractComponent {
    private final Rectangle rectangle = new Rectangle();
    private final List<ConfigButton> configButtons = new ArrayList<>();
    private float buttonSize = 16;
    private float buttonSpacing = -2;
    
    public ConfigDisplayComponent() {
        updateConfigButtons();
    }
    
    private void updateConfigButtons() {
        configButtons.clear();
        
        String[] availableConfigs = ConfigManager.getAvailableConfigs();
        for (String configName : availableConfigs) {
            configButtons.add(new ConfigButton(configName));
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        
        updateConfigButtons();
        
        float currentX = x + 5;
        for (ConfigButton button : configButtons) {
            button.x = currentX;
            button.y = y;
            button.size = buttonSize;

            renderConfigButton(context, matrix, button, mouseX, mouseY);
            
            currentX += buttonSize + buttonSpacing;
        }
    }
    
    private void renderConfigButton(DrawContext context, MatrixStack matrix, ConfigButton button, int mouseX, int mouseY) {
     
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (ConfigButton configButton : configButtons) {
            if (Calculate.isHovered(mouseX, mouseY, configButton.x, configButton.y, configButton.size, configButton.size)) {
                if (button == 0) {
                    loadConfig(configButton.configName);
                    return true;
                } else if (button == 1) {
                    deleteConfig(configButton.configName);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void loadConfig(String configName) {
        try {
            ConfigManager.loadConfig(configName);
            System.out.println("ConfigDisplayComponent: Config loaded: " + configName);
        } catch (Exception e) {
            System.err.println("ConfigDisplayComponent: Error loading config: " + e.getMessage());
        }
    }
    
    private void deleteConfig(String configName) {
        try {
            ConfigManager.deleteConfig(configName);
            System.out.println("ConfigDisplayComponent: Config deleted: " + configName);
        } catch (Exception e) {
            System.err.println("ConfigDisplayComponent: Error deleting config: " + e.getMessage());
        }
    }
    
    private static class ConfigButton {
        public String configName;
        public float x, y, size;
        
        public ConfigButton(String configName) {
            this.configName = configName;
        }
    }
}









