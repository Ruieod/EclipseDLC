package code.essence.display.screens.clickgui.components.implement.other;

import code.essence.utils.theme.ThemeManager;
import code.essence.utils.input.InputManager;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import code.essence.utils.display.render.font.FontRenderer;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.display.scissor.ScissorAssist;
import code.essence.Essence;
import code.essence.display.screens.clickgui.components.AbstractComponent;
import java.awt.*;

public class SearchComponent extends AbstractComponent implements InputManager.InputComponent, InputManager.ClearableInput {
    private boolean typing = false;
    private boolean dragging;
    private int cursorPosition = 0;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private long lastClickTime = 0;
    private float xOffset = 0;
    @Getter
    private String text = "";
    private Runnable onTextChanged;

    public void setText(String text) {
        this.text = text;
        cursorPosition = 0;
        clearSelection();
    }
    
    public void setOnTextChanged(Runnable callback) {
        this.onTextChanged = callback;
    }
    
    @Override
    public void setTyping(boolean typing) {
        this.typing = typing;
    }
    
    @Override
    public boolean isTyping() {
        return typing;
    }
    
    private void notifyTextChanged() {
        if (onTextChanged != null) {
            onTextChanged.run();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(12);
        updateXOffset(font, cursorPosition);
        width = 100;
        height = 21;

        rectangle.render(ShapeProperties.create(matrix, x -2, y, width-20, height)
                .round(6f)
                .softness(1)
                .thickness(2)
                .outlineColor(new Color(47, 47, 50, 255).getRGB())
                .color(ThemeManager.offModuleColor.getColor())
                .build());

        String displayText = text.equalsIgnoreCase("") && !typing ? "Search..." : text;
        ScissorAssist scissor = Essence.getInstance().getScissorManager();
        scissor.push(matrix.peek().getPositionMatrix(), x - 1, y, width - 3, height);
        if (typing && selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd) {
            int start = Math.max(0, Math.min(getStartOfSelection(), text.length()));
            int end = Math.max(0, Math.min(getEndOfSelection(), text.length()));
            if (start < end) {
                float selectionXStart = x + 4 - xOffset + font.getStringWidth(text.substring(0, start));
                float selectionXEnd = x + 4 - xOffset + font.getStringWidth(text.substring(0, end));
                float selectionWidth = selectionXEnd - selectionXStart;
                rectangle.render(ShapeProperties.create(matrix, selectionXStart, y + (height / 2) - 4, selectionWidth, 8).color(0xFF5585E8).build());
            }
        }
        float textX = x + 5;
        Fonts.getSize(15, Fonts.Type.SuisseIntlSemiBold).drawString(context.getMatrices(), displayText, textX -1, y + (height / 2) - 1.0F, typing ? ThemeManager.textColor.getColor() : new Color(137,137,140,255).getRGB());
        scissor.pop();
        long currentTime = System.currentTimeMillis();
        boolean focused = typing && (currentTime % 1000 < 500);
        if (focused && (selectionStart == -1 || selectionStart == selectionEnd)) {
            FontRenderer textFont = Fonts.getSize(15, Fonts.Type.SuisseIntlSemiBold);
            float cursorX = textFont.getStringWidth(text.substring(0, cursorPosition));
            float leftCursorX = x + 5 + cursorX- 1;
            rectangle.render(ShapeProperties.create(matrix, leftCursorX, y + (height / 2) - 3.5F, 0.5F, 7).color(ThemeManager.textColor.getColor()).build());
        }
        if (dragging) {
            cursorPosition = getCursorIndexAt(mouseX);
            if (selectionStart == -1) {
                selectionStart = cursorPosition + 1;
            }
            selectionEnd = cursorPosition;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (Calculate.isHovered(mouseX, mouseY, x, y, width, height) && button == 0) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < 250) {
                selectionStart = 0;
                selectionEnd = text.length();
            } else {
                InputManager.activateInput(this);
                dragging = true;
                lastClickTime = currentTime;
                cursorPosition = getCursorIndexAt(mouseX);
                selectionStart = cursorPosition;
                selectionEnd = cursorPosition;
            }
        } else {
            clearSelection();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (typing) {
            String testText = text.substring(0, cursorPosition) + chr + text.substring(cursorPosition);
            float testWidth = Fonts.getSize(15, Fonts.Type.SuisseIntlSemiBold).getStringWidth(testText);
            float maxWidth = width - 30;
            
            if (testWidth <= maxWidth) {
                deleteSelectedText();
                text = text.substring(0, cursorPosition) + chr + text.substring(cursorPosition);
                cursorPosition++;
                clearSelection();
                notifyTextChanged();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (typing) {
            if (Screen.hasControlDown()) switch (keyCode) {
                case GLFW.GLFW_KEY_A -> selectAllText();
                case GLFW.GLFW_KEY_V -> pasteFromClipboard();
                case GLFW.GLFW_KEY_C -> copyToClipboard();
            } else switch (keyCode) {
                case GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_KEY_ENTER -> handleTextModification(keyCode);
                case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT -> moveCursor(keyCode);
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void pasteFromClipboard() {
        String clipboardText = GLFW.glfwGetClipboardString(window.getHandle());
        if (clipboardText != null) {
            replaceText(cursorPosition, cursorPosition, clipboardText);
        }
    }

    private void copyToClipboard() {
        if (hasSelection()) {
            GLFW.glfwSetClipboardString(window.getHandle(), getSelectedText());
        }
    }

    private void selectAllText() {
        selectionStart = 0;
        selectionEnd = text.length();
    }

    private void handleTextModification(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (hasSelection()) {
                replaceText(getStartOfSelection(), getEndOfSelection(), "");
            } else if (cursorPosition > 0) {
                replaceText(cursorPosition - 1, cursorPosition, "");
            }
        } else if (keyCode == GLFW.GLFW_KEY_ENTER) {
            typing = false;
        }
    }

    private void moveCursor(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_LEFT && cursorPosition > 0) {
            cursorPosition--;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT && cursorPosition < text.length()) {
            cursorPosition++;
        }
        updateSelectionAfterCursorMove();
    }

    private void updateSelectionAfterCursorMove() {
        if (Screen.hasShiftDown()) {
            if (selectionStart == -1) selectionStart = cursorPosition;
            selectionEnd = cursorPosition;
        } else {
            clearSelection();
        }
    }

    private void replaceText(int start, int end, String replacement) {
        if (start < 0) start = 0;
        if (end > text.length()) end = text.length();
        if (start > end) start = end;
        String testText = text.substring(0, start) + replacement + text.substring(end);
        float testWidth = Fonts.getSize(15, Fonts.Type.SuisseIntlSemiBold).getStringWidth(testText);
        float maxWidth = width - 25;
        
        if (testWidth <= maxWidth) {
            text = testText;
            cursorPosition = start + replacement.length();
            clearSelection();
            notifyTextChanged();
        }
    }

    private boolean hasSelection() {
        return selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd;
    }

    private String getSelectedText() {
        return text.substring(getStartOfSelection(), getEndOfSelection());
    }

    private int getStartOfSelection() {
        return Math.min(selectionStart, selectionEnd);
    }

    private int getEndOfSelection() {
        return Math.max(selectionStart, selectionEnd);
    }

    private void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
    }

    private int getCursorIndexAt(double mouseX) {
        FontRenderer font = Fonts.getSize(15, Fonts.Type.SuisseIntlSemiBold);
        float textStartX = x + 5;
        float relativeX = (float) mouseX - textStartX;
        int position = 0;
        while (position < text.length()) {
            float charWidth = font.getStringWidth(text.substring(0, position + 1));
            if (charWidth > relativeX) {
                break;
            }
            position++;
        }
        return position;
    }

    private void updateXOffset(FontRenderer font, int cursorPosition) {
        xOffset = 0;
    }

    private void deleteSelectedText() {
        if (hasSelection()) {
            replaceText(getStartOfSelection(), getEndOfSelection(), "");
        }
    }
    
    @Override
    public void clearText() {
        text = "";
        cursorPosition = 0;
        clearSelection();
        notifyTextChanged();
    }
}