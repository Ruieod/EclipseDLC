package code.essence.display.screens.clickgui.components.implement.settings;

import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.animation.AnimationHelper;
import code.essence.utils.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

import code.essence.features.module.setting.implement.BindSetting;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.client.chat.StringHelper;

import java.awt.*;

import static code.essence.utils.display.render.font.Fonts.Type.*;

public class BindComponent extends AbstractSettingComponent {
    private final BindSetting setting;
    private boolean binding;

    public BindComponent(BindSetting setting) {
        super(setting);
        this.setting = setting;
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        boolean moduleEnabled = module != null && module.isState();
        boolean hasBind = setting.getKey() != -1;
        
        java.lang.String bindName = StringHelper.getBindName(setting.getKey());
        java.lang.String name = binding ? "?" : bindName;
        float stringWidth = Fonts.getSize(11, SuisseIntlSemiBold).getStringWidth(name) - 2;

        height = 15;



        float bindX = x + width - 11.f;
        float bindY = y + 8.5f;

        float buttonWidth = Math.max(6, stringWidth + 6);
        float buttonHeight = 6;

        String bindAnimationKey = "bind_" + System.identityHashCode(this);
        float bindScale = AnimationHelper.getAnimationValue(bindAnimationKey, 1f);

        int fillColor;
        if (moduleEnabled) {
            if (hasBind) {
                fillColor = ColorAssist.getClientColor();
            } else {
                fillColor = new Color(57, 57, 60, 255).getRGB();
            }
        } else {
            fillColor = new Color(47, 47, 50, 255).getRGB();
        }

        if (hasBind) {
            if (moduleEnabled) {
                rectangle.render(ShapeProperties.create(matrix, bindX - buttonWidth + 6, bindY- 6.5 , buttonWidth * bindScale, (buttonHeight +  1.5f) * bindScale)
                        .round(1.5f)
                        .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(), ColorAssist.getClientColor(), ColorAssist.getClientColor2())
                        .build());
            } else {
                rectangle.render(ShapeProperties.create(matrix, bindX - buttonWidth + 6, bindY- 6.5, buttonWidth * bindScale , (buttonHeight+  1.5f) * bindScale)
                        .round(1.5f)
                        .color(new Color(57, 57, 60, 255).getRGB())
                        .build());
            }
        } else {
            if (moduleEnabled) {
                rectangle.render(ShapeProperties.create(matrix, bindX - buttonWidth + 6, bindY - 6.5, buttonWidth, buttonHeight + 1.5)
                        .round(1.5f)
                        .color(new Color(57, 57, 60, 255).getRGB())
                        .build());
            } else {
                rectangle.render(ShapeProperties.create(matrix, bindX - buttonWidth + 6, bindY - 6.5, buttonWidth, buttonHeight + 1.5)
                        .round(1.5f)
                        .color(new Color(47, 47, 50, 255).getRGB())
                        .build());
            }
        }

        int bindingColor;
        if (moduleEnabled && hasBind) {
            bindingColor = ThemeManager.textColor.getColor();
        } else if (moduleEnabled) {
            bindingColor = new Color(137, 137, 140, 255).getRGB();
        } else {
            bindingColor = new Color(87, 87, 90, 255).getRGB();
        }
        Fonts.getSize(11, SuisseIntlSemiBold).drawString(matrix, name, bindX - buttonWidth + 8.1, bindY - 3.5, bindingColor);

        int titleColor = moduleEnabled ? ThemeManager.textColor.getColor() : new Color(137, 137, 140, 255).getRGB();
        Fonts.getSize(13, SuisseIntlSemiBold).drawString(context.getMatrices(), setting.getName(), x + 5, bindY - 4, titleColor);
   }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {

            if (Calculate.isHovered(mouseX, mouseY, x + 4, y+0.5f, width - 8, height - 4)) {
                binding = !binding;
            } else {
                binding = false;
            }
        }

        if (binding && button > 1) {
            String bindAnimationKey = "bind_" + System.identityHashCode(this);
            AnimationHelper.startAnimation(bindAnimationKey, 1f, 1.05f, 80, AnimationHelper.EasingType.EASE_OUT);
            
            setting.setKey(button);
            binding = false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int key = keyCode == GLFW.GLFW_KEY_DELETE ? -1 : keyCode;
        if (binding) {

            String bindAnimationKey = "bind_" + System.identityHashCode(this);
            AnimationHelper.startAnimation(bindAnimationKey, 1f, 1.2f, 100, AnimationHelper.EasingType.EASE_OUT);
            
            setting.setKey(key);
            binding = false;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
