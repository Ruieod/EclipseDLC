package code.essence.display.screens.clickgui.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.animation.AnimationHelper;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.theme.ThemeManager;
import code.essence.Essence;
import code.essence.utils.display.scissor.ScissorAssist;
import code.essence.display.screens.clickgui.MenuScreen;
import code.essence.display.screens.clickgui.components.implement.panel.PanelComponent;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static code.essence.utils.display.render.font.Fonts.Type.*;

public class SliderComponent extends AbstractSettingComponent {

    private final SliderSettings setting;
    private boolean wasModuleEnabled = false;

    private boolean dragging;
    private float scrollPosition = 0f;

    private float targetValue;

    public SliderComponent(SliderSettings setting) {
        super(setting);
        this.setting = setting;
        this.targetValue = setting.getValue();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        
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
        
        float brightness = AnimationHelper.getAnimationValue(alphaAnimationKey, moduleEnabled ? 1f : 0.537f);
        int titleColor;
        if (moduleEnabled) {
            titleColor = ThemeManager.textColor.getColor();
        } else {
            titleColor = new Color(137, 137, 140, 255).getRGB();
        }
        
        String value;
        if (setting.isInteger()) {
            value = String.valueOf((int) setting.getValue());
        } else {
            int decimalPlaces = 0;
            float step = setting.getStep();
            if (step > 0 && step < 1) {
                String stepStr = String.valueOf(step);
                if (stepStr.contains(".")) {
                    decimalPlaces = stepStr.length() - stepStr.indexOf('.') - 1;
                    while (decimalPlaces > 0 && stepStr.charAt(stepStr.length() - 1) == '0') {
                        stepStr = stepStr.substring(0, stepStr.length() - 1);
                        decimalPlaces--;
                    }
                }
            }
            if (decimalPlaces == 0) {
                value = String.valueOf((int) setting.getValue());
            } else {
                String format = "%." + decimalPlaces + "f";
                value = String.format(java.util.Locale.US, format, setting.getValue());
                value = value.replaceAll("0+$", "").replaceAll("\\.$", "");
            }
        }
        float valueWidth = Fonts.getSize(13, SuisseIntlSemiBold).getStringWidth(value);
        float valueX = x + width - valueWidth - 5;
        
        float nameStartX = x + 5;
        float nameMaxWidth = valueX - nameStartX - 4;
        
        var nameFont = Fonts.getSize(12, SuisseIntlSemiBold);
        String name = setting.getName();
        float nameWidth = nameFont.getStringWidth(name);
        
        boolean isInPanelBounds = false;
        float panelLeft = x;
        float panelRight = x + width;
        
        try {
            MenuScreen menuScreen = MenuScreen.INSTANCE;
            if (menuScreen != null && menuScreen.getPanelManager() != null) {
                for (PanelComponent panel : menuScreen.getPanelManager().getPanels()) {

                    float categoryTop = panel.y + 35 - 8;
                    float categoryBottom = panel.y + 35 + 22;
                    float moduleStartY = categoryBottom + 4;
                    float panelTop = moduleStartY - 10;
                    float panelBottom = panel.y + panel.height + 15;

                    float panelModuleLeft = panel.x + 1.5f;
                    float panelModuleRight = panel.x + panel.width - 1.5f;

                    if (y >= panelTop && y < panelBottom && 
                        (y + height) > panelTop && (y + height) <= panelBottom &&
                        x >= panelModuleLeft && x < panelModuleRight &&
                        (x + width) > panelModuleLeft && (x + width) <= panelModuleRight) {
                        isInPanelBounds = true;
                        panelLeft = panelModuleLeft;
                        panelRight = panelModuleRight;
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
          
        }
        
        if (!isInPanelBounds) {
            scrollPosition = 0f;
        } else {
            boolean isHovered = Calculate.isHovered(mouseX, mouseY, nameStartX, y + 2, nameMaxWidth, 12);
            
            if (nameWidth > nameMaxWidth && nameMaxWidth > 0) {
                ScissorAssist scissorManager = Essence.getInstance().getScissorManager();
                
                String separation = "  ";
                String scrollingText = name + separation + name;
                float scrollingTextWidth = nameFont.getStringWidth(scrollingText);
                float separationWidth = nameFont.getStringWidth(separation);
                float scrollCycle = nameWidth + separationWidth;
                
                if (isHovered) {
                    float scrollSpeed = 2.0f;
                    scrollPosition += scrollSpeed * delta;
                    scrollPosition = scrollPosition % scrollCycle;
                } else {
                    scrollPosition = 0f;
                }
                
                float textStartX = nameStartX - scrollPosition;
                float clipX = Math.max(panelLeft, nameStartX);
                float clipY = Math.max(y, y + 2);
                float maxAvailableWidth = Math.min(panelRight - clipX, x + width - clipX);
                float clipWidth = Math.min(nameMaxWidth, maxAvailableWidth);
                float clipHeight = Math.min(12, (y + height) - clipY);
                
                if (clipWidth > 0 && clipHeight > 0 && clipY < y + height && clipX >= panelLeft && clipX < panelRight) {
                    scissorManager.push(matrix.peek().getPositionMatrix(), clipX, clipY, clipWidth, clipHeight);
                    
                    nameFont.drawString(matrix, scrollingText, textStartX, y + 5, titleColor);
                    
                    scissorManager.pop();
                }
            } else {
                scrollPosition = 0f;
                if (nameWidth <= nameMaxWidth) {
                    nameFont.drawString(matrix, name, nameStartX, y + 5, titleColor);
                }
            }
        }
        
        if (moduleEnabled) {
            Fonts.getSize(12, SuisseIntlSemiBold).drawRainbowString(matrix, value, valueX, y + 5, ColorAssist.getClientColor(), ColorAssist.getClientColor2());
        } else {
            int valueColor = new Color(87, 87, 90, 255).getRGB();
            Fonts.getSize(12, SuisseIntlSemiBold).drawString(matrix, value, valueX, y + 5, valueColor);
        }
        
        float sliderY = y + 12;
        float sliderWidth = width - 10;
        float sliderHeight = 3;
        
        rectangle.render(ShapeProperties.create(matrix, x + 5, sliderY, sliderWidth, sliderHeight)
                .round(2)
                .color(new Color(40, 40, 45, 255).getRGB())
                .build());
        
        float progress = (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin());
        float progressWidth = MathHelper.clamp(sliderWidth * progress, 0, sliderWidth);
        
        if (moduleEnabled) {
            rectangle.render(ShapeProperties.create(matrix, x + 5, sliderY, progressWidth, sliderHeight)
                    .round(2)
                    .color(ColorAssist.getClientColor(), ColorAssist.getClientColor(),
                           ColorAssist.getClientColor2(), ColorAssist.getClientColor2())
                    .build());
        } else {
            rectangle.render(ShapeProperties.create(matrix, x + 5, sliderY, progressWidth, sliderHeight)
                    .round(2)
                    .color(new Color(57, 57, 60, 255).getRGB())
                    .build());
        }

        float sliderX = MathHelper.clamp(x + 5 + progressWidth - 3, x + 5, x + 5 + sliderWidth - 3);
        int sliderHandleColor = moduleEnabled ? ThemeManager.textColor.getColor() : new Color(137, 137, 140, 255).getRGB();
        rectangle.render(ShapeProperties.create(matrix, sliderX, sliderY - 1.2, 5, 5)
                .round(3)
                .color(sliderHandleColor)
                .build());
        boolean isLeftMousePressed = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (!isLeftMousePressed) {
            dragging = false;
        }
        
        if (dragging && isLeftMousePressed) {
            updateTargetValue(mouseX);
        }
        

        if (targetValue != setting.getValue()) {
            float lerpSpeed = 0.15f;
            float newValue = MathHelper.lerp(lerpSpeed, setting.getValue(), targetValue);
            if (Math.abs(newValue - targetValue) < 0.001f) {
                newValue = targetValue;
            }
            setting.setValue(newValue);
        }
        
        height = 20;
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float sliderY = y + 11;
        float sliderWidth = width ;
        float sliderHeight = 4;
        dragging = Calculate.isHovered(mouseX, mouseY, x + 5, sliderY, sliderWidth, sliderHeight) && button == 0;
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }


    private void updateTargetValue(int mouseX) {
        float sliderWidth = width - 10;
        float clampedMouseX = MathHelper.clamp(mouseX, x + 5, x + 5 + sliderWidth);
        float normalizedPos = (clampedMouseX - (x + 5)) / sliderWidth;
        float newValue = setting.getMin() + normalizedPos * (setting.getMax() - setting.getMin());
        

        if (setting.isInteger()) {
            newValue = Math.round(newValue);
        } else if (setting.getStep() > 0) {
            BigDecimal bdValue = BigDecimal.valueOf(newValue);
            BigDecimal bdStep = BigDecimal.valueOf(setting.getStep());
            BigDecimal stepsCount = bdValue.divide(bdStep, 0, RoundingMode.HALF_UP);
            newValue = stepsCount.multiply(bdStep).floatValue();
        }
        
        targetValue = MathHelper.clamp(newValue, setting.getMin(), setting.getMax());
    }
}