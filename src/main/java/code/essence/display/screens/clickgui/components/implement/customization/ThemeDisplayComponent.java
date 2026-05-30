package code.essence.display.screens.clickgui.components.implement.customization;

import code.essence.display.screens.clickgui.components.AbstractComponent;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.display.render.shape.implement.Rectangle;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.file.Files;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ThemeDisplayComponent extends AbstractComponent {
    private final Rectangle rectangle = new Rectangle();
    private final List<ThemeButton> themeButtons = new ArrayList<>();
    private float buttonSize = 16;
    private float buttonSpacing = -2;
    
    public ThemeDisplayComponent() {
        updateThemeButtons();
    }
    
    private void updateThemeButtons() {
        themeButtons.clear();
        
        themeButtons.add(new ThemeButton("default_dark", true));
        themeButtons.add(new ThemeButton("default_light", true));
        themeButtons.add(new ThemeButton("default_blur", true));
        
        String[] availableThemes = ThemeManager.getAvailableThemes();
        for (String themeName : availableThemes) {
            if (!themeName.equals("default_dark") && !themeName.equals("default_light") && !themeName.equals("default_blur")) {
                themeButtons.add(new ThemeButton(themeName, false));
            }
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        
        updateThemeButtons();
        
        ThemeButton hoveredButton = null;
        
        float currentX = x + 5;
        for (ThemeButton button : themeButtons) {
            button.x = currentX;
            button.y = y;
            button.size = buttonSize;
            
            renderThemeButton(context, matrix, button, mouseX, mouseY);

            if (Calculate.isHovered(mouseX + 6, mouseY + 4, button.x, button.y, button.size, button.size - 5)) {
                hoveredButton = button;
            }
            
            currentX += buttonSize + buttonSpacing;
        }

        if (hoveredButton != null) {
            renderTooltip(matrix, hoveredButton);
        }
    }
    
    private void renderThemeButton(DrawContext context, MatrixStack matrix, ThemeButton button, int mouseX, int mouseY) {
        String icon = "l";
        float iconX = button.x + (button.size - Fonts.getSize(16, Fonts.Type.ESSENCE).getStringWidth(icon)) / 2f;
        float iconY = button.y + (button.size - Fonts.getSize(16, Fonts.Type.ESSENCE).getStringHeight(icon)) / 2f;
        
        int[] themeColors = getThemeColors(button.themeName);
        Fonts.getSize(16, Fonts.Type.ESSENCE).drawHalfSplitString(matrix, icon, iconX - 6, iconY, themeColors[0], themeColors[1]);
    }
    
    private void renderTooltip(MatrixStack matrix, ThemeButton button) {
        var font = Fonts.getSize(11, Fonts.Type.SuisseIntlSemiBold);
        
        int paddingX = 3;
        float paddingY = 0.75F;
        float width = font.getStringWidth(button.themeName);
        float height = font.getFont().getSize() / 1.5F;

        float tooltipWidth = width + paddingX * 2;
        float iconOffset = -6;
        float posX = button.x + (button.size / 2f) - (tooltipWidth / 2f) + iconOffset;
        float posY = button.y - height - paddingY * 2 - 4;

        rectangle.render(ShapeProperties.create(matrix, 
                posX, 
                posY, 
                width + paddingX * 2, 
                height + paddingY * 2)
                .round(1f)
                .outlineColor(new Color(33, 33, 33, 150).getRGB())
                .color(ColorAssist.HALF_BLACK)
                .build());

        font.drawString(matrix, button.themeName, posX + paddingX, posY + 3.5f, -1);
    }
    
    
    private int[] getThemeColors(String themeName) {
        try {
            File themeDir = ThemeManager.getThemeDir();
            File themeFile = new File(themeDir, themeName + ".json");
            
            if (!themeFile.exists()) {
                System.out.println("[ThemeDisplayComponent] Theme file not found: " + themeFile.getAbsolutePath());
                return new int[]{0xFFFFFFFF, 0xFFFFFFFF};
            }
            
            String content = Files.readString(themeFile.toPath());
            JsonObject theme = JsonParser.parseString(content).getAsJsonObject();
            
            if (theme.has("colors")) {
                JsonObject colors = theme.getAsJsonObject("colors");
                int primaryColor = colors.has("Первый цвет") ? colors.get("Первый цвет").getAsInt() : 0xFFFFFFFF;
                int secondaryColor = colors.has("Второй цвет") ? colors.get("Второй цвет").getAsInt() : 0xFFFFFFFF;

                
                return new int[]{primaryColor, secondaryColor};
            }
            return new int[]{0xFFFFFFFF, 0xFFFFFFFF};
        } catch (Exception e) {
            System.out.println("Error loading theme '" + themeName + "': " + e.getMessage());
            e.printStackTrace();
            return new int[]{0xFFFFFFFF, 0xFFFFFFFF};
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (ThemeButton themeButton : themeButtons) {
            if (Calculate.isHovered(mouseX+6, mouseY+4, themeButton.x, themeButton.y, themeButton.size, themeButton.size - 5)) {
                if (button == 0) {
                    loadTheme(themeButton.themeName);
                    return true;
                } else if (button == 1 && !themeButton.isDefault) {
                    deleteTheme(themeButton.themeName);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void loadTheme(String themeName) {
        try {
            ThemeManager.loadTheme(themeName);
        } catch (Exception e) {
            System.out.println("Error loading theme: " + e.getMessage());
        }
    }
    
    private void deleteTheme(String themeName) {
        try {
            ThemeManager.deleteTheme(themeName);
        } catch (Exception e) {
            System.out.println("Error deleting theme: " + e.getMessage());
        }
    }
    
    @Override
    public boolean isHover(double mouseX, double mouseY) {
        for (ThemeButton button : themeButtons) {
            if (Calculate.isHovered(mouseX, mouseY, button.x, button.y, button.size, button.size)) {
                return true;
            }
        }
        return false;
    }
    
    private static class ThemeButton {
        public String themeName;
        public boolean isDefault;
        public float x, y, size;
        
        public ThemeButton(String themeName, boolean isDefault) {
            this.themeName = themeName;
            this.isDefault = isDefault;
        }
    }
}
