package code.essence.display.screens.clickgui.components.implement.window.implement.settings.color;

import code.essence.features.impl.render.Hud;
import code.essence.utils.display.render.geometry.Render2D;
import code.essence.utils.display.render.post.KawaseBlur;
import net.minecraft.client.gui.DrawContext;

import code.essence.features.module.setting.implement.ColorSetting;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.display.screens.clickgui.components.implement.window.AbstractWindow;
import code.essence.display.screens.clickgui.components.implement.window.implement.settings.color.component.*;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.math.calc.Calculate;
import code.essence.display.screens.clickgui.components.AbstractComponent;
import code.essence.utils.theme.ThemeManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ColorWindow extends AbstractWindow {
    private final List<AbstractComponent> components = new ArrayList<>();
    private final ColorSetting setting;
    private int lastColor = -1;

    private final HueComponent hueComponent;
    private final SaturationComponent saturationComponent;
    private final AlphaComponent alphaComponent;
    private final ColorEditorComponent colorEditorComponent;
    private final RGBInputComponent rgbInputComponent;

    public ColorWindow(ColorSetting setting) {
        this.setting = setting;
        this.lastColor = setting.getColor();

        components.addAll(
                Arrays.asList(
                        hueComponent = new HueComponent(setting),
                        saturationComponent = new SaturationComponent(setting),
                        alphaComponent = new AlphaComponent(setting),
                        colorEditorComponent = new ColorEditorComponent(setting),
                        rgbInputComponent = new RGBInputComponent(setting)
                )
        );
    }
    
    @Override
    public void drawWindow(DrawContext context, int mouseX, int mouseY, float delta) {

        if (Hud.blur.isValue()) {
            Render2D.rectangleWithMask(context.getMatrices().peek().getPositionMatrix(), x, y + 10, width, height - 10, 6f,0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment());

        }
        rectangle.render(ShapeProperties.create(context.getMatrices(), x, y + 10, width, height - 10)
                .round(6).thickness(2).softness(1).outlineColor(ColorAssist.getOutline()).color(ThemeManager.BackgroundGui.getColor()).build());

        alphaComponent.position(x, y);
        hueComponent.position(x, y);
        saturationComponent.position(x, y);
        colorEditorComponent.position(x, y);
        rgbInputComponent.position(x, y);

        height = 105;

        components.forEach(component -> component.render(context, mouseX, mouseY, delta));

        if (isThemeColor(setting) && setting.getColor() != lastColor) {
            ThemeManager.saveDefaultTheme();
            lastColor = setting.getColor();
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        draggable(Calculate.isHovered(mouseX, mouseY, x, y, width, 17));
        components.forEach(component -> component.mouseClicked(mouseX, mouseY, button));
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        components.forEach(component -> component.mouseScrolled(mouseX, mouseY, amount));
        return super.mouseScrolled(mouseX, mouseY, amount);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        components.forEach(component -> component.mouseReleased(mouseX, mouseY, button));

        if (isThemeColor(setting) && setting.getColor() != lastColor) {
            ThemeManager.saveDefaultTheme();
            lastColor = setting.getColor();
        }
        
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isThemeColor(ColorSetting setting) {
        return setting == ThemeManager.primaryColor ||
               setting == ThemeManager.secondaryColor ||
               setting == ThemeManager.BackgroundGui ||
               setting == ThemeManager.offModuleColor ||
               setting == ThemeManager.BackgroundSettings ||
               setting == ThemeManager.textColor;
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        components.forEach(component -> component.charTyped(chr, modifiers));
        return super.charTyped(chr, modifiers);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        components.forEach(component -> component.keyPressed(keyCode, scanCode, modifiers));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    public ColorSetting getSetting() {
        return setting;
    }
}
