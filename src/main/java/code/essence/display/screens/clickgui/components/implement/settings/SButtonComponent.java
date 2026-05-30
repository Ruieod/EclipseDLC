package code.essence.display.screens.clickgui.components.implement.settings;

import code.essence.display.screens.clickgui.components.implement.other.ButtonComponent;
import net.minecraft.client.gui.DrawContext;

import code.essence.features.module.setting.implement.ButtonSetting;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.animation.AnimationHelper;

import static code.essence.utils.display.render.font.Fonts.Type.BOLD;

public class SButtonComponent extends AbstractSettingComponent {
    private final ButtonComponent buttonComponent = new ButtonComponent();
    private final ButtonSetting setting;
    private boolean wasModuleEnabled = false;

    public SButtonComponent(ButtonSetting setting) {
        super(setting);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        height = 20;

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
        int titleColor = new java.awt.Color((int)brightness, (int)brightness, (int)brightness, 255).getRGB();

        Fonts.getSize(14, BOLD).drawString(context.getMatrices(), setting.getName(), x + 5, y + 6, titleColor);

        ((ButtonComponent) buttonComponent.setText("Click on me")
                .setRunnable(setting.getRunnable())
                .position(x + width - 9 - buttonComponent.width, y + 5))
                .render(context, mouseX, mouseY, delta);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        buttonComponent.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }
}