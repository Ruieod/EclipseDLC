package code.essence.display.screens.clickgui.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.glfw.GLFW;

import code.essence.features.module.setting.implement.BindSetting;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.animation.AnimationHelper;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.client.chat.StringHelper;

import java.awt.*;

import static code.essence.utils.display.render.font.Fonts.Type.*;

public class IconBindComponent extends AbstractSettingComponent {
    private final BindSetting setting;
    private boolean binding;
    private boolean wasModuleEnabled = false;

    public IconBindComponent(BindSetting setting) {
        super(setting);
        this.setting = setting;
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        java.lang.String bindName = StringHelper.getBindName(setting.getKey());
        java.lang.String name = binding ? "(" + bindName + ") ..." : bindName;
        float stringWidth = Fonts.getSize(11, SEMI).getStringWidth(name) - 2;

        height = 20;

        rectangle.render(ShapeProperties.create(matrix, x + width - stringWidth - 17, y + 5.5f, stringWidth + 10, 12)
                .round(3f)
                .outlineColor(new Color(200, 200, 200, 255).getRGB())
                .color(
                        new Color(61, 67, 71, 80).getRGB(),
                        new Color(71, 77, 81, 80).getRGB(),
                        new Color(81, 87, 91, 80).getRGB(),
                        new Color(91, 97, 101, 80).getRGB())
                .build());
        int bindingColor = ColorHelper.getArgb(255, 135, 136, 148);

        Fonts.getSize(11, SEMI).drawString(matrix, name, x + width - 12 - stringWidth - 1, y + 11, bindingColor);

        Fonts.getSize(14, GUIICONS).drawString(context.getMatrices(), "L", x + 6, y + 11f, new Color(128, 128, 128, 64).getRGB());

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
        int titleColor = new Color((int)brightness, (int)brightness, (int)brightness, 255).getRGB();

        Fonts.getSize(12, DEFAULT).drawString(context.getMatrices(), setting.getName(), x + 5, y + 10f, titleColor);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (Calculate.isHovered(mouseX, mouseY, x, y, width, height)) {
                binding = !binding;
            } else {
                binding = false;
            }
        }

        if (binding && button > 1) {
            setting.setKey(button);
            binding = false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int key = keyCode == GLFW.GLFW_KEY_DELETE ? -1 : keyCode;
        if (binding) {
            setting.setKey(key);
            binding = false;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
