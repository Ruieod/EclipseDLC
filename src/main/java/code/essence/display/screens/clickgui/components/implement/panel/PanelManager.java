package code.essence.display.screens.clickgui.components.implement.panel;

import code.essence.common.animation.Easy.EaseBackIn;
import code.essence.common.animation.Easy.Direction;
import code.essence.display.screens.clickgui.components.AbstractComponent;
import code.essence.display.screens.clickgui.components.implement.other.SearchComponent;
import code.essence.features.impl.render.Hud;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.geometry.Render2D;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.display.render.post.KawaseBlur;
import code.essence.utils.theme.ThemeManager;
import code.essence.Essence;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class PanelManager extends AbstractComponent {
    private final List<PanelComponent> panels = new ArrayList<>();
    private float basePanelWidth = 110;
    private float basePanelHeight = 290;
    private float baseGap = 10;
    
    private float panelWidth;
    private float panelHeight;
    private float gap;
    
    private float guiOffsetX = 0;
    private float guiOffsetY = -25;
    

    private final SearchComponent searchComponent = new SearchComponent();
    private final List<PanelAnimation> panelAnimations = new ArrayList<>();
    private boolean lastCustomizationVisible = true;
    private boolean lastConfigsVisible = true;

    private static class PanelAnimation {
        PanelComponent panel;
        float targetX, targetY;
        float startX, startY;
        EaseBackIn xAnimation, yAnimation;
        boolean visible;
        boolean animating;

        PanelAnimation(PanelComponent panel, float targetX, float targetY, boolean visible) {
            this.panel = panel;
            this.targetX = targetX;
            this.targetY = targetY;
            this.startX = panel.x;
            this.startY = panel.y;
            this.visible = visible;
            this.animating = false;
            this.xAnimation = new EaseBackIn(200, 1f, 1.5f);
            this.yAnimation = new EaseBackIn(200, 1f, 1.5f);
        }

        void updatePosition(float newTargetX, float newTargetY, boolean newVisible) {
            boolean positionChanged = Math.abs(targetX - newTargetX) > 0.1f || Math.abs(targetY - newTargetY) > 0.1f;
            boolean visibilityChanged = visible != newVisible;

            if (positionChanged || visibilityChanged) {
                startX = panel.x;
                startY = panel.y;
                xAnimation.reset();
                yAnimation.reset();
                xAnimation.setDirection(Direction.FORWARDS);
                yAnimation.setDirection(Direction.FORWARDS);

                targetX = newTargetX;
                targetY = newTargetY;
                visible = newVisible;
                animating = true;
            }
        }

        float getAnimatedX() {
            if (!animating || xAnimation.isDone()) {
                animating = false;
                return targetX;
            }
            double progress = xAnimation.getOutput();
            return (float) (startX + (targetX - startX) * progress);
        }

        float getAnimatedY() {
            if (!animating || yAnimation.isDone()) {
                return targetY;
            }
            double progress = yAnimation.getOutput();
            return (float) (startY + (targetY - startY) * progress);
        }

        boolean isVisible() {
            return visible;
        }
    }

    public PanelManager() {
        initializePanels();
        searchComponent.setOnTextChanged(this::updateSearch);
        panelWidth = basePanelWidth;
        panelHeight = basePanelHeight;
        gap = baseGap;
        
        code.essence.features.impl.render.ClickGui clickGui = code.essence.features.impl.render.ClickGui.getInstance();
        if (clickGui != null) {
            lastCustomizationVisible = clickGui.shouldRenderCustomization();
            lastConfigsVisible = clickGui.shouldRenderConfigs();
        }
    }
    
    private void updatePanelDimensions() {
        panelWidth = basePanelWidth;
        panelHeight = basePanelHeight;
        gap = baseGap;
    }

    private void initializePanels() {
        panels.clear();
        int index = 0;
        for (ModuleCategory category : ModuleCategory.values()) {
            panels.add(new PanelComponent(category, index));
            index++;
        }
    }

    @Override
    public void tick() {
        for (PanelComponent panel : panels) {
            panel.tick();
        }
        searchComponent.tick();
        updatePanelPositions();

        for (PanelAnimation animation : panelAnimations) {
            animation.panel.x = animation.getAnimatedX();
            animation.panel.y = animation.getAnimatedY();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        for (PanelAnimation animation : panelAnimations) {
            if (animation.isVisible()) {
                animation.panel.render(context, mouseX, mouseY, delta);
            }
        }
        float screenWidth = window.getScaledWidth();
        float screenHeight = window.getScaledHeight();
        searchComponent.x = (screenWidth - searchComponent.width) / 2f + guiOffsetX;
        searchComponent.y = (screenHeight - panelHeight) / 2f + panelHeight + 10 + guiOffsetY + 60;
        

        float backgroundWidth = panelWidth + 4;
        float backgroundHeight = searchComponent.height + 10;
        float backgroundX = (screenWidth - backgroundWidth) / 2f + guiOffsetX;
        float backgroundY = searchComponent.y - 5;
        if (Hud.blur.isValue()) {
            Render2D.rectangleWithMask(context.getMatrices().peek().getPositionMatrix(), backgroundX, backgroundY, backgroundWidth, backgroundHeight, 10,0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment());
        }
        rectangle.render(ShapeProperties.create(context.getMatrices(), backgroundX, backgroundY , backgroundWidth, backgroundHeight)
                .round(10f)
                .color(ThemeManager.BackgroundGui.getColor())
                .build());



        String playerName = Essence.getInstance().getNativeUsername();
        String playerUUID = "[" + Essence.getInstance().getNativeUserIdentifier() +"]";
        String displayText = playerName + playerUUID;

        float textWidth = Fonts.getSize(14, Fonts.Type.SuisseIntlMedium).getStringWidth(displayText);
        float secondBackgroundWidth = Math.max(52, textWidth + 16);
        float secondBackgroundHeight = 19;
        float secondBackgroundX = (screenWidth - secondBackgroundWidth) / 2f + guiOffsetX;
        float secondBackgroundY = backgroundY + (backgroundHeight - secondBackgroundHeight) / 2f;
        if (Hud.blur.isValue()) {
            Render2D.rectangleWithMask(context.getMatrices().peek().getPositionMatrix(), secondBackgroundX, secondBackgroundY-30, secondBackgroundWidth-3, secondBackgroundHeight, 8f,0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment());
        }
        rectangle.render(ShapeProperties.create(context.getMatrices(), secondBackgroundX , secondBackgroundY - 30, secondBackgroundWidth -3, secondBackgroundHeight)
                .round(8f)
                .color(ThemeManager.BackgroundGui.getColor())
                .build());
        

        float textX = secondBackgroundX + (secondBackgroundWidth - textWidth) / 2f;
        float textY = secondBackgroundY - 30 + (secondBackgroundHeight - Fonts.getSize(14, Fonts.Type.SuisseIntlMedium).getStringHeight(displayText)) / 2f;
        

        float nameWidth = Fonts.getSize(14, Fonts.Type.SuisseIntlMedium).getStringWidth(playerName);
        Fonts.getSize(14, Fonts.Type.SuisseIntlMedium).drawString(context.getMatrices(), playerName, textX - 2, textY + 6.5, ThemeManager.textColor.getColor());

        float uidX = textX + nameWidth;
        Fonts.getSize(14, Fonts.Type.SuisseIntlMedium).drawRainbowString(context.getMatrices(), playerUUID, uidX, textY+6.5,
            code.essence.utils.display.color.ColorAssist.getClientColor(), 
            code.essence.utils.display.color.ColorAssist.getClientColor2());
        

        float squareSize = 20;
        float squareX = backgroundX + backgroundWidth - squareSize - 5;
        float squareY = backgroundY + (backgroundHeight - squareSize) / 2f;
        
        rectangle.render(ShapeProperties.create(context.getMatrices(), squareX, squareY, squareSize, squareSize)
                .round(4f)
                .color(code.essence.utils.display.color.ColorAssist.getClientColor(), 
                       code.essence.utils.display.color.ColorAssist.getClientColor2(),
                       code.essence.utils.display.color.ColorAssist.getClientColor(), 
                       code.essence.utils.display.color.ColorAssist.getClientColor2())
                .build());
        

        float zX = squareX + (squareSize - Fonts.getSize(16, code.essence.utils.display.render.font.Fonts.Type.ESSENCE).getStringWidth("z")) / 2f;
        float zY = squareY + (squareSize - Fonts.getSize(16, code.essence.utils.display.render.font.Fonts.Type.ESSENCE).getStringHeight("z")) / 2f;
        Fonts.getSize(16, Fonts.Type.ESSENCE).drawString(context.getMatrices(), "z", zX, zY + 6, ThemeManager.textColor.getColor());

        
        searchComponent.render(context, mouseX, mouseY, delta);
    }

    private void updatePanelPositions() {
        float screenWidth = window.getScaledWidth();
        float screenHeight = window.getScaledHeight();

        code.essence.features.impl.render.ClickGui clickGui = code.essence.features.impl.render.ClickGui.getInstance();
        boolean currentCustomizationVisible = clickGui != null && clickGui.shouldRenderCustomization();
        boolean currentConfigsVisible = clickGui != null && clickGui.shouldRenderConfigs();
        
        boolean visibilityChanged = (currentCustomizationVisible != lastCustomizationVisible) || 
                                   (currentConfigsVisible != lastConfigsVisible);
        
        if (visibilityChanged) {
            lastCustomizationVisible = currentCustomizationVisible;
            lastConfigsVisible = currentConfigsVisible;
        }

        List<PanelComponent> visiblePanels = new ArrayList<>();
        for (PanelComponent panel : panels) {
            if (shouldShowPanel(panel)) {
                visiblePanels.add(panel);
            }
        }

        int visiblePanelCount = visiblePanels.size();
        float totalWidth = visiblePanelCount * panelWidth + (visiblePanelCount > 0 ? (visiblePanelCount - 1) * gap : 0);

        float startX = (screenWidth - totalWidth) / 2f + guiOffsetX;
        float startY = (screenHeight - panelHeight) / 2f + guiOffsetY;

        if (!visibilityChanged) {
            if (panelAnimations.size() != panels.size()) {
                visibilityChanged = true;
            } else {
                for (int i = 0; i < panels.size(); i++) {
                    PanelComponent panel = panels.get(i);
                    PanelAnimation animation = panelAnimations.get(i);
                    boolean shouldBeVisible = shouldShowPanel(panel);
                    if (animation.visible != shouldBeVisible) {
                        visibilityChanged = true;
                        break;
                    }
                }
            }
        }

        if (visibilityChanged || panelAnimations.isEmpty()) {
            panelAnimations.clear();

            int visibleIndex = 0;
            for (PanelComponent panel : panels) {
                boolean shouldBeVisible = shouldShowPanel(panel);
                float targetX = shouldBeVisible ? startX + visibleIndex * (panelWidth + gap) : panel.x;
                float targetY = startY;

                if (panelAnimations.isEmpty()) {
                    panel.x = targetX;
                    panel.y = targetY;
                }

                PanelAnimation animation = new PanelAnimation(panel, targetX, targetY, shouldBeVisible);
                panelAnimations.add(animation);

                if (shouldBeVisible) {
                    visibleIndex++;
                }
            }
        } else {
            int visibleIndex = 0;
            for (PanelAnimation animation : panelAnimations) {
                boolean shouldBeVisible = shouldShowPanel(animation.panel);
                float targetX = shouldBeVisible ? startX + visibleIndex * (panelWidth + gap) : animation.targetX;
                float targetY = startY;

                animation.updatePosition(targetX, targetY, shouldBeVisible);

                if (shouldBeVisible) {
                    visibleIndex++;
                }
            }
        }

        for (PanelComponent panel : panels) {
            panel.width = panelWidth;
            panel.height = panelHeight;
        }
    }

    private boolean shouldShowPanel(PanelComponent panel) {
        try {
            code.essence.features.impl.render.ClickGui clickGui = code.essence.features.impl.render.ClickGui.getInstance();
            if (clickGui == null) {
                return true;
            }
            
            ModuleCategory category = panel.getCategory();
            
            if (category == ModuleCategory.CUSTOMIZATION) {
                return clickGui.shouldRenderCustomization();
            }
            if (category == ModuleCategory.CONFIGS) {
                return clickGui.shouldRenderConfigs();
            }
            return true;
        } catch (Exception ignored) {
            return true;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (PanelAnimation animation : panelAnimations) {
            if (animation.isVisible() && animation.panel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        if (searchComponent.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (PanelAnimation animation : panelAnimations) {
            if (animation.isVisible()) {
                animation.panel.mouseReleased(mouseX, mouseY, button);
            }
        }

        searchComponent.mouseReleased(mouseX, mouseY, button);

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        for (PanelAnimation animation : panelAnimations) {
            if (animation.isVisible()) {
                animation.panel.mouseScrolled(mouseX, mouseY, amount);
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        if (keyCode == GLFW.GLFW_KEY_F && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            code.essence.utils.input.InputManager.activateInput(searchComponent);
            return true;
        }
        

        for (PanelComponent panel : panels) {
            if (panel.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        

        float moveSpeed = 10f;
        
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT:
                guiOffsetX -= moveSpeed;
                return true;
            case GLFW.GLFW_KEY_RIGHT:
                guiOffsetX += moveSpeed;
                return true;
            case GLFW.GLFW_KEY_UP:
                guiOffsetY -= moveSpeed;
                return true;
            case GLFW.GLFW_KEY_DOWN:
                guiOffsetY += moveSpeed;
                return true;
        }
        

        if (searchComponent.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (PanelComponent panel : panels) {
            panel.charTyped(chr, modifiers);
        }

        if (searchComponent.charTyped(chr, modifiers)) {
            return true;
        }
        
        return super.charTyped(chr, modifiers);
    }

    public void setSearchText(String text) {
        for (PanelComponent panel : panels) {
            panel.setSearchText(text);
        }
        searchComponent.setText(text);
    }

    public void updateSearch() {
        String searchText = searchComponent.getText();
        for (PanelComponent panel : panels) {
            panel.setSearchText(searchText);
        }
    }

    public List<PanelComponent> getPanels() {
        return panels;
    }

    public void setPanelDimensions(float width, float height, float gap) {
        this.basePanelWidth = width;
        this.basePanelHeight = height;
        this.baseGap = gap;
        updatePanelDimensions();
    }

    public void forceUpdatePositions() {
        panelAnimations.clear();
        updatePanelPositions();
    }
}

