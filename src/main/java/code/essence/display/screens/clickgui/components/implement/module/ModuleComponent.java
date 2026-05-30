package code.essence.display.screens.clickgui.components.implement.module;

import code.essence.utils.client.chat.StringHelper;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.display.render.font.FontRenderer;
import code.essence.utils.theme.ThemeManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import code.essence.features.module.Module;
import code.essence.features.module.setting.SettingComponentAdder;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.display.render.shape.implement.Rectangle;
import code.essence.display.screens.clickgui.components.AbstractComponent;
import code.essence.display.screens.clickgui.components.implement.settings.AbstractSettingComponent;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.animation.AnimationHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static code.essence.utils.display.render.font.Fonts.Type.*;

@Getter
public class ModuleComponent extends AbstractComponent {
    private final List<AbstractSettingComponent> components = new ArrayList<>();
    private final Module module;
    private final Rectangle rectangle = new Rectangle();
    private boolean binding = false;

    public ModuleComponent(Module module) {
        this.module = module;
        new SettingComponentAdder().addSettingComponent(module.settings(), components, module);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hasSettings = !module.settings().isEmpty();
        boolean isExpanded = isExpanded();
        
        height = 20;
        
        int backgroundColor = module.isState() ? ColorAssist.getClientColor() : ThemeManager.offModuleColor.getColor();
        int secondaryColor = module.isState() ? ColorAssist.getClientColor2() : ThemeManager.offModuleColor.getColor();
        
        rectangle.render(ShapeProperties.create(context.getMatrices(), x, y, width, height)
                .round(5)
                .color(backgroundColor, backgroundColor, secondaryColor, secondaryColor)
                .build());

        String moduleName = binding ? "binding.." : module.getVisibleName();
        float textWidth = Fonts.getSize(14, SuisseIntlSemiBold).getStringWidth(moduleName);
        float textX = x + (width - textWidth) / 2f;
        float textY = y + 16.5f - Fonts.getSize(14, SuisseIntlSemiBold).getStringHeight(moduleName) / 2f;
        
        Fonts.getSize(15, SuisseIntlSemiBold).drawString(context.getMatrices(), moduleName, textX, textY,
            ThemeManager.textColor.getColor());
        if (hasSettings) {
            String arrow = isExpanded ? "j" : "i";
            float arrowX = x + width - 15;
            float arrowY = y + 15.5f - Fonts.getSize(12, ESSENCE).getStringHeight(arrow) / 2f;
            
            Fonts.getSize(13, ESSENCE).drawString(context.getMatrices(), arrow, arrowX, arrowY,
                ThemeManager.textColor.getColor());
        }

        String animationKey = "module_settings_" + module.getName();
        boolean isAnimating = AnimationHelper.isAnimationActive(animationKey);
        
        if ((isExpanded() && hasSettings) || isAnimating) {
             renderSettings(context, mouseX, mouseY, delta);
        }
    }


