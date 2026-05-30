package code.essence.display.screens.clickgui.components.implement.window.implement.settings.color.component;

import code.essence.features.module.setting.implement.ColorSetting;
import code.essence.utils.display.render.font.Fonts;
import code.essence.display.screens.clickgui.components.AbstractComponent;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.display.render.shape.implement.Rectangle;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class RGBInputComponent extends AbstractComponent {
    private final ColorSetting setting;
    private final Rectangle rectangle = new Rectangle();

    private final NumberInputField redField;
    private final NumberInputField greenField;
    private final NumberInputField blueField;
    private final NumberInputField alphaField;
    
    public RGBInputComponent(ColorSetting setting) {
        this.setting = setting;

        this.redField = new NumberInputField(0, 255);
        this.greenField = new NumberInputField(0, 255);
        this.blueField = new NumberInputField(0, 255);
        this.alphaField = new NumberInputField(0, 255);

        updateFieldsFromColor();
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        if (!redField.isTyping() && !greenField.isTyping() && !blueField.isTyping() && !alphaField.isTyping()) {
            updateFieldsFromColor();
        }
        

        float fieldY = y + 90;
        float fieldWidth = 20;
        float fieldHeight = 10;
        float fieldSpacing = 2;
        
         renderInputField(context, matrix, redField, x + 6, fieldY, fieldWidth, fieldHeight);

        renderInputField(context, matrix, greenField, x + 6 + fieldWidth + fieldSpacing, fieldY, fieldWidth, fieldHeight);

        renderInputField(context, matrix, blueField, x + 6 + (fieldWidth + fieldSpacing) * 2, fieldY, fieldWidth, fieldHeight);

        renderInputField(context, matrix, alphaField, x + 6 + (fieldWidth + fieldSpacing) * 3, fieldY, fieldWidth, fieldHeight);
    }
    
    private void renderInputField(DrawContext context, MatrixStack matrix, NumberInputField field, 
                                 float x, float y, float width, float height) {

        rectangle.render(ShapeProperties.create(matrix, x, y, width, height)
                .round(2)
                .color(ThemeManager.BackgroundSettings.getColor())
                .thickness(1)
                .outlineColor(field.isTyping() ? new Color(255, 101, 57, 255).getRGB() : new Color(47, 47, 50, 255).getRGB())
                .build());
        String displayText = field.isTyping() ? field.getText() : String.valueOf(field.getValue());
        float textWidth = Fonts.getSize(11, Fonts.Type.SuisseIntlSemiBold).getStringWidth(displayText);
        float textX = x + (width - textWidth) / 2f;
        float textY = y + (height - Fonts.getSize(11, Fonts.Type.SuisseIntlSemiBold).getStringHeight(displayText)) / 2f;
        
        Fonts.getSize(11, Fonts.Type.SuisseIntlSemiBold).drawString(matrix, displayText, textX, textY +5.5, ThemeManager.textColor.getColor());
        if (field.isTyping() && (System.currentTimeMillis() % 1000 < 500)) {
            float cursorX = textX + textWidth;
            rectangle.render(ShapeProperties.create(matrix, cursorX, y + 1, 1, height - 2)
                    .color(-1)
                    .build());
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            float fieldY = y + 90;
            float fieldWidth = 20;
            float fieldHeight = 10;
            float fieldSpacing = 2;
            if (Calculate.isHovered(mouseX, mouseY, x + 6, fieldY, fieldWidth, fieldHeight)) {
                redField.setTyping(true);
                greenField.setTyping(false);
                blueField.setTyping(false);
                alphaField.setTyping(false);
                return true;
            }
            
            if (Calculate.isHovered(mouseX, mouseY, x + 6 + fieldWidth + fieldSpacing, fieldY, fieldWidth, fieldHeight)) {
                redField.setTyping(false);
                greenField.setTyping(true);
                blueField.setTyping(false);
                alphaField.setTyping(false);
                return true;
            }
            
            if (Calculate.isHovered(mouseX, mouseY, x + 6 + (fieldWidth + fieldSpacing) * 2, fieldY, fieldWidth, fieldHeight)) {
                redField.setTyping(false);
                greenField.setTyping(false);
                blueField.setTyping(true);
                alphaField.setTyping(false);
                return true;
            }
            
            if (Calculate.isHovered(mouseX, mouseY, x + 6 + (fieldWidth + fieldSpacing) * 3, fieldY, fieldWidth, fieldHeight)) {
                redField.setTyping(false);
                greenField.setTyping(false);
                blueField.setTyping(false);
                alphaField.setTyping(true);
                return true;
            }
            redField.setTyping(false);
            greenField.setTyping(false);
            blueField.setTyping(false);
            alphaField.setTyping(false);
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (redField.isTyping()) {
            return redField.charTyped(chr, modifiers);
        } else if (greenField.isTyping()) {
            return greenField.charTyped(chr, modifiers);
        } else if (blueField.isTyping()) {
            return blueField.charTyped(chr, modifiers);
        } else if (alphaField.isTyping()) {
            return alphaField.charTyped(chr, modifiers);
        }
        return false;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (redField.isTyping()) {
            if (redField.keyPressed(keyCode, scanCode, modifiers)) {
                updateColorFromFields();
                return true;
            }
        } else if (greenField.isTyping()) {
            if (greenField.keyPressed(keyCode, scanCode, modifiers)) {
                updateColorFromFields();
                return true;
            }
        } else if (blueField.isTyping()) {
            if (blueField.keyPressed(keyCode, scanCode, modifiers)) {
                updateColorFromFields();
                return true;
            }
        } else if (alphaField.isTyping()) {
            if (alphaField.keyPressed(keyCode, scanCode, modifiers)) {
                updateColorFromFields();
                return true;
            }
        }
        return false;
    }
    
    private void updateColorFromFields() {
        int r = redField.getValue();
        int g = greenField.getValue();
        int b = blueField.getValue();
        int a = alphaField.getValue();
        
        Color newColor = new Color(r, g, b, a);
        setting.setColor(newColor.getRGB());
    }
    
    private void updateFieldsFromColor() {
        Color currentColor = new Color(setting.getColor(), true);
        redField.setValue(currentColor.getRed());
        greenField.setValue(currentColor.getGreen());
        blueField.setValue(currentColor.getBlue());
        alphaField.setValue(currentColor.getAlpha());
    }

    private static class NumberInputField {
        private final int minValue;
        private final int maxValue;
        private int value;
        private String text = "";
        private boolean typing = false;
        
        public NumberInputField(int minValue, int maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.value = minValue;
        }
        
        public boolean isTyping() {
            return typing;
        }
        
        public void setTyping(boolean typing) {
            this.typing = typing;
            if (!typing) {
                try {
                    int newValue = Integer.parseInt(text.isEmpty() ? "0" : text);
                    this.value = Math.max(minValue, Math.min(maxValue, newValue));
                    this.text = String.valueOf(this.value);
                } catch (NumberFormatException e) {
                    this.text = String.valueOf(this.value);
                }
            } else {
                this.text = String.valueOf(this.value);
            }
        }
        
        public int getValue() {
            return value;
        }
        
        public void setValue(int value) {
            this.value = Math.max(minValue, Math.min(maxValue, value));
            this.text = String.valueOf(this.value);
        }
        
        public String getText() {
            return text;
        }
        
        public boolean charTyped(char chr, int modifiers) {
            if (!typing) return false;
            
            if (chr >= '0' && chr <= '9') {
                String newText = text + chr;
                try {
                    int newValue = Integer.parseInt(newText);
                    if (newValue <= maxValue) {
                        text = newText;
                        return true;
                    }
                } catch (NumberFormatException e) {
                }
            }
            return false;
        }
        
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!typing) return false;
            
            switch (keyCode) {
                case GLFW.GLFW_KEY_BACKSPACE:
                    if (!text.isEmpty()) {
                        text = text.substring(0, text.length() - 1);
                        return true;
                    }
                    break;
                case GLFW.GLFW_KEY_ENTER:
                    typing = false;
                    try {
                        int newValue = Integer.parseInt(text.isEmpty() ? "0" : text);
                        this.value = Math.max(minValue, Math.min(maxValue, newValue));
                        this.text = String.valueOf(this.value);
                    } catch (NumberFormatException e) {
                        this.text = String.valueOf(this.value);
                    }
                    return true;
                case GLFW.GLFW_KEY_ESCAPE:
                    typing = false;
                    this.text = String.valueOf(this.value);
                    return true;
            }
            return false;
        }
    }
}
