package code.essence.display.screens.clickgui.components.implement.settings;

import code.essence.display.screens.clickgui.components.implement.other.SettingComponent;
import net.minecraft.client.gui.DrawContext;

import code.essence.features.module.setting.implement.GroupSetting;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.animation.AnimationHelper;
import code.essence.display.screens.clickgui.components.implement.window.AbstractWindow;
import code.essence.display.screens.clickgui.components.implement.window.implement.settings.group.GroupWindow;
import code.essence.display.screens.clickgui.components.implement.other.CheckComponent;

import java.awt.*;

import static code.essence.utils.display.render.font.Fonts.Type.*;
import static code.essence.utils.display.render.font.Fonts.Type.DEFAULT;

public class GroupComponent extends AbstractSettingComponent {
    private final CheckComponent checkComponent = new CheckComponent();
    private final SettingComponent settingComponent = new SettingComponent();
    private boolean wasModuleEnabled = false;

    private final GroupSetting setting;

    public GroupComponent(GroupSetting setting) {
        super(setting);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        height = 15;

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

        Fonts.getSize(20, GUIICONS).drawString(context.getMatrices(), "K", x + 6, y + 10f, new Color(128, 128, 128, 64).getRGB());

        Fonts.getSize(12, DEFAULT).drawString(context.getMatrices(), setting.getName(), x + 5, y + 11f, titleColor);

        ((CheckComponent) checkComponent.position(x + width - 19, y + 6.5F))
                .setRunnable(() -> setting.setValue(!setting.isValue()))
                .setState(setting.isValue())
                .render(context, mouseX, mouseY, delta);

        ((SettingComponent) settingComponent.position(x + width - 31, y + 6))
                .setRunnable(() -> spawnWindow(mouseX, mouseY))
                .render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        checkComponent.mouseClicked(mouseX, mouseY, button);
        settingComponent.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void spawnWindow(int mouseX, int mouseY) {
        AbstractWindow existingWindow = null;

        for (AbstractWindow window : windowManager.getWindows()) {
            if (window instanceof GroupWindow && ((GroupWindow) window).getSetting() == setting) {
                existingWindow = window;
                break;
            }
        }

        if (existingWindow != null) {
            windowManager.delete(existingWindow);
        } else {
            AbstractWindow groupWindow = new GroupWindow(setting)
                    .position(mouseX + 10, mouseY)
                    .size(137, 23)
                    .draggable(false);

            windowManager.add(groupWindow);
        }
    }
}