    public void renderModuleDescriptionAt(DrawContext context, float moduleX, float moduleY, float moduleWidth, boolean isHovered) {
        if (context == null) {
            return;
        }

        try {
            String description = ModuleDescriptions.getDescription(module);
            if (description == null || description.isEmpty() || description.equals("Описание модуля отсутствует")) {
                return;
            }

            MatrixStack matrix = context.getMatrices();
            if (matrix == null) return;

            FontRenderer font = Fonts.getSize(12, SuisseIntlSemiBold);
            if (font == null) return;

            String animationKey = "module_description_alpha_" + module.getName();
            boolean isAnimationActive = AnimationHelper.isAnimationActive(animationKey);
            float currentAlpha = AnimationHelper.getAnimationValue(animationKey, isHovered ? 1f : 0f);

            if (isHovered) {
                if (!isAnimationActive && currentAlpha < 0.99f) {
                    AnimationHelper.fadeIn(animationKey, 400);
                }
            }

            float alpha = AnimationHelper.getAnimationValue(animationKey, isHovered ? 1f : 0f);

            if (alpha <= 0f) {
                return;
            }

            float maxWidth = 200f;
            String wrappedText = StringHelper.wrap(description, (int) maxWidth, 12);
            if (wrappedText == null) return;

            String[] lines = wrappedText.split("\n");
            if (lines == null || lines.length == 0) return;

            float lineHeight = font.getStringHeight("A");
            float textPadding = 6f;

            float descriptionWidth = 0f;
            for (String line : lines) {
                if (line == null) continue;
                float lineWidth = font.getStringWidth(line);
                if (lineWidth > descriptionWidth) {
                    descriptionWidth = lineWidth;
                }
            }
            descriptionWidth = Math.min(descriptionWidth + textPadding, maxWidth);

            float descriptionHeight = lines.length * lineHeight;

            float descriptionX = moduleX + moduleWidth + 5f;
            float descriptionY = moduleY+3.5f;

            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();

            if (descriptionX + descriptionWidth > screenWidth) {
                descriptionX = moduleX - descriptionWidth - 2.5f;
            }

            if (descriptionY + descriptionHeight > screenHeight) {
                descriptionY = screenHeight - descriptionHeight;
            }

            if (descriptionY < 0) {
                descriptionY = 0;
            }

            int alphaValue = (int) (alpha * 255);
            int backgroundColor = new Color(25, 25, 25, alphaValue).getRGB();
            int textColor = new Color(255, 255, 255, alphaValue).getRGB();

            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            com.mojang.blaze3d.systems.RenderSystem.disableScissor();

            matrix.push();
            matrix.translate(0, 0, 3000);

            rectangle.render(ShapeProperties.create(matrix, descriptionX, descriptionY, descriptionWidth, descriptionHeight)
                    .round(2)
                    .color(backgroundColor)
                    .build());

            float textX = descriptionX + textPadding/2;
            float textY = descriptionY + textPadding;


            for (String line : lines) {
                if (line != null && font != null) {
                    font.drawString(matrix, line, textX, textY, textColor);
                    textY += lineHeight;
                }
            }

            matrix.pop();
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        } catch (Exception e) {
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        }
    }

