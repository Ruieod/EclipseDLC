package code.essence.display.screens.clickgui;

import code.essence.display.screens.clickgui.components.implement.autobuy.autobuyui.AutoBuyGuiComponent;
import code.essence.features.impl.render.ClickGui;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import code.essence.features.module.ModuleCategory;
import code.essence.common.animation.Easy.EaseBackIn;
import code.essence.common.animation.Easy.Direction;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.display.screens.clickgui.components.AbstractComponent;
import code.essence.display.screens.clickgui.components.implement.panel.PanelManager;
import code.essence.display.screens.clickgui.components.implement.settings.TextComponent;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.input.InputManager;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


@Setter
@Getter
public class MenuScreen extends Screen implements QuickImports {
    public static MenuScreen INSTANCE = new MenuScreen();
    private final List<AbstractComponent> components = new ArrayList<>();
    private final PanelManager panelManager = new PanelManager();
    private final AutoBuyGuiComponent autoBuyGuiComponent = new AutoBuyGuiComponent();
    public final EaseBackIn animation = new EaseBackIn(400 / 2, 1f, 1.5f);
    public ModuleCategory category = ModuleCategory.COMBAT;
    public int x, y, width, height;

    private final Random random = new Random();
    private static final int MAX_SNOWFLAKES = 300;
    private static final float SPAWN_RATE = 1.0f;

    private boolean resizeModeActive = false;
    private long lastClickTime = 0;
    private static final long DOUBLE_CLICK_DELAY_MS = 300;
    private float scaleFactor = 1.0f; 

    public void initialize() {
        
        components.addAll(Arrays.asList(panelManager));
    }

    public MenuScreen() {
        super(Text.of("MenuScreen"));
        initialize();
    }

    @Override
    public void tick() {
        close();
        components.forEach(AbstractComponent::tick);
        super.tick();
    }

    private int lastMouseX = 0;
    private int lastMouseY = 0;



    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        rectangle.render(ShapeProperties.create(context.getMatrices(), 0, 0, window.getScaledWidth(), window.getScaledHeight())
                .color(Calculate.applyOpacity(0xFF000000, 100 * getScaleAnimation()))
                .build());
        
        float centerX = window.getScaledWidth() / 2f;
        float centerY = window.getScaledHeight() / 2f;
        float scale = getScaleAnimation() * scaleFactor;
        
        double transformedMouseX = (mouseX - centerX) / scale + centerX;
        double transformedMouseY = (mouseY - centerY) / scale + centerY;
        
        context.getMatrices().push();
        context.getMatrices().translate(centerX, centerY, 0);
        context.getMatrices().scale(scale, scale, 1);
        context.getMatrices().translate(-centerX, -centerY, 0);
        
        Calculate.setAlpha(getScaleAnimation(), () -> {
            components.forEach(component -> component.render(context, (int)transformedMouseX, (int)transformedMouseY, delta));
            windowManager.render(context, (int)transformedMouseX, (int)transformedMouseY, delta);
        });
        
        context.getMatrices().pop();

