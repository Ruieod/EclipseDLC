package code.essence.display.screens.clickgui.components.implement.customization;

import code.essence.utils.animation.AnimationHelper;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.input.InputManager;
import code.essence.utils.theme.ThemeManager;
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

public class ThemeInputComponent extends AbstractComponent implements InputManager.InputComponent, InputManager.ClearableInput {
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
    private long squareClickTime = 0;
    
    private final ThemeDisplayComponent themeDisplay;
    
    public ThemeInputComponent() {
        this.themeDisplay = new ThemeDisplayComponent();
    }

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
        
      

        rectangle.render(ShapeProperties.create(matrix, x , y, width - 16, height)
                .round(6f)
                .softness(1)
                .thickness(2)
                .outlineColor(new Color(47, 47, 50, 255).getRGB())
                .color(ThemeManager.offModuleColor.getColor())
                .build());

        float squareSize = 20;
        float squareX = x + width - squareSize - 5;
        float squareY = y + (height - squareSize) / 2f;
        
        long currentTime = System.currentTimeMillis();
        boolean isClicked = (currentTime - squareClickTime) < 200;
        
        int[] colors;
        if (isClicked) {
            colors = new int[]{-1, -1, -1, 5};
        } else {
            colors = new int[]{ColorAssist.getClientColor(), ColorAssist.getClientColor2(),
                    ColorAssist.getClientColor(), ColorAssist.getClientColor2()};
        }
        
        String buttonAnimationKey = "theme_save_button_" + System.identityHashCode(10);
        float buttonScale = AnimationHelper.getAnimationValue(buttonAnimationKey, 1f);
        
        rectangle.render(ShapeProperties.create(matrix, squareX + 12, squareY, squareSize * buttonScale, squareSize * buttonScale)
                .round(4f)
                .color(colors[0], colors[1], colors[2], colors[3])
                .build());

        float iconX = squareX + (squareSize - Fonts.getSize(18, Fonts.Type.ESSENCE).getStringWidth("y")) / 2f;
        float iconY = squareY + (squareSize - Fonts.getSize(18, Fonts.Type.ESSENCE).getStringHeight("y")) / 2f;
        Fonts.getSize(18, Fonts.Type.ESSENCE).drawString(context.getMatrices(), "y",  iconX+12.3, iconY + 7.6, ThemeManager.textColor.getColor());

        String displayText = text.equalsIgnoreCase("") && !typing ? "New Theme..." : text;
        ScissorAssist scissor = Essence.getInstance().getScissorManager();
        scissor.push(matrix.peek().getPositionMatrix(), x - 1, y, width - 25, height);
        
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
        Fonts.getSize(15, Fonts.Type.SuisseIntlSemiBold).drawString(context.getMatrices(), displayText, textX , y + (height / 2) - 1.0F, typing ? ThemeManager.textColor.getColor(): new Color(137, 137, 140, 255).getRGB());
        scissor.pop();
        
        long cursorTime = System.currentTimeMillis();
        boolean focused = typing && (cursorTime % 1000 < 500);
        if (focused && (selectionStart == -1 || selectionStart == selectionEnd)) {
            FontRenderer textFont = Fonts.getSize(15, Fonts.Type.SuisseIntlSemiBold);
            int safeCursorPosition = Math.min(cursorPosition, text.length());
            float cursorX = textFont.getStringWidth(text.substring(0, safeCursorPosition));
            float leftCursorX = x + 5 + cursorX;
            rectangle.render(ShapeProperties.create(matrix, leftCursorX, y + (height / 2) - 3.5F, 0.5F, 7).color(ThemeManager.textColor.getColor()).build());
        }
        
        if (dragging && GLFW.glfwGetMouseButton(window.getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            cursorPosition = getCursorIndexAt(mouseX);
            if (selectionStart == -1) {
                selectionStart = cursorPosition + 1;
            }
            selectionEnd = cursorPosition;
        } else if (dragging) {
            dragging = false;
        }
        
        themeDisplay.x = x;
        themeDisplay.y = y - 11;
        themeDisplay.width = width;
        themeDisplay.height = 20;
        themeDisplay.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
       
        
        themeDisplay.x = x ;
        themeDisplay.y = y - 11;
        themeDisplay.width = width ;
        themeDisplay.height = 20;
        if (themeDisplay.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        float squareSize = 20;
        float squareX = x + width - squareSize - 12;
        float squareY = y + (height - squareSize) / 2f;
        
        if (Calculate.isHovered(mouseX, mouseY, squareX + 12, squareY, squareSize, squareSize) && button == 0) {
            String buttonAnimationKey = "theme_save_button_" + System.identityHashCode(this);
            AnimationHelper.startAnimation(buttonAnimationKey, 1f, 1.2f, 100, AnimationHelper.EasingType.EASE_OUT);
            
            squareClickTime = System.currentTimeMillis();
            saveTheme();
            return true;
        }
        
        if (Calculate.isHovered(mouseX, mouseY, x, y, width - 25, height) && button == 0) {
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
            return true;
        } else {
            typing = false;
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
        text = text.substring(0, start) + replacement + text.substring(end);
        cursorPosition = start + replacement.length();
        clearSelection();
        notifyTextChanged();
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
    
    private void saveTheme() {
        if (text.isEmpty()) {
            System.out.println("ThemeInputComponent: Cannot save theme - name is empty");
            return;
        }
        
        try {
            code.essence.utils.theme.ThemeManager.saveTheme(text);
            
            text = "";
            cursorPosition = 0;
            clearSelection();
            
        } catch (Exception e) {
            System.out.println("ThemeInputComponent: Error saving theme: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void clearText() {
        text = "";
        cursorPosition = 0;
        clearSelection();
    }
}