    public void renderModuleDescription(DrawContext context, int mouseX, int mouseY) {
        if (context == null) return;

        try {
            String description = ModuleDescriptions.getDescription(module);
            if (description == null || description.isEmpty() || description.equals("Описание модуля отсутствует")) {
                return;
            }

            MatrixStack matrix = context.getMatrices();
            if (matrix == null) return;

            FontRenderer font = Fonts.getSize(12, SuisseIntlSemiBold);
            if (font == null) return;

            float maxWidth = 200f;
            String wrappedText = StringHelper.wrap(description, (int) maxWidth, 12);
            if (wrappedText == null) return;

            String[] lines = wrappedText.split("\n");
            if (lines == null || lines.length == 0) return;

            float lineHeight = font.getStringHeight("A");
            float textPadding = 6f;

            float descriptionWidth = 0f;
            for (String line : lines) {
                if (line == null) continue;
                float lineWidth = font.getStringWidth(line);
                if (lineWidth > descriptionWidth) {
                    descriptionWidth = lineWidth;
                }
            }
            descriptionWidth = Math.min(descriptionWidth + textPadding, maxWidth);

            float descriptionHeight = lines.length * lineHeight;

            float descriptionX = x + width + 5f;
            float descriptionY = y;

            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();

            if (descriptionX + descriptionWidth > screenWidth) {
                descriptionX = x - descriptionWidth - 2.5f;
            }

            if (descriptionY + descriptionHeight > screenHeight) {
                descriptionY = screenHeight - descriptionHeight;
            }

            if (descriptionY < 0) {
                descriptionY = 0;
            }

            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            com.mojang.blaze3d.systems.RenderSystem.disableScissor();

            matrix.push();
            matrix.translate(0, 0, 3000);

            rectangle.render(ShapeProperties.create(matrix, descriptionX, descriptionY, descriptionWidth, descriptionHeight)
                    .round(3)
                    .color(new Color(25, 25, 25, 255).getRGB())
                    .build());

            float textX = descriptionX + textPadding/2;
            float textY = descriptionY + textPadding;

            int textColor = ThemeManager.textColor != null ? ThemeManager.textColor.getColor() : 0xFFFFFF;

            for (String line : lines) {
                if (line != null && font != null) {
                    font.drawString(matrix, line, textX, textY, textColor);
                    textY += lineHeight;
                }
            }

            matrix.pop();
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        } catch (Exception e) {
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        }
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isExpanded && !module.settings().isEmpty()) {
            updateSettingsPositions();
            
            
            String animationKey = "module_settings_" + module.getName();
            float slideProgress = AnimationHelper.getAnimationValue(animationKey, 1f);
            
            for (AbstractSettingComponent setting : components) {
                if (setting.getSetting().isVisible()) {
                    float settingOffset = (setting.y - y - 20 - 1) * (1f - slideProgress);
                    float actualSettingY = setting.y - settingOffset;
                    
                    
                    if (Calculate.isHovered(mouseX, mouseY, x, actualSettingY, width, setting.height)) {
                        if (setting.mouseClicked(mouseX, mouseY, button)) {
                            return true;
                        }
                    }
                }
            }
        }
        
        
        if (Calculate.isHovered(mouseX, mouseY, x, y, width, 20)) {
            if (button == 0) {
                String moduleToggleKey = "module_toggle_" + module.getName();
                AnimationHelper.startAnimation(moduleToggleKey, 1f, 1.1f, 150, AnimationHelper.EasingType.EASE_OUT);
                
                module.switchState();
                return true;
            } else if (button == 1 && !module.settings().isEmpty()) {
                wasExpanded = isExpanded;
                isExpanded = !isExpanded;
                
                String animationKey = "module_settings_" + module.getName();
                if (isExpanded) {
                    AnimationHelper.startAnimation(animationKey, 0f, 1f, 200, AnimationHelper.EasingType.EASE_OUT);
                } else {
                    AnimationHelper.startAnimation(animationKey, 1f, 0f, 150, AnimationHelper.EasingType.EASE_IN);
                }
                
                return true;
            } else if (button == 2) {
                binding = !binding;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        
        if (Calculate.isHovered(mouseX, mouseY, x, y, width, 20)) {
            return true;
        }
        
        if (isExpanded && !module.settings().isEmpty()) {
            updateSettingsPositions();
            
            
            String animationKey = "module_settings_" + module.getName();
            float slideProgress = AnimationHelper.getAnimationValue(animationKey, 1f);
            
            for (AbstractSettingComponent setting : components) {
                if (setting.getSetting().isVisible()) {
                    float settingOffset = (setting.y - y - 20 - 1) * (1f - slideProgress);
                    float actualSettingY = setting.y - settingOffset;
                    
                    
                    if (Calculate.isHovered(mouseX, mouseY, x, actualSettingY, width, setting.height)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    @Override
    public void tick() {
        if (isExpanded && !module.settings().isEmpty()) {
            components.stream().filter(setting -> setting.getSetting().isVisible()).forEach(AbstractComponent::tick);
        }
        super.tick();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isExpanded && !module.settings().isEmpty()) {
            updateSettingsPositions();
            for (AbstractSettingComponent setting : components) {
                if (setting.getSetting().isVisible() && setting.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                    return true;
                }
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isExpanded && !module.settings().isEmpty()) {
            updateSettingsPositions();
            for (AbstractSettingComponent setting : components) {
                if (setting.getSetting().isVisible()) {
                    setting.mouseReleased(mouseX, mouseY, button);
                }
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (isExpanded && !module.settings().isEmpty()) {
            updateSettingsPositions();
            for (AbstractSettingComponent setting : components) {
                if (setting.getSetting().isVisible() && setting.mouseScrolled(mouseX, mouseY, amount)) {
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                binding = false;
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_DELETE) {
                module.setKey(GLFW.GLFW_KEY_UNKNOWN);
                binding = false;
                return true;
            } else {
                module.setKey(keyCode);
                binding = false;
                return true;
            }
        }
        
        if (isExpanded && !module.settings().isEmpty()) {
            for (AbstractSettingComponent setting : components) {
                if (setting.getSetting().isVisible() && setting.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (isExpanded && !module.settings().isEmpty()) {
            for (AbstractSettingComponent setting : components) {
                if (setting.getSetting().isVisible() && setting.charTyped(chr, modifiers)) {
                    return true;
                }
            }
        }
        return super.charTyped(chr, modifiers);
    }

    public int getComponentHeight() {
        int baseHeight = 20;
        
        String animationKey = "module_settings_" + module.getName();
        boolean isAnimating = AnimationHelper.isAnimationActive(animationKey);
        
        if ((isExpanded() && !module.settings().isEmpty()) || isAnimating) {
            int settingsHeight = 0;
            for (AbstractSettingComponent setting : components) {
                if (setting.getSetting().isVisible()) {
                    settingsHeight += setting.height +1;
                }
            }
            settingsHeight += 2;
            
            float slideProgress = AnimationHelper.getAnimationValue(animationKey, 1f);
            
            return baseHeight + (int)(settingsHeight * slideProgress );
        }
        
        return baseHeight;
    }
    

    public float getSettingsBottomY() {
        String animationKey = "module_settings_" + module.getName();
        boolean isAnimating = AnimationHelper.isAnimationActive(animationKey);
        
        if ((isExpanded() && !module.settings().isEmpty()) || isAnimating) {

            int settingsHeight = 0;
            for (AbstractSettingComponent setting : components) {
                if (setting.getSetting().isVisible()) {
                    settingsHeight += setting.height + 1;
                }
            }
            settingsHeight += 2;
            
            float slideProgress = AnimationHelper.getAnimationValue(animationKey, 1f);
            float currentHeight = settingsHeight * slideProgress;

            return y + 20 + 1 + currentHeight ;
        }

        return y + 20;
    }
    
    private boolean isExpanded = false;
    private boolean wasExpanded = false;
    
    private boolean isExpanded() {
        return isExpanded;
    }
    
    private void renderSettings(DrawContext context, int mouseX, int mouseY, float delta) {
        updateSettingsPositions();
        
        String animationKey = "module_settings_" + module.getName();
        
        float slideProgress = AnimationHelper.getAnimationValue(animationKey, 1f);

        int settingsHeight = 0;
        for (AbstractSettingComponent setting : components) {
            if (setting.getSetting().isVisible()) {
                settingsHeight += setting.height + 1; 
            }
        }
        settingsHeight += 2;

        float currentHeight = settingsHeight * slideProgress;
        float backgroundHeight = currentHeight;
        
        int backgroundColor = ThemeManager.BackgroundSettings.getColor();

        if (backgroundHeight > 0) {
            rectangle.render(ShapeProperties.create(context.getMatrices(), x, y + 20 + 1, width, backgroundHeight)
                    .round(3)
                    .color(backgroundColor)
                    .build());
        }

        float prevAlpha = RenderSystem.getShaderColor()[3];
        RenderSystem.setShaderColor(1f, 1f, 1f, slideProgress * prevAlpha);
        for (AbstractSettingComponent setting : components) {
            if (setting.getSetting().isVisible()) {
                float originalY = setting.y;
                float originalX = setting.x;
                setting.x = x;
                
                setting.render(context, mouseX, mouseY, delta);
                
                setting.y = originalY;
                setting.x = originalX;
            }
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, prevAlpha);
    }

    private void updateSettingsPositions() {
        float settingsY = y + 20 + 2 + 4f;
        for (AbstractSettingComponent setting : components) {
            if (setting.getSetting().isVisible()) {
                setting.x = x;
                setting.y = settingsY;
                setting.width = width;
                settingsY += setting.height + 1;
            }
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModuleComponent that = (ModuleComponent) o;
        return module.equals(that.module);
    }

    @Override
    public int hashCode() {
        return Objects.hash(module);
    }
}