package code.essence.display.screens.clickgui.components.implement.customization;

import code.essence.display.screens.clickgui.components.AbstractComponent;
import code.essence.display.screens.clickgui.components.implement.window.AbstractWindow;
import code.essence.display.screens.clickgui.components.implement.window.implement.settings.color.ColorWindow;
import code.essence.features.module.setting.implement.ColorSetting;
import code.essence.utils.customization.CustomizationManager;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.display.render.shape.implement.Rectangle;
import code.essence.utils.math.calc.Calculate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;
import java.util.List;

import static code.essence.utils.display.render.font.Fonts.Type.SuisseIntlSemiBold;

public class CustomizationComponent extends AbstractComponent {
    private final Rectangle rectangle = new Rectangle();
    private final List<ColorSetting> settings;
    public final ThemeInputComponent themeInput;
    
    public CustomizationComponent() {
        this.settings = CustomizationManager.getCustomizationSettings();
        this.themeInput = new ThemeInputComponent();
        this.height = calculateHeight();
    }
    
    private int calculateHeight() {

        return 1 + (settings.size() * 22);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();


        

        float settingY = y + 1;
        
        for (ColorSetting setting : settings) {
            renderColorSetting(context, matrix, setting, x, settingY, width, 20);
            settingY += 22;
        }

    }
    
    private void renderColorSetting(DrawContext context, MatrixStack matrix, ColorSetting setting,
                                    float x, float y, float width, float height) {

        float colorX = x;
        float colorY = y;
        float colorWidth = width;
        float colorHeight = 20;
        

        rectangle.render(ShapeProperties.create(matrix, colorX - 0.5, colorY -0.5, colorWidth + 1, colorHeight + 1)
                .round(5)
                .color(new Color(47, 47, 50, 255).getRGB())
                .build());


        rectangle.render(ShapeProperties.create(matrix, colorX, colorY, colorWidth, colorHeight)
                .round(5)
                .color(setting.getColor())
                .build());
        

        float textX = x + 5;
        float textY = y + 16.5f - Fonts.getSize(15, SuisseIntlSemiBold).getStringHeight(setting.getName()) / 2f;
        
        int textColor = getContrastTextColor(setting);
        
        Fonts.getSize(15, SuisseIntlSemiBold).drawString(context.getMatrices(),
            setting.getName(), textX, textY + 1, textColor);
        

          String icon = "l";
        float iconX = x + width - 15;
        float iconY = y + 15.5f - Fonts.getSize(13, Fonts.Type.ESSENCE).getStringHeight(icon) / 2f;

        Fonts.getSize(18, Fonts.Type.ESSENCE).drawString(context.getMatrices(), icon, iconX, iconY-1, textColor);
    }
    

    private int getContrastTextColor(ColorSetting setting) {
        Color backgroundColor = new Color(setting.getColor(), true);
        return getContrastColor(backgroundColor);
    }

    private int getContrastColor(Color backgroundColor) {
        double luminance = (0.299 * backgroundColor.getRed() + 0.587 * backgroundColor.getGreen() + 0.114 * backgroundColor.getBlue()) / 255.0;
        
        if (luminance > 0.5) {
            return new Color(0, 0, 0, 255).getRGB();
        } else {
            return new Color(255, 255, 255, 255).getRGB();
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (Calculate.isHovered(mouseX, mouseY, x, y, width, height)) {
            float settingY = y + 1;
            
            for (ColorSetting setting : settings) {
                float colorX = x;
                float colorY = settingY;
                float colorWidth = width;
                float colorHeight = 20;
                
                
                if (Calculate.isHovered(mouseX, mouseY, colorX, colorY, colorWidth, colorHeight) && button == 0) {
                    AbstractWindow existingWindow = null;
                    
                    for (AbstractWindow window : windowManager.getWindows()) {
                        if (window instanceof ColorWindow colorWindow) {
                            if (colorWindow.getSetting() == setting) {
                                existingWindow = window;
                                break;
                            }
                        }
                    }
                    
                    if (existingWindow != null) {
                        windowManager.delete(existingWindow);
                    } else {
                        AbstractWindow colorWindow = new ColorWindow(setting)
                                .position((int) (mouseX - 110), (int) (mouseY - 20))
                                .size(100, 155)
                                .draggable(true);
                        
                        windowManager.add(colorWindow);
                    }
                    return true;
                }
                
                settingY += 22;
            }
            
            System.out.println("CustomizationComponent: Checking ThemeInputComponent click");
            if (themeInput.mouseClicked(mouseX, mouseY, button)) {
                System.out.println("CustomizationComponent: ThemeInputComponent handled click");
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        themeInput.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        System.out.println("CustomizationComponent: charTyped called, chr=" + chr);
        if (themeInput.charTyped(chr, modifiers)) {
            System.out.println("CustomizationComponent: ThemeInputComponent handled charTyped");
            return true;
        }
        return super.charTyped(chr, modifiers);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        System.out.println("CustomizationComponent: keyPressed called, keyCode=" + keyCode);
        if (themeInput.keyPressed(keyCode, scanCode, modifiers)) {
            System.out.println("CustomizationComponent: ThemeInputComponent handled keyPressed");
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return Calculate.isHovered(mouseX, mouseY, x, y, width, height);
    }
}