        if (ClickGui.getInstance() != null && ClickGui.getInstance().shouldShowModuleDescriptions()) {
            renderModuleDescriptions(context, mouseX, mouseY);
        }


    }

    private String currentHoveredModule = null;
    private code.essence.display.screens.clickgui.components.implement.module.ModuleComponent currentHoveredComponent = null;
    private float lastModuleX = 0;
    private float lastModuleY = 0;
    private float lastModuleWidth = 0;
    private String lastSearchText = "";

    private void renderModuleDescriptions(DrawContext context, int mouseX, int mouseY) {
        if (context == null || window == null) return;

        try {
            float centerX = window.getScaledWidth() / 2f;
            float centerY = window.getScaledHeight() / 2f;
            float scale = getScaleAnimation() * scaleFactor;

            if (scale <= 0) return;

            
            String currentSearchText = "";
            try {
                if (panelManager != null) {
                    java.lang.reflect.Field searchComponentField = panelManager.getClass().getDeclaredField("searchComponent");
                    searchComponentField.setAccessible(true);
                    Object searchComponent = searchComponentField.get(panelManager);
                    if (searchComponent != null) {
                        java.lang.reflect.Method getTextMethod = searchComponent.getClass().getMethod("getText");
                        Object text = getTextMethod.invoke(searchComponent);
                        if (text != null) {
                            currentSearchText = text.toString();
                        }
                    }
                }
            } catch (Exception e) {
                
            }
            
            
            if (!currentSearchText.equals(lastSearchText)) {
                if (currentHoveredModule != null) {
                    code.essence.utils.animation.AnimationHelper.stopAnimation("module_description_alpha_" + currentHoveredModule);
                    currentHoveredModule = null;
                    currentHoveredComponent = null;
                }
                lastSearchText = currentSearchText;
            }

            double transformedMouseX = (mouseX - centerX) / scale + centerX;
            double transformedMouseY = (mouseY - centerY) / scale + centerY;

            String hoveredModuleName = null;
            code.essence.display.screens.clickgui.components.implement.module.ModuleComponent hoveredComponent = null;
            float hoveredModuleX = 0;
            float hoveredModuleY = 0;
            float hoveredModuleWidth = 0;
            boolean isMouseOverAnyPanel = false;

            for (AbstractComponent component : components) {
                if (component == null) continue;

                if (component instanceof code.essence.display.screens.clickgui.components.implement.panel.PanelManager) {
                    code.essence.display.screens.clickgui.components.implement.panel.PanelManager panelManager =
                            (code.essence.display.screens.clickgui.components.implement.panel.PanelManager) component;

                    if (panelManager == null) continue;

                    java.util.List<code.essence.display.screens.clickgui.components.implement.panel.PanelComponent> panels = panelManager.getPanels();
                    if (panels == null) continue;

                    for (code.essence.display.screens.clickgui.components.implement.panel.PanelComponent panel : panels) {
                        if (panel == null) continue;

                        boolean isMouseOverPanel = code.essence.utils.math.calc.Calculate.isHovered(
                                transformedMouseX, transformedMouseY,
                                panel.x, panel.y,
                                panel.width, panel.height);

                        if (isMouseOverPanel) {
                            isMouseOverAnyPanel = true;
                        }

                        java.util.List<code.essence.display.screens.clickgui.components.implement.module.ModuleComponent> moduleComponents = panel.getModuleComponents();
                        if (moduleComponents == null) continue;

                        for (code.essence.display.screens.clickgui.components.implement.module.ModuleComponent moduleComponent : moduleComponents) {
                            if (moduleComponent == null) continue;

                            try {
                                
                                boolean matchesSearch = true;
                                try {
                                    java.lang.reflect.Field searchTextField = panel.getClass().getDeclaredField("searchText");
                                    searchTextField.setAccessible(true);
                                    String panelSearchText = (String) searchTextField.get(panel);
                                    
                                    if (panelSearchText != null && !panelSearchText.isEmpty()) {
                                        matchesSearch = moduleComponent.getModule().getVisibleName().toLowerCase().contains(panelSearchText.toLowerCase());
                                    }
                                } catch (Exception ignored) {}
                                
                                if (!matchesSearch) {
                                    continue;
                                }
                                
                                
                                float moduleHeaderY = moduleComponent.y;
                                float moduleHeaderBottom = moduleComponent.y + 20; 
                                float panelClipTop = panel.y + 35 - 8 + 22 + 4; 
                                float panelClipBottom = panel.y + panel.height;
                                
                                boolean isHeaderVisible = moduleHeaderBottom >= panelClipTop && moduleHeaderY <= panelClipBottom;

                                if (!isHeaderVisible) {
                                    continue;
                                }

                                
                                int headerHeight = 20;
                                boolean isHovered = code.essence.utils.math.calc.Calculate.isHovered(
                                        transformedMouseX, transformedMouseY,
                                        moduleComponent.x, moduleComponent.y,
                                        moduleComponent.width, headerHeight);

                                if (isHovered) {
                                    hoveredModuleName = moduleComponent.getModule().getName();
                                    hoveredComponent = moduleComponent;

                                    hoveredModuleX = moduleComponent.x;
                                    hoveredModuleY = moduleComponent.y;
                                    hoveredModuleWidth = moduleComponent.width;
                                    break;
                                }
                            } catch (Exception e) {
                                continue;
                            }
                        }
                    }
                }
            }

            if (hoveredModuleName == null && currentHoveredModule != null) {
                
                boolean isCurrentModuleVisible = false;
                boolean isCurrentModuleExists = false;
                
                if (currentHoveredComponent != null) {
                    for (AbstractComponent component : components) {
                        if (component instanceof code.essence.display.screens.clickgui.components.implement.panel.PanelManager) {
                            code.essence.display.screens.clickgui.components.implement.panel.PanelManager panelManager =
                                    (code.essence.display.screens.clickgui.components.implement.panel.PanelManager) component;
                            if (panelManager == null) continue;
                            
                            java.util.List<code.essence.display.screens.clickgui.components.implement.panel.PanelComponent> panels = panelManager.getPanels();
                            if (panels == null) continue;
                            
                            for (code.essence.display.screens.clickgui.components.implement.panel.PanelComponent panel : panels) {
                                if (panel == null) continue;
                                
                                java.util.List<code.essence.display.screens.clickgui.components.implement.module.ModuleComponent> moduleComponents = panel.getModuleComponents();
                                if (moduleComponents == null) continue;
                                
                                
                                for (code.essence.display.screens.clickgui.components.implement.module.ModuleComponent moduleComponent : moduleComponents) {
                                    if (moduleComponent != null && moduleComponent.getModule() != null && 
                                        moduleComponent.getModule().getName().equals(currentHoveredModule)) {
                                        
                                        
                                        
                                        boolean matchesSearch = true;
                                        try {
                                            java.lang.reflect.Field searchTextField = panel.getClass().getDeclaredField("searchText");
                                            searchTextField.setAccessible(true);
                                            String searchText = (String) searchTextField.get(panel);
                                            
                                            if (searchText != null && !searchText.isEmpty()) {
                                                matchesSearch = moduleComponent.getModule().getVisibleName().toLowerCase().contains(searchText.toLowerCase());
                                            }
                                        } catch (Exception e) {
                                            
                                        }
                                        
                                        if (matchesSearch) {
                                            isCurrentModuleExists = true;
                                            
                                            
                                            float moduleHeaderY = moduleComponent.y;
                                            float moduleHeaderBottom = moduleComponent.y + 20; 
                                            float panelClipTop = panel.y + 35 - 8 + 22 + 4; 
                                            float panelClipBottom = panel.y + panel.height;
                                            
                                            boolean isHeaderVisible = moduleHeaderBottom >= panelClipTop && moduleHeaderY <= panelClipBottom;
                                            
                                            if (isHeaderVisible) {
                                                isCurrentModuleVisible = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (isCurrentModuleVisible) break;
                            }
                            if (isCurrentModuleVisible) break;
                        }
                    }
                }
                
                
                if (!isMouseOverAnyPanel || !isCurrentModuleVisible || !isCurrentModuleExists) {
                    code.essence.utils.animation.AnimationHelper.stopAnimation("module_description_alpha_" + currentHoveredModule);
                    currentHoveredModule = null;
                    currentHoveredComponent = null;
                } else {
                    String fadeOutKey = "module_description_alpha_" + currentHoveredModule;
                    boolean isActive = code.essence.utils.animation.AnimationHelper.isAnimationActive(fadeOutKey);

                    if (isActive) {
                        float alpha = code.essence.utils.animation.AnimationHelper.getAnimationValue(fadeOutKey, 0f);
                        if (alpha > 0f && currentHoveredComponent != null) {
                            float transformedModuleX = (lastModuleX - centerX) * scale + centerX;
                            float transformedModuleY = (lastModuleY - centerY) * scale + centerY;
                            currentHoveredComponent.renderModuleDescriptionAt(context, transformedModuleX, transformedModuleY, lastModuleWidth, false);
                        } else {
                            currentHoveredModule = null;
                            currentHoveredComponent = null;
                        }
                    } else {
                        code.essence.utils.animation.AnimationHelper.fadeOut(fadeOutKey, 300);
                        if (currentHoveredComponent != null) {
                            float transformedModuleX = (lastModuleX - centerX) * scale + centerX;
                            float transformedModuleY = (lastModuleY - centerY) * scale + centerY;
                            currentHoveredComponent.renderModuleDescriptionAt(context, transformedModuleX, transformedModuleY, lastModuleWidth, false);
                        }
                    }
                }
            } else if (hoveredModuleName != null) {
                String hoveredAnimationKey = "module_description_alpha_" + hoveredModuleName;
                boolean hoveredAnimationActive = code.essence.utils.animation.AnimationHelper.isAnimationActive(hoveredAnimationKey);
                float hoveredAlpha = code.essence.utils.animation.AnimationHelper.getAnimationValue(hoveredAnimationKey, 0f);

                if (!hoveredModuleName.equals(currentHoveredModule)) {
                    if (currentHoveredModule != null) {
                        code.essence.utils.animation.AnimationHelper.fadeOut("module_description_alpha_" + currentHoveredModule, 300);
                    }

                    if (hoveredAnimationActive && hoveredAlpha <= 0.01f) {
                        code.essence.utils.animation.AnimationHelper.stopAnimation(hoveredAnimationKey);
                        code.essence.utils.animation.AnimationHelper.fadeIn(hoveredAnimationKey, 400);
                    } else if (!hoveredAnimationActive || hoveredAlpha < 0.99f) {
                        code.essence.utils.animation.AnimationHelper.fadeIn(hoveredAnimationKey, 400);
                    }

                    currentHoveredModule = hoveredModuleName;
                    currentHoveredComponent = hoveredComponent;
                }

                lastModuleX = hoveredModuleX;
                lastModuleY = hoveredModuleY;
                lastModuleWidth = hoveredModuleWidth;

                float transformedModuleX = (hoveredModuleX - centerX) * scale + centerX;
                float transformedModuleY = (hoveredModuleY - centerY) * scale + centerY;
                hoveredComponent.renderModuleDescriptionAt(context, transformedModuleX, transformedModuleY, hoveredModuleWidth, true);
            }
        } catch (Exception e) {
            
        }
    }




    private void renderTopImages(DrawContext context) {
        try {
            MatrixStack matrix = context.getMatrices();
            matrix.push();

            float centerX = window.getScaledWidth() / 2f;
            float centerY = window.getScaledHeight() / 2f;
            float scale = getScaleAnimation() * scaleFactor;

            matrix.push();
            matrix.translate(centerX, centerY, 0);
            matrix.scale(scale, scale, 1);
            matrix.translate(-centerX, -centerY, 0);

            Calculate.setAlpha(getScaleAnimation(), () -> {
                float[][] imageOffsets = {
                        {363, -40},
                        {-131, -45},
                        {60, -16}, {160, -45},
                        {0,0},
                        {0, 0}
                };
                float[] imageSizes = {
                        100,
                        109,
                        36,
                        106,
                        0,
                        0};
                String[] imageTextures = {
                        "minecraft:textures/1.png", "minecraft:textures/2.png", "minecraft:textures/3.png",
                        "minecraft:textures/4.png", "", ""
                };
                List<code.essence.display.screens.clickgui.components.implement.panel.PanelComponent> panels = panelManager.getPanels();

                for (int i = 0; i < 6; i++) {
                    if (i == 4 || imageSizes[i] <= 0) continue;

                    float categoryX = 0, categoryY = 0;
                    if (i < panels.size()) {
                        code.essence.display.screens.clickgui.components.implement.panel.PanelComponent panel = panels.get(i);
                        categoryX = panel.x + 0.5f;
                        categoryY = panel.y + 34f;
                    }

                    matrix.push();
                    matrix.translate(0, 0, 2000);
                    image.setTexture(imageTextures[i])
                         .render(ShapeProperties.create(matrix, categoryX + imageOffsets[i][0], categoryY + imageOffsets[i][1], imageSizes[i], imageSizes[i])
                             .color(0xFFFFFFFF)
                             .build());
                    matrix.pop();
                }
            });
            matrix.pop();
        } catch (Exception ignored) {}
    }


    public void openGui() {
        panelManager.forceUpdatePositions();
        animation.setDirection(Direction.FORWARDS);
        mc.setScreen(this);
    }


    public float getScaleAnimation() {
        return (float) animation.getOutput();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float centerX = window.getScaledWidth() / 2f;
        float centerY = window.getScaledHeight() / 2f;
        float scale = getScaleAnimation() * scaleFactor;
        
        double transformedX = (mouseX - centerX) / scale + centerX;
        double transformedY = (mouseY - centerY) / scale + centerY;
        
        if (!windowManager.mouseClicked(transformedX, transformedY, button)) {
            components.forEach(component -> component.mouseClicked(transformedX, transformedY, button));
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        float centerX = window.getScaledWidth() / 2f;
        float centerY = window.getScaledHeight() / 2f;
        float scale = getScaleAnimation() * scaleFactor;
        
        double transformedX = (mouseX - centerX) / scale + centerX;
        double transformedY = (mouseY - centerY) / scale + centerY;
        
        components.forEach(component -> component.mouseReleased(transformedX, transformedY, button));
        windowManager.mouseReleased(transformedX, transformedY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        float centerX = window.getScaledWidth() / 2f;
        float centerY = window.getScaledHeight() / 2f;
        float scale = getScaleAnimation() * scaleFactor;
        
        double transformedX = (mouseX - centerX) / scale + centerX;
        double transformedY = (mouseY - centerY) / scale + centerY;
        double transformedDeltaX = deltaX / scale;
        double transformedDeltaY = deltaY / scale;
        
        if (!windowManager.mouseDragged(transformedX, transformedY, button, transformedDeltaX, transformedDeltaY)) {
            components.forEach(component -> component.mouseDragged(transformedX, transformedY, button, transformedDeltaX, transformedDeltaY));
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (resizeModeActive) {
            float[] levels = {0.5f, 0.7f, 0.85f, 1.0f, 1.3333f, 1.7f}; 
            int currentLevel = 3; 
            
            for (int i = 0; i < levels.length; i++) {
                if (Math.abs(scaleFactor - levels[i]) < 0.05f) {
                    currentLevel = i;
                    break;
                }
            }
            
            int newLevel = currentLevel + (vertical > 0 ? 1 : -1);
            newLevel = Math.max(0, Math.min(5, newLevel));
            
            if (newLevel != currentLevel) {
                scaleFactor = levels[newLevel];
                return true;
            }
        }
        
        float centerX = window.getScaledWidth() / 2f;
        float centerY = window.getScaledHeight() / 2f;
        float scale = getScaleAnimation() * scaleFactor;
        
        double transformedX = (mouseX - centerX) / scale + centerX;
        double transformedY = (mouseY - centerY) / scale + centerY;
        
        if (!windowManager.mouseScrolled(transformedX, transformedY, vertical)) {
            components.forEach(component -> component.mouseScrolled(transformedX, transformedY, vertical));
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && shouldCloseOnEsc()) {
          
            animation.setDirection(Direction.BACKWARDS);
            return true;
        }
        if (!windowManager.keyPressed(keyCode, scanCode, modifiers)) {
            components.forEach(component -> component.keyPressed(keyCode, scanCode, modifiers));
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!windowManager.charTyped(chr, modifiers)) {
            components.forEach(component -> component.charTyped(chr, modifiers));
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (animation.finished(Direction.BACKWARDS)) {
            TextComponent.typing = false;
            InputManager.clearAllInputs();
            super.close();
        }
    }

}