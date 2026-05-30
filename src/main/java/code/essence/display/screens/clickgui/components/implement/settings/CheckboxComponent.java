package code.essence.display.screens.clickgui.components.implement.settings;

import code.essence.Essence;
import code.essence.display.screens.clickgui.MenuScreen;
import code.essence.display.screens.clickgui.components.implement.panel.PanelComponent;
import code.essence.utils.animation.AnimationHelper;

import code.essence.features.module.setting.implement.BooleanSetting;
import code.essence.utils.display.render.font.Fonts;
import code.essence.display.screens.clickgui.components.implement.other.CheckComponent;
import code.essence.utils.display.scissor.ScissorAssist;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

import static code.essence.utils.display.render.font.Fonts.Type.*;

public class CheckboxComponent extends AbstractSettingComponent {
    private final CheckComponent checkComponent = new CheckComponent();
    private final BooleanSetting setting;
    private boolean wasModuleEnabled = false;
    private float scrollPosition = 0f;
    public CheckboxComponent(BooleanSetting setting) {
        super(setting);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
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
        
        float brightness = AnimationHelper.getAnimationValue(alphaAnimationKey, moduleEnabled ? 1f : 0.537f);
        int titleColor;
        if (moduleEnabled) {
            titleColor = ThemeManager.textColor.getColor();
        } else {
            titleColor = new Color(137, 137, 140, 255).getRGB();
        }

        float checkboxX = x + width - 12.5f;
        float nameStartX = x + 4;
        float nameMaxWidth = checkboxX - nameStartX - 4;

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
            nameFont.drawString(matrix, name, nameStartX, y + 5, titleColor);
        } else {
            boolean isHovered = Calculate.isHovered(mouseX, mouseY, nameStartX, y + 2, nameMaxWidth, 12);

            if (nameWidth > nameMaxWidth && nameMaxWidth > 0) {
                ScissorAssist scissorManager = Essence.getInstance().getScissorManager();

                String separation = "  ";
                String scrollingText = name + separation + name;
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

        ((CheckComponent) checkComponent.position(checkboxX, y + 3f))
                .setRunnable(() -> setting.setValue(!setting.isValue()))
                .setState(setting.isValue())
                .setModuleEnabled(moduleEnabled)
                .render(context, mouseX, mouseY, delta);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && Calculate.isHovered(mouseX, mouseY, x + 4, y + 1, width - 8, height - 4)) {
            String toggleAnimationKey = "checkbox_toggle_" + System.identityHashCode(checkComponent);
            AnimationHelper.startAnimation(toggleAnimationKey, 1f, 1.2f, 100, AnimationHelper.EasingType.EASE_OUT);
            setting.setValue(!setting.isValue());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
