package code.essence.display.screens.clickgui.components.implement.settings;

import code.essence.utils.theme.ThemeManager;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

import code.essence.features.module.setting.implement.TextSetting;
import code.essence.utils.display.render.font.FontRenderer;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.animation.AnimationHelper;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.client.chat.StringHelper;
import code.essence.utils.display.scissor.ScissorAssist;
import code.essence.Essence;

import java.awt.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TextComponent extends AbstractSettingComponent {
    public static boolean typing;
    private final TextSetting setting;
    private boolean wasModuleEnabled = false;
    private float rectX, rectY, rectWidth, rectHeight;
    private boolean dragging;
    private int cursorPosition = 0;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private long lastClickTime = 0;
    private float xOffset = 0;
    private java.lang.String text = "";

    public TextComponent(TextSetting setting) {
        super(setting);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        java.lang.String wrapped = StringHelper.wrap(setting.getDescription(), 70, 12);
        FontRenderer font = Fonts.getSize(13, Fonts.Type.SuisseIntlMedium);
        height = 30;


        this.rectX = x + 5;
        this.rectY = y + 15;
        this.rectWidth = width - 10;
        this.rectHeight = 12.0F;

        rectangle.render(ShapeProperties.create(matrix, rectX-2, rectY-5, rectWidth + 2, rectHeight + 4)
                .round(2)
                .thickness(2)
                .outlineColor(ColorAssist.getOutline())
                .color(ThemeManager.offModuleColor.getColor())
                .build());


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
        Fonts.getSize(13, Fonts.Type.SuisseIntlSemiBold).drawString(context.getMatrices(), setting.getName(), x + 5, y + 3, titleColor);

        updateXOffset(font, cursorPosition);

        ScissorAssist scissor = Essence.getInstance().getScissorManager();
        scissor.push(matrix.peek().getPositionMatrix(), rectX + 1, (float) window.getScaledHeight() / 2 - 96, rectWidth - 3, 220);

        if (typing && selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd) {
            int start = Math.max(0, Math.min(getStartOfSelection(), text.length()));
            int end = Math.max(0, Math.min(getEndOfSelection(), text.length()));
            if (start < end) {
                float selectionXStart = rectX + 3 - xOffset + font.getStringWidth(text.substring(0, start));
                float selectionXEnd = rectX + 3 - xOffset + font.getStringWidth(text.substring(0, end));
                float selectionWidth = selectionXEnd - selectionXStart;

                rectangle.render(ShapeProperties.create(matrix, selectionXStart, rectY + (rectHeight / 2) - 4, selectionWidth, 8).color(0xFF5585E8).build());
            }
        }

        int textColor;
        if (moduleEnabled) {
            textColor =  new java.awt.Color(157, 157, 160, 255).getRGB();
        } else {
            textColor = new java.awt.Color(137, 137, 140, 255).getRGB();
        }
        
        font.drawString(context.getMatrices(), text, rectX + 3 - xOffset, rectY + (rectHeight / 2) - 4.0F, textColor);

        if (!typing && text.isEmpty()) {
            font.drawString(context.getMatrices(), text = setting.getText(), rectX + 3, rectY + (rectHeight / 2) - 4.0F, textColor);
        }

        scissor.pop();
        long currentTime = System.currentTimeMillis();
        boolean focused = typing && (currentTime % 1000 < 500);

        if (focused && (selectionStart == -1 || selectionStart == selectionEnd)) {
            float cursorX = font.getStringWidth(text.substring(0, cursorPosition));
            rectangle.render(ShapeProperties.create(matrix, rectX + 3 - xOffset + cursorX, rectY + (rectHeight / 2) - 6.5F, 0.5F, 7).color(-1).build());
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        dragging = true;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (Calculate.isHovered(mouseX, mouseY, rectX, rectY, rectWidth, rectHeight) && button == 0) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < 250) {
                selectionStart = 0;
                selectionEnd = text.length();
            } else {
                typing = true;
                dragging = true;
                lastClickTime = currentTime;
                cursorPosition = getCursorIndexAt(mouseX);
                selectionStart = cursorPosition;
                selectionEnd = cursorPosition;
            }
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
        if (typing && (text.length() < setting.getMax())) {
            deleteSelectedText();
            text = text.substring(0, cursorPosition) + chr + text.substring(cursorPosition);
            cursorPosition++;
            clearSelection();
        }
        return super.charTyped(chr, modifiers);
    }

    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (typing) {
            if (Screen.hasControlDown()) switch (keyCode) {
                case GLFW.GLFW_KEY_A -> selectAllText();
                case GLFW.GLFW_KEY_V -> pasteFromClipboard();
                case GLFW.GLFW_KEY_C -> copyToClipboard();
            }
            else switch (keyCode) {
                case GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_KEY_ENTER -> handleTextModification(keyCode);
                case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT -> moveCursor(keyCode);
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }


    private void pasteFromClipboard() {
        java.lang.String clipboardText = GLFW.glfwGetClipboardString(window.getHandle());
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
            if (text.length() >= setting.getMin() && text.length() <= setting.getMax()) {
                setting.setText(text);
                typing = false;
            }
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

    
    private void replaceText(int start, int end, java.lang.String replacement) {
        if (start < 0) start = 0;
        if (end > text.length()) end = text.length();
        if (start > end) start = end;

        text = text.substring(0, start) + replacement + text.substring(end);
        cursorPosition = start + replacement.length();
        clearSelection();
    }

    
    private boolean hasSelection() {
        return selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd;
    }


    private java.lang.String getSelectedText() {
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
        FontRenderer font = Fonts.getSize(12, Fonts.Type.SuisseIntlMedium);
        float relativeX = (float) mouseX - rectX - 3 + xOffset;
        int position = 0;
        while (position < text.length()) {
            float textWidth = font.getStringWidth(text.substring(0, position + 1));
            if (textWidth > relativeX) {
                break;
            }
            position++;
        }
        return position;
    }

    
    private void updateXOffset(FontRenderer font, int cursorPosition) {
        float cursorX = font.getStringWidth(text.substring(0, cursorPosition));
        if (cursorX < xOffset) {
            xOffset = cursorX;
        } else if (cursorX - xOffset > rectWidth - 7) {
            xOffset = cursorX - (rectWidth - 7);
        }
    }

    
    private void deleteSelectedText() {
        if (hasSelection()) {
            replaceText(getStartOfSelection(), getEndOfSelection(), "");
        }
    }
}
