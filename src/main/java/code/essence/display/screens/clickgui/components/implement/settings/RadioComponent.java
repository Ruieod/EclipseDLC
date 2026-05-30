package code.essence.display.screens.clickgui.components.implement.settings;

import code.essence.features.module.setting.implement.RadioSetting;
import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.utils.animation.AnimationHelper;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.theme.ThemeManager;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.display.render.shape.implement.Rectangle;
import code.essence.utils.math.calc.Calculate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class RadioComponent extends AbstractSettingComponent {
    private final RadioSetting radioSetting;
    private final SelectSetting selectSetting;
    private final Rectangle rectangle = new Rectangle();
    private final Map<String, Float> textScrolls = new HashMap<>();
    private float titleScroll = 0f;
    private boolean isHoveringTitle = false;
    private boolean wasModuleEnabled = false;

    public RadioComponent(RadioSetting setting) {
        super(setting);
        this.radioSetting = setting;
        this.selectSetting = null;
        for (String text : setting.getOptions()) {
            textScrolls.put(text, 0f);
        }
    }
    
    public RadioComponent(SelectSetting setting) {
        super(setting);
        this.radioSetting = null;
        this.selectSetting = setting;
        for (String text : setting.getList()) {
            textScrolls.put(text, 0f);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        
        String settingName = getSetting().getName();
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
        
        Fonts.getSize(13, Fonts.Type.SuisseIntlSemiBold).drawString(matrix, settingName, x + 4, y + 3, titleColor);
        
        String selectedValue = getSelectedValue();
        float selectedValueWidth = Fonts.getSize(12, Fonts.Type.SuisseIntlSemiBold).getStringWidth(selectedValue);
        
        if (moduleEnabled) {
            Fonts.getSize(12, Fonts.Type.SuisseIntlSemiBold).drawRainbowString(matrix, selectedValue, x + width - selectedValueWidth - 7, y + 3.5,
                ColorAssist.getClientColor(), ColorAssist.getClientColor2());
        } else {
            int selectedColor = new Color(87, 87, 90, 255).getRGB();
            Fonts.getSize(12, Fonts.Type.SuisseIntlSemiBold).drawString(matrix, selectedValue, x + width - selectedValueWidth - 7, y + 3.5, selectedColor);
        }

        float offset = 1;
        String[] options = getOptions();
        int maxOptions = Math.min(options.length, 8);
        for (int i = 0; i < maxOptions; i++) {
            String text = options[i];
            float off = 8;
            boolean isSelected = text.equals(getSelectedValue());

            boolean hovered = Calculate.isHovered(mouseX, mouseY, x + 5, y + 11.2f + offset, width - 25, 12);
            float textWidth = Fonts.getSize(10, Fonts.Type.SuisseIntlSemiBold).getStringWidth(text);
            boolean textInRegion = !(textWidth > width - 30);

            float currentScroll = textScrolls.getOrDefault(text, 0f);
            if (hovered) {
                if (!textInRegion) {
                    textScrolls.put(text, Calculate.interpolateSmooth(6, currentScroll, (width - 30) - textWidth - 4));
                }
            } else {
                textScrolls.put(text, Calculate.interpolateSmooth(3, currentScroll, 0));
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
                    textColor = new Color(157, 157, 160, 255).getRGB();
                } else {
                    textColor = new Color(87, 87, 90, 255).getRGB();
                }
            }
            
            Fonts.getSize(12, Fonts.Type.SuisseIntlMedium).drawString(matrix, text, x + 5 + textScrolls.get(text), y + 11.2f + offset, textColor);

            String radioAnimationKey = "radio_toggle_" + System.identityHashCode(this) + "_" + text;
            float radioScale = AnimationHelper.getAnimationValue(radioAnimationKey, 1f);
            
            if (isSelected) {
                if (moduleEnabled) {
                    rectangle.render(ShapeProperties.create(matrix, x + width - 12, y + 9f + offset, 6 * radioScale, 6 * radioScale)
                            .round(3)
                            .color(ColorAssist.getClientColor(),ColorAssist.getClientColor2(),ColorAssist.getClientColor(),ColorAssist.getClientColor2())
                            .build());
                } else {
                    rectangle.render(ShapeProperties.create(matrix, x + width - 12, y + 9f + offset, 6 * radioScale, 6 * radioScale)
                            .round(3)
                            .color(new Color(57, 57, 60, 255).getRGB())
                            .build());
                }
            } else {
                rectangle.render(ShapeProperties.create(matrix, x + width - 12, y + 9.5f + offset, 6 * radioScale, 6 * radioScale)
                        .round(3)
                        .softness(1)
                        .thickness(2)
                        .outlineColor(new Color(57, 57, 60, 255).getRGB())
                        .color(new Color(32, 32, 35, 40).getRGB())
                        .build());
            }
            
            if (isSelected) {
                int dotColor = moduleEnabled ? ThemeManager.textColor.getColor() : new Color(137, 137, 140, 255).getRGB();
                rectangle.render(ShapeProperties.create(matrix, x + width - 10.05f, y + 10.7f + offset, 2.35f, 2.35f)
                        .round(1.425f)
                        .color(dotColor)
                        .build());
            }

            offset += off;
        }
        
        height = (int) (offset + 12);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            float offset = 1;
            String[] options = getOptions();
            int maxOptions = Math.min(options.length, 8);
            for (int i = 0; i < maxOptions; i++) {
                String text = options[i];
                float off = 8;
                boolean isSelected = text.equals(getSelectedValue());

                if (Calculate.isHovered(mouseX, mouseY, x + 5, y + 7.5f + offset, width - 10, 10)) {
                    String radioAnimationKey = "radio_toggle_" + System.identityHashCode(this) + "_" + text;
                    AnimationHelper.startAnimation(radioAnimationKey, 1f, 1.3f, 120, AnimationHelper.EasingType.EASE_OUT);
                    
                    setSelectedValue(text);
                    return true;
                }
                
                offset += off;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean isVisible() {
        return getSetting().isVisible();
    }
    
    private String getSelectedValue() {
        if (radioSetting != null) {
            return radioSetting.get();
        } else {
            return selectSetting.getSelected();
        }
    }
    
    private String[] getOptions() {
        if (radioSetting != null) {
            return radioSetting.getOptions();
        } else {
            return selectSetting.getList().toArray(new String[0]);
        }
    }
    
    private void setSelectedValue(String value) {
        if (radioSetting != null) {
            radioSetting.set(value);
        } else {
            selectSetting.setSelected(value);
        }
    }
}
