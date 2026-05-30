package code.essence.display.screens.clickgui.components.implement.category;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.common.animation.Animation;
import code.essence.common.animation.Direction;
import code.essence.common.animation.implement.InOutBack;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.display.screens.clickgui.components.implement.module.ModuleComponent;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.display.scissor.ScissorAssist;
import code.essence.Essence;
import code.essence.display.screens.clickgui.MenuScreen;
import code.essence.display.screens.clickgui.components.AbstractComponent;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryComponent extends AbstractComponent {
    private final List<ModuleComponent> moduleComponents = new ArrayList<>();
    private static final Set<ModuleComponent> globalModuleComponents = new HashSet<>();
    private final ModuleCategory category;
    private final Animation alphaAnimation = new InOutBack().setMs(300).setValue(1);
    private final Animation scaleAnimation = new InOutBack().setMs(300).setValue(1);
    private boolean initializedAnimations = false;
    private float scroll = 0;
    private float smoothedScroll = 0;

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

    public void postInitialize() {
        if (!initializedAnimations) {
            if (MenuScreen.INSTANCE.getCategory().equals(category)) {
                alphaAnimation.setDirection(Direction.FORWARDS);
                scaleAnimation.setDirection(Direction.FORWARDS);
                alphaAnimation.reset();
                scaleAnimation.reset();
                alphaAnimation.setMs(0);
                scaleAnimation.setMs(0);
            } else {
                alphaAnimation.setDirection(Direction.BACKWARDS);
                scaleAnimation.setDirection(Direction.BACKWARDS);
                alphaAnimation.reset();
                scaleAnimation.reset();
                alphaAnimation.setMs(0);
                scaleAnimation.setMs(0);
            }
            initializedAnimations = true;
        }
    }

    public CategoryComponent(ModuleCategory category) {
        this.category = category;
        initializeModules();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        
        postInitialize();
        MenuScreen menuScreen = MenuScreen.INSTANCE;
        globalModuleComponents.clear();
        Matrix4f positionMatrix = context.getMatrices().peek().getPositionMatrix();
        ScissorAssist scissorManager = Essence.getInstance().getScissorManager();
        

        drawCategoryTab(context, context.getMatrices(), mouseX, mouseY);
        
        drawModulesVertical(context, mouseX, mouseY, delta, positionMatrix, scissorManager, menuScreen);
    }

    private void drawModulesVertical(DrawContext context, int mouseX, int mouseY, float delta, 
                                     Matrix4f positionMatrix, ScissorAssist scissorManager, MenuScreen menuScreen) {
        float totalModuleHeight = 0;
        for (ModuleComponent component : moduleComponents) {
            if (shouldRenderComponent(component)) {
                totalModuleHeight += component.getComponentHeight() + 5;
            }
        }

        float offsetX = 35, offsetY = 14;
        float visibleHeight = menuScreen.height - offsetY;
        
        float categoryTop = menuScreen.y + 14 - 8;
        
        if (totalModuleHeight > visibleHeight) {
            float maxScroll = -(totalModuleHeight - visibleHeight);
            scroll = MathHelper.clamp(scroll, maxScroll, 0);
        } else {
            scroll = 0;
        }
        
        smoothedScroll = Calculate.interpolateSmooth(8, smoothedScroll, scroll);

        scissorManager.push(positionMatrix, menuScreen.x + offsetX - 75, categoryTop, 
                           menuScreen.width - offsetX + 150, menuScreen.height - categoryTop + 15);

        float moduleX = menuScreen.x + 36;
        float moduleY = menuScreen.y + 25 + smoothedScroll;
        float clipTop = categoryTop;
        float clipBottom = clipTop + (menuScreen.height - categoryTop + 15);

        for (ModuleComponent component : moduleComponents) {
            if (!shouldRenderComponent(component)) continue;

            int componentHeight = component.getComponentHeight() + 5;
            float moduleBottom = moduleY + componentHeight;

            if (moduleBottom < clipTop || moduleY > clipBottom) {
                moduleY += componentHeight;
                continue;
            }

            component.x = moduleX;
            component.y = moduleY;
            component.width = 179;
            
            if (component.y > menuScreen.y - componentHeight && 
                menuScreen.y + menuScreen.height + 15 > component.y) {
                component.render(context, mouseX, mouseY, delta);
            }

            moduleY += componentHeight;
        }

        scissorManager.pop();

        if (totalModuleHeight > visibleHeight) {
            
        }
    }

    private void renderScrollbar(DrawContext context, MenuScreen menuScreen, float totalHeight, 
                                 float visibleHeight, float offsetX, float offsetY) {
        float scrollbarWidth = 4;
        float scrollbarX = menuScreen.x + menuScreen.width - offsetX - scrollbarWidth + 50;
        float scrollbarY = menuScreen.y + offsetY + 12;
        float scrollbarHeight = menuScreen.height - offsetY * 2;

        rectangle.render(ShapeProperties.create(context.getMatrices(), scrollbarX, scrollbarY, 
                scrollbarWidth, scrollbarHeight)
                .round(2F)
                .color(new Color(30, 30, 30, 100).getRGB())
                .build());

        float handleHeight = Math.max(20, visibleHeight * (visibleHeight / totalHeight));
        float maxScroll = -(totalHeight - visibleHeight);
        float scrollRatio = maxScroll != 0 ? (-smoothedScroll) / (-maxScroll) : 0;
        float handleY = scrollbarY + (scrollbarHeight - handleHeight) * scrollRatio;

        rectangle.render(ShapeProperties.create(context.getMatrices(), scrollbarX, handleY,
                scrollbarWidth, handleHeight)
                .round(2F)
                .color(new Color(100, 100, 100, 150).getRGB())
                .build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MenuScreen menuScreen = MenuScreen.INSTANCE;
        float scale = 0.5f + (scaleAnimation.getOutput().floatValue() * 0.5f);
        float baseWidth = 20;
        float baseHeight = 20;
        float scaledWidth = baseWidth * scale;
        float scaledHeight = baseHeight * scale;
        float baseX = ModuleCategory.RENDER.equals(category) ? x + 4.65f : ModuleCategory.MOVEMENT.equals(category) ? x + 4.75f : x + 5.25f;
        float baseY = y;
        float centerX = baseX + baseWidth / 2;
        float centerY = baseY + baseHeight / 2;
        float scaledX = centerX - scaledWidth / 2;
        float scaledY = centerY - scaledHeight / 2;
        float hoverX = ModuleCategory.RENDER.equals(category) ? x + 4.65f : ModuleCategory.MOVEMENT.equals(category) ? x + 4.75f : x + 5.25f;
        float hoverY = y;
        
        if (Calculate.isHovered(mouseX, mouseY, hoverX, hoverY, baseWidth, baseHeight) && button == 0) {
            MenuScreen.INSTANCE.setCategory(category);
            alphaAnimation.setMs(300);
            scaleAnimation.setMs(300);
            alphaAnimation.setDirection(Direction.FORWARDS);
            scaleAnimation.setDirection(Direction.FORWARDS);
            return true;
        }
        
        float offsetX = 84, offsetY = 29;
        if (Calculate.isHovered(mouseX, mouseY, menuScreen.x + offsetX, menuScreen.y + offsetY, 
                               menuScreen.width - offsetX, menuScreen.height - offsetY)) {
            for (ModuleComponent moduleComponent : moduleComponents) {
                if (shouldRenderComponent(moduleComponent) && moduleComponent.isHover(mouseX, mouseY)) {
                    moduleComponent.mouseClicked(mouseX, mouseY, button);
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        float scale = 0.5f + (scaleAnimation.getOutput().floatValue() * 0.5f);
        float baseWidth = 20;
        float baseHeight = 20;
        float scaledWidth = baseWidth * scale;
        float scaledHeight = baseHeight * scale;
        float baseX = ModuleCategory.RENDER.equals(category) ? x + 4.65f : ModuleCategory.MOVEMENT.equals(category) ? x + 4.75f : x + 5.25f;
        float baseY = y;
        float centerX = baseX + baseWidth / 2;
        float centerY = baseY + baseHeight / 2;
        float scaledX = centerX - scaledWidth / 2;
        float scaledY = centerY - scaledHeight / 2;

        boolean isHovered = Calculate.isHovered(mouseX, mouseY, scaledX, scaledY, scaledWidth, scaledHeight);

        if (isHovered) {
            return true;
        }

        moduleComponents.forEach(moduleComponent -> moduleComponent.isHover(mouseX, mouseY));
        for (ModuleComponent moduleComponent : moduleComponents) {
            if (moduleComponent.isHover(mouseX, mouseY)) {
                return true;
            }
        }
        return super.isHover(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        moduleComponents.forEach(moduleComponent -> moduleComponent.mouseReleased(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        MenuScreen menuScreen = MenuScreen.INSTANCE;
        float offsetX = 35, offsetY = 13;
        if (Calculate.isHovered(mouseX, mouseY, menuScreen.x + offsetX, menuScreen.y + offsetY, 
                               menuScreen.width - offsetX + 7, menuScreen.height - offsetY + 15)) {
            scroll += amount * 20;
        }
        moduleComponents.forEach(moduleComponent -> {
            if (shouldRenderComponent(moduleComponent)) {
                moduleComponent.mouseScrolled(mouseX, mouseY, amount);
            }
        });
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        moduleComponents.forEach(moduleComponent -> {
            if (shouldRenderComponent(moduleComponent)) {
                moduleComponent.keyPressed(keyCode, scanCode, modifiers);
            }
        });
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        moduleComponents.forEach(moduleComponent -> {
            if (shouldRenderComponent(moduleComponent)) {
                moduleComponent.charTyped(chr, modifiers);
            }
        });
        return super.charTyped(chr, modifiers);
    }

    private void drawCategoryTab(DrawContext context, MatrixStack matrix, int mouseX, int mouseY) {

        
        alphaAnimation.setDirection(MenuScreen.INSTANCE.getCategory().equals(category) ? Direction.FORWARDS : Direction.BACKWARDS);
        scaleAnimation.setDirection(MenuScreen.INSTANCE.getCategory().equals(category) ? Direction.FORWARDS : Direction.BACKWARDS);
        float anim = alphaAnimation.getOutput().floatValue();
        float scale = 0.5f + (scaleAnimation.getOutput().floatValue() * 0.5f);
        int alpha = MathHelper.clamp((int) (anim * 135), 0, 135);
        float baseWidth = 179 + 1; 
        float baseHeight = 20;
        float scaledWidth = baseWidth * scale;
        float scaledHeight = baseHeight * scale;
        
        MenuScreen menuScreen = MenuScreen.INSTANCE;
        float moduleCenterX = menuScreen.x + 36 + 179 / 2f;
        float moduleCenterY = menuScreen.y + 14;
        
        float baseX = moduleCenterX - baseWidth / 2f;
        float baseY = moduleCenterY - baseHeight / 2f;
        float centerX = baseX + baseWidth / 2;
        float centerY = baseY + baseHeight / 2;
        float scaledX = centerX - scaledWidth / 2;
        float scaledY = centerY - scaledHeight / 2;
        float hoverX = baseX;
        float hoverY = baseY;
        

        if (MenuScreen.INSTANCE.getCategory().equals(category)) {

        } else {
            Color glowColor = new Color(ColorAssist.getClientColor());
            


            

            rectangle.render(ShapeProperties.create(matrix, hoverX, hoverY, baseWidth, baseHeight)
                    .round(4F)
                    .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(),
                           ColorAssist.getClientColor2(), ColorAssist.getClientColor())
                    .build());
        }
        String icon;
        switch (category) {
            case COMBAT -> icon = "b";
            case MOVEMENT -> icon = "c";
            case RENDER -> icon = "d";
            case PLAYER -> icon = "e";
            case MISC -> icon = "f";
            case CONFIGS -> icon = "h";
            case CUSTOMIZATION -> icon = "i";
            default -> icon = category.getReadableName().substring(0, 1);
        }
        float iconX = centerX;
        float iconY = centerY;
        
        if (ModuleCategory.COMBAT.equals(category)) {
            Fonts.getSize(21, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "A", iconX, iconY, ColorAssist.getText(0.7F));
        }
        if (ModuleCategory.MOVEMENT.equals(category)) {
            Fonts.getSize(23, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "B", iconX, iconY, ColorAssist.getText(0.7F));
        }
        if (ModuleCategory.RENDER.equals(category)) {
            Fonts.getSize(21, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "C", iconX, iconY, ColorAssist.getText(0.7F));
        }
        if (ModuleCategory.PLAYER.equals(category)) {
            Fonts.getSize(23, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "D", iconX, iconY, ColorAssist.getText(0.7F));
        }
        if (ModuleCategory.MISC.equals(category)) {
            Fonts.getSize(21, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "E", iconX, iconY, ColorAssist.getText(0.7F));
        }
        if (ModuleCategory.CONFIGS.equals(category)) {
            Fonts.getSize(21, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "F", iconX, iconY, ColorAssist.getText(0.7F));
        }
        if (ModuleCategory.CUSTOMIZATION.equals(category)) {
            Fonts.getSize(21, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "H", iconX, iconY, ColorAssist.getText(0.7F));
        }
    }

    private boolean shouldRenderComponent(ModuleComponent component) {
        return component.getModule().getCategory().equals(category);
    }
}
