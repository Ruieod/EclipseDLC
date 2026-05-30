package code.essence.display.screens.clickgui.components.implement.other;

import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.display.render.font.Fonts;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.display.screens.clickgui.components.AbstractComponent;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.animation.AnimationHelper;
import code.essence.utils.theme.ThemeManager;

import java.awt.*;

import static code.essence.utils.display.render.font.Fonts.Type.ESSENCE;

@Setter
@Accessors(chain = true)
public class CheckComponent extends AbstractComponent {
    private boolean state;
    private Runnable runnable;
    private boolean moduleEnabled = true;

    @Override
    public CheckComponent position(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        

        String toggleAnimationKey = "checkbox_toggle_" + System.identityHashCode(this);
        float scale = AnimationHelper.getAnimationValue(toggleAnimationKey, 1f);
        
        if (state) {
            if (moduleEnabled) {
                rectangle.render(ShapeProperties.create(matrix, x, y, 7 * scale, 7 * scale)
                        .round(1.5f)
                        .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(), ColorAssist.getClientColor(), ColorAssist.getClientColor2())
                        .build());
            } else {
                rectangle.render(ShapeProperties.create(matrix, x, y, 7 * scale, 7 * scale)
                        .round(1.5f)
                        .color(new Color(57, 57, 60, 255).getRGB())
                        .build());
            }
        } else {
            rectangle.render(ShapeProperties.create(matrix, x, y, 7 * scale, 7 * scale)
                    .round(1.5f)
                    .softness(1)
                    .thickness(2)
                    .outlineColor(new Color(57, 57, 60, 255).getRGB())
                    .color(new Color(32, 32, 35, 40).getRGB())
                    .build());
        }
        
        if (state && moduleEnabled) {
            Fonts.getSize(13, ESSENCE).drawString(matrix, "k", x + .2f, y + 2.9f, ThemeManager.textColor.getColor());
        } else if (state && !moduleEnabled) {
            Fonts.getSize(13, ESSENCE).drawString(matrix, "k", x + .2f, y + 2.9f, new Color(137, 137, 140, 255).getRGB());
        }
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (Calculate.isHovered(mouseX, mouseY, x, y, 6, 6) && button == 0) {
            String toggleAnimationKey = "checkbox_toggle_" + System.identityHashCode(this);
            AnimationHelper.startAnimation(toggleAnimationKey, 1f, 1.2f, 100, AnimationHelper.EasingType.EASE_OUT);
            
            runnable.run();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
