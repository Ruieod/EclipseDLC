package code.essence.display.widgets;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static code.essence.utils.display.interfaces.QuickImports.mc;

public class ClearButtonWidget implements Drawable, Element, Selectable {
    private static final Identifier CLEAR_BUTTON_TEXTURE = Identifier.of("minecraft", "textures/clear_button.png");
    private static final Identifier HOVERED_CLEAR_BUTTON_TEXTURE = Identifier.of("minecraft", "textures/hovered_clear_button.png");
    
    private int x;
    private int y;
    private final int width;
    private final int height;
    private final Runnable onPress;
    private boolean hovered = false;

    public ClearButtonWidget(int x, int y, int width, int height, Runnable onPress) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.onPress = onPress;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        Identifier texture = this.hovered ? HOVERED_CLEAR_BUTTON_TEXTURE : CLEAR_BUTTON_TEXTURE;
        context.drawTexture(RenderLayer::getGuiTextured, texture, this.x, this.y, 0, 0, this.width, this.height, this.width, this.height);
        
        if (this.hovered && mc.currentScreen != null) {
            context.drawTooltip(mc.textRenderer, Text.literal("Очистить инвентарь"), mouseX, mouseY);
        }
        
        
        if (System.currentTimeMillis() % 1000 < 16) {
            System.out.println("[ClearButton] render: x=" + this.x + ", y=" + this.y + ", mouseX=" + mouseX + ", mouseY=" + mouseY + ", hovered=" + this.hovered);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        System.out.println("[ClearButton] mouseClicked: button=" + button + ", hovered=" + isHovered + ", x=" + this.x + ", y=" + this.y + ", width=" + this.width + ", height=" + this.height + ", mouseX=" + mouseX + ", mouseY=" + mouseY);
        if (button == 0 && isHovered) {
            System.out.println("[ClearButton] Кнопка кликнута! Вызываем onPress...");
            this.onPress.run();
            return true;
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public SelectionType getType() {
        return SelectionType.NONE;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
    }
}

