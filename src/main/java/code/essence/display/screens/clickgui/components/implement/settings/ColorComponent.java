package code.essence.display.screens.clickgui.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import code.essence.features.module.setting.implement.ColorSetting;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.display.screens.clickgui.components.implement.window.AbstractWindow;
import code.essence.utils.animation.AnimationHelper;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.theme.ThemeManager;
import code.essence.display.screens.clickgui.components.implement.window.implement.settings.color.ColorWindow;

import java.awt.*;

import static code.essence.utils.display.render.font.Fonts.Type.*;

public class ColorComponent extends AbstractSettingComponent {
    private final ColorSetting setting;
    private boolean wasModuleEnabled = false;

    public ColorComponent(ColorSetting setting) {
        super(setting);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        height = 12;

        
 

        float ColorY = y -2;
        
        boolean moduleEnabled = module != null && module.isState();
        
        String alphaAnimationKey = "setting_text_alpha_" + setting.getName();
        
        if (moduleEnabled != wasModuleEnabled) {
            if (moduleEnabled) {
                AnimationHelper.startAnimation(alphaAnimationKey, 137f, 255f, 150, AnimationHelper.EasingType.EASE_OUT);
            } else {
                AnimationHelper.startAnimation(alphaAnimationKey, 255f, 137f, 150, AnimationHelper.EasingType.EASE_IN);
            }
            wasModuleEnabled = moduleEnabled;
        }
        
        float brightness = AnimationHelper.getAnimationValue(alphaAnimationKey, moduleEnabled ? 255f : 137f);
        int textColor = new Color((int)brightness, (int)brightness, (int)brightness, 255).getRGB();
        
        Fonts.getSize(13, SuisseIntlSemiBold).drawString(context.getMatrices(), setting.getName(), x + 5, ColorY + 5f, ThemeManager.textColor.getColor());


        float circleX = x + width - 12;
        float circleY = y +1f;
        float circleSize = 6;
        
        rectangle.render(ShapeProperties.create(matrix, circleX, circleY, circleSize, circleSize)
                .round(3).color(setting.getColor()).build());

        int outlineColor;
        if (moduleEnabled) {
            outlineColor = ThemeManager.textColor.getColor();
        } else {
            Color settingColor = new Color(setting.getColor(), true);
            outlineColor = new Color(settingColor.getRed(), settingColor.getGreen(), settingColor.getBlue(), 64).getRGB();
        }
        
        rectangle.render(ShapeProperties.create(matrix, circleX, circleY, circleSize, circleSize)
                .round(3).thickness(2.5f).softness(1).outlineColor(outlineColor).color(0x0FFFFFF).build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (Calculate.isHovered(mouseX, mouseY, x + width - 11, y , 6, 6) && button == 0) {
            AbstractWindow existingWindow = null;

            for (AbstractWindow window : windowManager.getWindows()) {
                if (window instanceof ColorWindow) {
                    existingWindow = window;
                    break;
                }
            }

            if (existingWindow != null) {
                windowManager.delete(existingWindow);
            } else {
                AbstractWindow colorWindow = new ColorWindow(setting)
                        .position((int) (mouseX - 110), (int) (mouseY- 20))
                        .size(100, 155)
                        .draggable(true);

                windowManager.add(colorWindow);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
