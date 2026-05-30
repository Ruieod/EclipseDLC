package code.essence.display.screens.clickgui.components.implement.panel;

import code.essence.Essence;
import code.essence.display.screens.clickgui.components.AbstractComponent;
import code.essence.display.screens.clickgui.components.implement.module.ModuleComponent;
import code.essence.display.screens.clickgui.components.implement.customization.CustomizationComponent;
import code.essence.display.screens.clickgui.components.implement.configs.ConfigsComponent;

import java.util.logging.Logger;
import code.essence.features.impl.render.Hud;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.display.render.font.FontRenderer;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.geometry.Render2D;
import code.essence.utils.display.scissor.ScissorAssist;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.display.render.post.KawaseBlur;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PanelComponent extends AbstractComponent implements QuickImports {
    private final List<ModuleComponent> moduleComponents = new ArrayList<>();
    private static final Set<ModuleComponent> globalModuleComponents = new HashSet<>();
    private final ModuleCategory category;
    private final int index;
    private final CustomizationComponent customizationComponent;
    private final ConfigsComponent configsComponent;
    private static final Logger logger = Logger.getLogger(PanelComponent.class.getName());
    
    private float scroll = 0;
    private float smoothedScroll = 0;
    private boolean searching = false;
    private String searchText = "";

    public PanelComponent(ModuleCategory category, int index) {
        this.category = category;
        this.index = index;
        this.customizationComponent = new CustomizationComponent();
        this.configsComponent = new ConfigsComponent();
        initializeModules();
    }

    private boolean shouldRenderCustomization() {
        try {
            code.essence.features.impl.render.ClickGui clickGui = code.essence.features.impl.render.ClickGui.getInstance();
            return clickGui != null && clickGui.shouldRenderCustomization();
        } catch (Exception ignored) {
            return true;
        }
    }

    private boolean shouldRenderConfigs() {
        try {
            code.essence.features.impl.render.ClickGui clickGui = code.essence.features.impl.render.ClickGui.getInstance();
            return clickGui != null && clickGui.shouldRenderConfigs();
        } catch (Exception ignored) {
            return true;
        }
    }

    private void initializeModules() {
        List<Module> modules = Essence.getInstance()
                .getModuleRepository()
                .modules();
        for (Module module : modules) {
            if (module.getCategory() != category) continue;
            ModuleComponent newComponent = new ModuleComponent(module);
            if (globalModuleComponents.add(newComponent)) {
                moduleComponents.add(newComponent);
            }
        }
    }

    public List<ModuleComponent> getModuleComponents() {
        return moduleComponents;
    }

    public boolean isModuleVisible(ModuleComponent component) {
        if (component == null) return false;

        float categoryTop = y + 35 - 8;
        float categoryBottom = y + 35 + 22;
        float moduleStartY = categoryBottom + 4;
        float clipTop = moduleStartY;
        float clipBottom = y + height;

        float componentY = component.y;
        float componentHeight = component.getComponentHeight();
        float componentBottom = componentY + componentHeight;

        return componentBottom >= clipTop && componentY <= clipBottom;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if ((category == ModuleCategory.CUSTOMIZATION && !shouldRenderCustomization()) ||
            (category == ModuleCategory.CONFIGS && !shouldRenderConfigs())) {
            return;
        }
        
        MatrixStack matrix = context.getMatrices();

        if (Hud.blur.isValue()) {
            blur(context, matrix, mouseX, mouseY);
        }
        drawPanelMainBackground(context, matrix, mouseX, mouseY);
        
        drawModulesVertical(context, matrix, mouseX, mouseY, delta);
        
        drawCategoryBackground(context, matrix, mouseX, mouseY);
        

        
        rectangle.render(ShapeProperties.create(matrix, x - 6, y + 22, width + 12, 30)
                .round(15F)
                .color(255, 255, 255, 20)
                .build());
        
        rectangle.render(ShapeProperties.create(matrix, x - 4, y + 24, width + 8, 26)
                .round(12F)
                .color(255, 255, 255, 40)
                .build());
        
        rectangle.render(ShapeProperties.create(matrix, x - 2, y + 26, width + 4, 22)
                .round(8F)
                .color(255, 255, 255, 60)
                .build());
        
        rectangle.render(ShapeProperties.create(matrix, x + 2, y + 28, width - 13, 18)
                .round(6F)
                .color(255, 255, 255, 30)
                .build());
        

    }

    private void blur(DrawContext context, MatrixStack matrix, int mouseX, int mouseY) {
        Render2D.rectangleWithMask(matrix.peek().getPositionMatrix(),  x - 2, y + 31, width + 4, height - 27, 6f,0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment());

    }

    private void drawPanelMainBackground(DrawContext context, MatrixStack matrix, int mouseX, int mouseY) {
        rectangle.render(ShapeProperties.create(matrix, x - 2, y + 31, width + 4, height - 27 )
                .round(6f)
                .color(ThemeManager.BackgroundGui.getColor())
                .build());
    }
    

    private void drawCategoryBackground(DrawContext context, MatrixStack matrix, int mouseX, int mouseY) {
        float moduleWidth = width - 4;
        float categoryWidth = moduleWidth + 3;
        float categoryX = x + 3 - 2.5f;
        float categoryY = y + 34f;
        
        rectangle.render(ShapeProperties.create(matrix, categoryX, categoryY, categoryWidth, 22)
                .round(4f)
                        .softness(4)
                .color(ColorAssist.getClientColor(1f), ColorAssist.getClientColor(1f),
                        ColorAssist.getClientColor2(1f), ColorAssist.getClientColor2(1f))
                .build());

        rectangle.render(ShapeProperties.create(matrix, categoryX - 4, categoryY - 4, categoryWidth + 8, 30)
                .round(8f)
                .softness(4)
                .color(new Color(255, 255, 255, 5).getRGB())
                .build());

        rectangle.render(ShapeProperties.create(matrix, categoryX - 3, categoryY - 3, categoryWidth + 6, 28)
                .round(7f)
                .softness(3)
                .color(ColorAssist.getClientColor(0.1f), ColorAssist.getClientColor(0.1f),
                        ColorAssist.getClientColor2(0.1f), ColorAssist.getClientColor2(0.1f))
                .build());

        rectangle.render(ShapeProperties.create(matrix, categoryX - 2, categoryY - 2, categoryWidth + 4, 26)
                .round(6f)
                .softness(2)
                .color(ColorAssist.getClientColor(0.1f), ColorAssist.getClientColor(0.1f),
                        ColorAssist.getClientColor2(0.1f), ColorAssist.getClientColor2(0.1f))
                .build());

        String icon = getCategoryIcon();
        boolean isRenderOrMisc = category == ModuleCategory.RENDER || category == ModuleCategory.MISC;
        int iconFontSize = isRenderOrMisc ? 80 : 18;
        float scale = isRenderOrMisc ? 18f / 76f : 1f;
        float iconSize = Fonts.getSize(iconFontSize, Fonts.Type.ESSENCE).getStringWidth(icon) * scale;
        float nameWidth = Fonts.getSize(18, Fonts.Type.SuisseIntlSemiBold).getStringWidth(category.getReadableName());

        float categoryCenterX = categoryX + categoryWidth / 2f;
        float categoryCenterY = categoryY + 22 / 2f;

        float totalTextWidth = nameWidth + iconSize + 5;
        float textStartX = categoryCenterX - (totalTextWidth / 2f);
        
        float iconY = categoryCenterY - 1f;
        if (isRenderOrMisc) {
            iconY = categoryCenterY - 3.5f;
        }
        
        if (isRenderOrMisc) {
            matrix.push();
            matrix.translate(textStartX, iconY, 0);
            matrix.scale(scale, scale, 1);
            Fonts.getSize(iconFontSize, Fonts.Type.ESSENCE).drawString(matrix, icon, 0, 0, ThemeManager.textColor.getColor());
            matrix.pop();
        } else {
            Fonts.getSize(iconFontSize, Fonts.Type.ESSENCE).drawString(matrix, icon,
                    textStartX, iconY, ThemeManager.textColor.getColor());
        }


        FontRenderer fontCategory = Fonts.getSize(18, Fonts.Type.SuisseIntlMedium);
        fontCategory.drawString(matrix, category.getReadableName(),
                textStartX + iconSize + 5, categoryCenterY - 1.5f, ThemeManager.textColor.getColor());

    }

    private void drawModulesVertical(DrawContext context, MatrixStack matrix, int mouseX, int mouseY, float delta) {
        float totalModuleHeight = 0;

        if (category == ModuleCategory.CUSTOMIZATION && shouldRenderCustomization()) {
            totalModuleHeight += customizationComponent.height + 2;
        }

        if (category == ModuleCategory.CONFIGS && shouldRenderConfigs()) {
            totalModuleHeight += configsComponent.calculateHeight() + 2;
        }
        
        for (ModuleComponent component : moduleComponents) {
            if (shouldRenderModule(component)) {
                totalModuleHeight += component.getComponentHeight() + 2;
            }
        }

        float categoryTop = y + 35 - 8;
        float categoryBottom = y + 35 + 22;
        float moduleStartY = categoryBottom + 4;
        float visibleHeight = (y + height) - moduleStartY;
        
        if (totalModuleHeight > visibleHeight) {
            float maxScroll = -(totalModuleHeight - visibleHeight);
            scroll = MathHelper.clamp(scroll, maxScroll, 0);
        } else {
            scroll = 0;
        }
        
        smoothedScroll = Calculate.interpolateSmooth(2, smoothedScroll, scroll);

        Matrix4f positionMatrix = matrix.peek().getPositionMatrix();
        ScissorAssist scissorManager = Essence.getInstance().getScissorManager();
        
        float clipTop = moduleStartY;
        float clipBottom = y + height;
        scissorManager.push(positionMatrix, x, moduleStartY, width, clipBottom - moduleStartY);

        float moduleX = x + 1.5f;
        float moduleY = moduleStartY + smoothedScroll;

        if (category == ModuleCategory.CUSTOMIZATION && shouldRenderCustomization()) {
            float customizationHeight = customizationComponent.height + 2;
            float customizationBottom = moduleY + customizationComponent.height;
            
            if (customizationBottom >= clipTop && moduleY <= clipBottom) {
                customizationComponent.x = moduleX;
                customizationComponent.y = moduleY;
                customizationComponent.width = width - 3;
                customizationComponent.render(context, mouseX, mouseY, delta);
            }
            moduleY += customizationHeight;
        }

        if (category == ModuleCategory.CONFIGS && shouldRenderConfigs()) {
            float configsHeight = configsComponent.calculateHeight() + 2;
            float configsBottom = moduleY + configsComponent.calculateHeight();
            
            if (configsBottom >= clipTop && moduleY <= clipBottom) {
                configsComponent.x = moduleX;
                configsComponent.y = moduleY;
                configsComponent.width = width - 3;
                configsComponent.render(context, mouseX, mouseY, delta);
            }
            moduleY += configsHeight;
        }

        for (ModuleComponent component : moduleComponents) {
            if (!shouldRenderModule(component)) continue;

            component.x = moduleX;
            component.y = moduleY;
            component.width = width - 3;

            float componentBottom = moduleY + component.getComponentHeight();

            if (componentBottom >= clipTop && moduleY <= clipBottom) {
                int heightBefore = component.getComponentHeight();
                component.render(context, mouseX, mouseY, delta);
                int heightAfter = component.getComponentHeight();
            }

            float settingsBottomY = component.getSettingsBottomY();
            moduleY = settingsBottomY + 2;
        }

        scissorManager.pop();

        if (category == ModuleCategory.CUSTOMIZATION && shouldRenderCustomization()) {
            customizationComponent.themeInput.x = x + 1.5f;
            customizationComponent.themeInput.y = y + height - 21;
            customizationComponent.themeInput.width = width - 3;
            customizationComponent.themeInput.render(context, mouseX, mouseY, delta);
        }

        if (category == ModuleCategory.CONFIGS && shouldRenderConfigs()) {
            configsComponent.configInput.x = x + 1.5f;
            configsComponent.configInput.y = y + height - 21;
            configsComponent.configInput.width = width - 3;
            configsComponent.configInput.render(context, mouseX, mouseY, delta);
        }

        if (totalModuleHeight > visibleHeight) {
        }
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (Calculate.isHovered(mouseX, mouseY, x, y, width, height)) {
            List<ModuleComponent> visibleModules = new ArrayList<>();
            float categoryTop = y + 35 - 8;
            float categoryBottom = y + 35 + 22;
            float moduleStartY = categoryBottom + 4;
            float moduleY = moduleStartY + smoothedScroll;
            float clipTop = moduleStartY;
            float clipBottom = y + height;

            if (category == ModuleCategory.CUSTOMIZATION && shouldRenderCustomization()) {
                int customizationHeight = (int) (customizationComponent.height + 2);
                float customizationBottom = moduleY + customizationHeight;

                if (customizationBottom >= clipTop && moduleY <= clipBottom) {
                    customizationComponent.x = x + 1.5f;
                    customizationComponent.y = moduleY;
                    customizationComponent.width = width - 3;
                    
                    if (customizationComponent.mouseClicked(mouseX, mouseY, button)) {
                        return true;
                    }
                }
                moduleY += customizationHeight;
                

                customizationComponent.themeInput.x = x + 1.5f;
                customizationComponent.themeInput.y = y + height - 21;
                customizationComponent.themeInput.width = width - 3;
                if (customizationComponent.themeInput.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }

            if (category == ModuleCategory.CONFIGS && shouldRenderConfigs()) {
                int configsHeight = (int) (configsComponent.calculateHeight() + 2);
                float configsBottom = moduleY + configsHeight;

                if (configsBottom >= clipTop && moduleY <= clipBottom) {
                    configsComponent.x = x + 1.5f;
                    configsComponent.y = moduleY;
                    configsComponent.width = width - 3;
                    
                    if (configsComponent.mouseClicked(mouseX, mouseY, button)) {
                        return true;
                    }
                }
                moduleY += configsHeight;

                configsComponent.configInput.x = x + 1.5f;
                configsComponent.configInput.y = y + height - 21;
                configsComponent.configInput.width = width - 3;
                if (configsComponent.configInput.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }

            for (ModuleComponent component : moduleComponents) {
                if (!shouldRenderModule(component)) continue;


                component.x = x + 1.5f;
                component.y = moduleY;
                component.width = width - 3;

                float moduleBottom = moduleY + component.getComponentHeight();

                if (moduleBottom >= clipTop && moduleY <= clipBottom) {
                    visibleModules.add(component);
                }

                float settingsBottomY = component.getSettingsBottomY();
                moduleY = settingsBottomY + 2;
            }

            for (int i = visibleModules.size() - 1; i >= 0; i--) {
                ModuleComponent moduleComponent = visibleModules.get(i);
                if (moduleComponent.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (Calculate.isHovered(mouseX, mouseY, x, y, width, height)) {
            List<ModuleComponent> visibleModules = new ArrayList<>();
            float categoryTop = y + 35 - 8;
            float categoryBottom = y + 35 + 22;
            float moduleStartY = categoryBottom + 4;
            float moduleY = moduleStartY + smoothedScroll;
            float clipTop = moduleStartY;
            float clipBottom = y + height;

            if (category == ModuleCategory.CUSTOMIZATION && shouldRenderCustomization()) {
                int customizationHeight = (int) (customizationComponent.height + 2);
                float customizationBottom = moduleY + customizationHeight;

                if (customizationBottom >= clipTop && moduleY <= clipBottom) {
                    customizationComponent.x = x + 1.5f;
                    customizationComponent.y = moduleY;
                    customizationComponent.width = width - 3;
                    
                    if (customizationComponent.mouseReleased(mouseX, mouseY, button)) {
                        return true;
                    }
                }
                moduleY += customizationHeight;
            }

            if (category == ModuleCategory.CONFIGS && shouldRenderConfigs()) {
                int configsHeight = (int) (configsComponent.calculateHeight() + 2);
                float configsBottom = moduleY + configsHeight;

                if (configsBottom >= clipTop && moduleY <= clipBottom) {
                    configsComponent.x = x + 1.5f;
                    configsComponent.y = moduleY;
                    configsComponent.width = width - 3;
                    
                    if (configsComponent.mouseReleased(mouseX, mouseY, button)) {
                        return true;
                    }
                }
                moduleY += configsHeight;
            }

            for (ModuleComponent component : moduleComponents) {
                if (!shouldRenderModule(component)) continue;


                component.x = x + 1.5f;
                component.y = moduleY;
                component.width = width - 3;

                float moduleBottom = moduleY + component.getComponentHeight();

                if (moduleBottom >= clipTop && moduleY <= clipBottom) {
                    visibleModules.add(component);
                }

                float settingsBottomY = component.getSettingsBottomY();
                moduleY = settingsBottomY + 2;
            }

            for (int i = visibleModules.size() - 1; i >= 0; i--) {
                ModuleComponent moduleComponent = visibleModules.get(i);
                if (moduleComponent.mouseReleased(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (Calculate.isHovered(mouseX, mouseY, x, y + 56, width, height - 52)) {
            
            int scrollSpeed = (int) code.essence.features.impl.render.ClickGui.scrollSpeed.getValue();
            
            
            float baseModuleHeight = 22f; 
            
            
            float scrollStep = baseModuleHeight * scrollSpeed;
            scroll += amount > 0 ? scrollStep : -scrollStep;
        }

        if (Calculate.isHovered(mouseX, mouseY, x, y, width, height)) {
            List<ModuleComponent> visibleModules = new ArrayList<>();
            float categoryTop = y + 35 - 8;
            float categoryBottom = y + 35 + 22;
            float moduleStartY = categoryBottom + 4;
            float moduleY = moduleStartY + smoothedScroll;
            float clipTop = moduleStartY;
            float clipBottom = y + height;
            
            for (ModuleComponent component : moduleComponents) {
                if (!shouldRenderModule(component)) continue;

                component.x = x + 1.5f;
                component.y = moduleY;
                component.width = width - 3;
                
                float moduleBottom = moduleY + component.getComponentHeight();
                
                if (moduleBottom >= clipTop && moduleY <= clipBottom) {
                    visibleModules.add(component);
                }
                

                float settingsBottomY = component.getSettingsBottomY();
                moduleY = settingsBottomY + 2;
            }
            
            for (int i = visibleModules.size() - 1; i >= 0; i--) {
                ModuleComponent moduleComponent = visibleModules.get(i);
                if (moduleComponent.mouseScrolled(mouseX, mouseY, amount)) {
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isPanelVisible()) {
            return false;
        }

        if (category == ModuleCategory.CUSTOMIZATION) {
            if (customizationComponent.themeInput.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        if (category == ModuleCategory.CONFIGS) {
            if (configsComponent.configInput.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        
        for (int i = moduleComponents.size() - 1; i >= 0; i--) {
            ModuleComponent moduleComponent = moduleComponents.get(i);
            if (shouldRenderModule(moduleComponent)) {
                if (moduleComponent.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!isPanelVisible()) {
            return false;
        }

        if (category == ModuleCategory.CUSTOMIZATION) {
            if (customizationComponent.themeInput.charTyped(chr, modifiers)) {
                return true;
            }
        }

        if (category == ModuleCategory.CONFIGS) {
            if (configsComponent.configInput.charTyped(chr, modifiers)) {
                return true;
            }
        }
        
        for (int i = moduleComponents.size() - 1; i >= 0; i--) {
            ModuleComponent moduleComponent = moduleComponents.get(i);
            if (shouldRenderModule(moduleComponent)) {
                if (moduleComponent.charTyped(chr, modifiers)) {
                    return true;
                }
            }
        }
        return super.charTyped(chr, modifiers);
    }

    private boolean shouldRenderModule(ModuleComponent component) {
        if (!searchText.isEmpty()) {
            boolean matches = component.getModule().getVisibleName().toLowerCase().contains(searchText.toLowerCase());
            if (matches) {
            }
            return matches;
        }
        return true;
    }
    
    private boolean isPanelVisible() {
        float screenWidth = window.getScaledWidth();
        float screenHeight = window.getScaledHeight();

        boolean visible = x < screenWidth && x + width > 0 && y < screenHeight && y + height > 0;
        
        if (screenWidth < 320 || screenHeight < 240) {
            return false;
        }
        
        return visible;
    }

    private String getCategoryIcon() {
        return switch (category) {
            case COMBAT -> "b";
            case MOVEMENT -> "d";
            case RENDER -> "e";
            case PLAYER -> "c";
            case MISC -> "f";
            case CUSTOMIZATION -> "g";
            case CONFIGS -> "h";
        };
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public void setSearchText(String text) {
        this.searchText = text;
    }

    public int getIndex() {
        return index;
    }
}

