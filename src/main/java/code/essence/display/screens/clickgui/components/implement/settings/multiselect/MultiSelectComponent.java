package code.essence.display.screens.clickgui.components.implement.settings.multiselect;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import code.essence.display.screens.clickgui.components.implement.settings.AbstractSettingComponent;
import code.essence.features.module.setting.implement.MultiSelectSetting;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.animation.AnimationHelper;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.theme.ThemeManager;
import code.essence.utils.math.calc.Calculate;
import java.awt.*;

import static code.essence.utils.display.render.font.Fonts.Type.*;

public class MultiSelectComponent extends AbstractSettingComponent {
    private final MultiSelectSetting setting;
    private boolean wasModuleEnabled = false;
    

    private float cachedRenderX, cachedRenderY, cachedRenderWidth;

    public MultiSelectComponent(MultiSelectSetting setting) {
        super(setting);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();

        cachedRenderX = x;
        cachedRenderY = y;
        cachedRenderWidth = width;



        String settingName = setting.getName();
        boolean moduleEnabled = module != null && module.isState();

        String alphaAnimationKey = "setting_text_alpha_" + settingName;

        if (moduleEnabled != wasModuleEnabled) {
            if (moduleEnabled) {
                AnimationHelper.startAnimation(alphaAnimationKey, 0.537f, 1f, 150, AnimationHelper.EasingType.EASE_OUT);
            } else {
                AnimationHelper.startAnimation(alphaAnimationKey, 1f, 0.537f, 150, AnimationHelper.EasingType.EASE_IN);
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
        
        Fonts.getSize(13, SuisseIntlSemiBold).drawString(matrices, settingName, x + 4, y + 2, titleColor);
        
        String selectedCount = setting.getSelected().size() + "/" + setting.getList().size();
        float selectedCountWidth = Fonts.getSize(12, SuisseIntlSemiBold).getStringWidth(selectedCount);
        
        if (moduleEnabled) {
            Fonts.getSize(12, SuisseIntlSemiBold).drawRainbowString(matrices, selectedCount, x + width - selectedCountWidth - 7, y + 2,
                ColorAssist.getClientColor(), ColorAssist.getClientColor2());
        } else {
            int selectedColor = new Color(87, 87, 90, 255).getRGB();
            int totalColor = new Color(57, 57, 60, 255).getRGB();
            
            String selectedPart = String.valueOf(setting.getSelected().size());
            String totalPart = "/" + setting.getList().size();
            float selectedPartWidth = Fonts.getSize(12, SuisseIntlSemiBold).getStringWidth(selectedPart);
            
            Fonts.getSize(12, SuisseIntlSemiBold).drawString(matrices, selectedPart, x + width - selectedCountWidth - 7, y + 2, selectedColor);
            Fonts.getSize(12, SuisseIntlSemiBold).drawString(matrices, totalPart, x + width - selectedCountWidth - 7 + selectedPartWidth, y + 2, totalColor);
        }


        float leftMargin = 4;
        float xOffset = x + leftMargin;
        float yOffset = y + 10;
        float lineHeight = 0;
        final int HORIZONTAL_PADDING = 2;
        final int VERTICAL_PADDING = 2;
        final int HORIZONTAL_SPACING = 0;
        
        for (String option : setting.getList()) {
            float buttonWidth = Math.min(85, Fonts.getSize(12, SuisseIntlMedium).getStringWidth(option) + 6);
            float buttonHeight = 12;
            
            if (xOffset + (buttonWidth - 2) > x + width) {
                xOffset = x + leftMargin;
                yOffset += lineHeight + VERTICAL_PADDING;
            }
            
            boolean isSelected = setting.getSelected().contains(option);

            String multiSelectAnimationKey = "multiselect_" + System.identityHashCode(this) + "_" + option;
            float buttonScale = AnimationHelper.getAnimationValue(multiSelectAnimationKey, 1f);
            
            if (isSelected) {
                if (moduleEnabled) {
                    rectangle.render(ShapeProperties.create(matrices, xOffset, yOffset, (buttonWidth - 2) * buttonScale , buttonHeight * buttonScale)
                            .round(2.2f)
                            .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(),ColorAssist.getClientColor(),ColorAssist.getClientColor2())
                            .build());
                } else {
                    rectangle.render(ShapeProperties.create(matrices, xOffset, yOffset, (buttonWidth - 2) * buttonScale, buttonHeight * buttonScale)
                            .round(2.2f)
                            .color(new Color(57, 57, 60, 255).getRGB())
                            .build());
                }
            } else {
                if (moduleEnabled) {
                    rectangle.render(ShapeProperties.create(matrices, xOffset, yOffset, buttonWidth - 2, buttonHeight)
                            .round(2.2f)
                            .softness(1)
                            .thickness(2)
                            .outlineColor(new Color(33, 33, 33, 255).getRGB())
                            .color(new Color(32, 32, 35, 1).getRGB())
                            .build());
                } else {
                    rectangle.render(ShapeProperties.create(matrices, xOffset, yOffset, buttonWidth - 2, buttonHeight)
                            .round(2.2f)
                            .softness(1)
                            .thickness(2)
                            .outlineColor(new Color(33, 33, 33, 255).getRGB())
                            .color(new Color(32, 32, 35, 1).getRGB())
                            .build());
                }
            }
            
            boolean hovered = Calculate.isHovered(mouseX, mouseY, xOffset, yOffset, buttonWidth - 2, buttonHeight);
            boolean textInRegion = !(Fonts.getSize(12, SuisseIntlMedium).getStringWidth(option) > (buttonWidth - 4));
            
            float textX = xOffset + 2;
            float textY = yOffset + VERTICAL_PADDING + 2.5f;
            
            if (hovered && !textInRegion) {
                float textScroll = Math.max(0, Fonts.getSize(12, SuisseIntlMedium).getStringWidth(option) - (buttonWidth - 4));
                textX -= textScroll;
            }
            
            int textColor;
            if (isSelected) {
                if (moduleEnabled) {
                    textColor = ThemeManager.textColor.getColor();
                } else {
                    textColor = new Color(137, 137, 140, 255).getRGB();
                }
            } else {
                if (moduleEnabled) {
                    textColor = new Color(87, 87, 90, 255).getRGB();
                } else {
                    textColor = new Color(87, 87, 90, 255).getRGB();
                }
            }
            
            Fonts.getSize(12, SuisseIntlMedium).drawString(matrices, option, textX, textY, textColor);
            
            xOffset += buttonWidth + HORIZONTAL_SPACING;
            lineHeight = buttonHeight;
        }
        
        int newHeight = Math.round(yOffset - y + lineHeight + 2);
        if (height != newHeight) {
        }
        height = newHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            float leftMargin = 4;
            float xOffset = cachedRenderX + leftMargin;
            float yOffset = cachedRenderY + 10;
            float lineHeight = 0;
            final int VERTICAL_PADDING = 2;
            final int HORIZONTAL_SPACING = 0;
            
            for (String option : setting.getList()) {
                float buttonWidth = Math.min(85, Fonts.getSize(12, SuisseIntlMedium).getStringWidth(option) + 6);
                float buttonHeight = 12;
                
                if (xOffset + (buttonWidth - 2) > cachedRenderX + cachedRenderWidth) {
                    xOffset = cachedRenderX + leftMargin;
                    yOffset += lineHeight + VERTICAL_PADDING;
                }
                
                boolean isSelected = setting.getSelected().contains(option);
                
                boolean isHovered = Calculate.isHovered(mouseX, mouseY, xOffset, yOffset, buttonWidth - 2, buttonHeight);
                
                if (isHovered) {

                    String multiSelectAnimationKey = "multiselect_" + System.identityHashCode(this) + "_" + option;
                    AnimationHelper.startAnimation(multiSelectAnimationKey, 1f, 1.05f, 80, AnimationHelper.EasingType.EASE_OUT);
                    
                    if (setting.getSelected().contains(option)) {
                        setting.getSelected().remove(option);
                    } else {
                        setting.getSelected().add(option);
                    }
                    return true;
                }
                
                xOffset += buttonWidth + HORIZONTAL_SPACING;
                lineHeight = buttonHeight;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


}