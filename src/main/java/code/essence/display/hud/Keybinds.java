package code.essence.display.hud;

import code.essence.common.animation.Direction;
import code.essence.features.impl.render.Hud;
import code.essence.utils.display.render.geometry.Render2D;
import code.essence.utils.display.render.post.KawaseBlur;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import code.essence.utils.client.managers.api.draggable.AbstractDraggable;
import code.essence.features.module.Module;
import code.essence.utils.display.render.font.FontRenderer;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.client.chat.StringHelper;
import code.essence.Essence;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.*;

import code.essence.common.animation.implement.EaseOut;

public class Keybinds extends AbstractDraggable {
    private List<Module> keysList = new ArrayList<>();
    private long lastKeyChange = 0;
    private java.lang.String currentRandomKey = "NONE";

    public Keybinds() {
        super("Keybinds", 300, 40, 80, 23, true);
        this.scaleAnimation = new EaseOut().setValue(1).setMs(100);
    }

    @Override
    public boolean visible() {
        return !keysList.isEmpty() || PlayerInteractionHelper.isChat(mc.currentScreen);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            FontRenderer essenceFont = Fonts.getSize(15, Fonts.Type.ESSENCE);
            float pX = getX() + getWidth() - essenceFont.getStringWidth("p") - 8;
            float pY = getY() + 9.5f;
            float pWidth = essenceFont.getStringWidth("p");
            float pHeight = essenceFont.getStringHeight("p");
            
            if (mouseX >= pX && mouseX <= pX + pWidth && mouseY >= pY && mouseY <= pY + pHeight) {
                java.util.List<String> selected = new java.util.ArrayList<>(Hud.getInstance().interfaceSettings.getSelected());
                selected.remove("Keybinds");
                Hud.getInstance().interfaceSettings.setSelected(selected);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        Essence essence = Essence.getInstance();
        if (essence == null) {
            keysList = new ArrayList<>();
            return;
        }

        code.essence.features.module.ModuleRepository moduleRepository = essence.getModuleRepository();
        if (moduleRepository != null) {
            keysList = moduleRepository.modules().stream()
                    .filter(module -> module.getAnimation().getOutput().floatValue() != 0 && module.getKey() != -1)
                    .filter(module -> !"ClickGui".equals(module.getName())) 
                    .toList();
        } else {
            keysList = new ArrayList<>();
        }
        if (keysList.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastKeyChange >= 1000) {
                List<java.lang.String> availableKeys = List.of("A", "B", "C", "D", "E");
                currentRandomKey = availableKeys.get(new Random().nextInt(availableKeys.size()));
                lastKeyChange = currentTime;
            }
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();

        FontRenderer fontMediuim = Fonts.getSize(13, Fonts.Type.SuisseIntlMedium);
        FontRenderer font = Fonts.getSize(15, Fonts.Type.SuisseIntlMedium);
        FontRenderer fontModule = Fonts.getSize(13, Fonts.Type.SuisseIntlMedium);
        FontRenderer items = Fonts.getSize(12, Fonts.Type.DEFAULT);
        FontRenderer categoryIcon = Fonts.getSize(16, Fonts.Type.ESSENCE);

        long activeModules = keysList.stream().filter(m -> !m.getAnimation().isFinished(Direction.BACKWARDS)).count();
        java.lang.String moduleCountText = java.lang.String.valueOf(activeModules);
        float textWidth = items.getStringWidth(moduleCountText);
        float boxWidth = textWidth + 6;

        if (Hud.blur.isValue()) {
            Render2D.rectangleWithMask(matrix.peek().getPositionMatrix(), getX(), getY(), getWidth(), getHeight(), 5.5f,0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment());
        }
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .round(5.5f)
                .color(ThemeManager.BackgroundGui.getColor())
                .build());
        

        rectangle.render(ShapeProperties.create(matrix, getX() + 3, getY() + 3, getWidth() - 6, 15.5F)
                .round(4f)
                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                .color(ColorAssist.getClientColor(),ColorAssist.getClientColor(), ColorAssist.getClientColor2(),ColorAssist.getClientColor2())
                .build());



      
        


        rectangle.render(ShapeProperties.create(matrix, getX() + 3, getY() + 21.5F, getWidth() - 6, getHeight() - 24.5F)
                .round(4f)
                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                .color(ThemeManager.BackgroundSettings.getColor())
                .build());

        Fonts.getSize(15, Fonts.Type.ESSENCE).drawString(matrix, "q", getX() + 7.6f, getY() + 9.5f, ThemeManager.textColor.getColor());
       
        font.drawString(matrix, getName(), getX() + 18, getY() + 9.5f, ThemeManager.textColor.getColor());
        FontRenderer essenceFont = Fonts.getSize(15, Fonts.Type.ESSENCE);
        essenceFont.drawString(matrix, "p", getX() + getWidth() - essenceFont.getStringWidth("p") - 8, getY() + 9.9f, ThemeManager.textColor.getColor());

        float centerX = getX() + getWidth() / 2F;
        int offset = 26;
        int maxWidth = 65;

        if (keysList.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            float centerY = getY() + offset;
            java.lang.String name = "Module";
            java.lang.String bind = currentRandomKey;
            java.lang.String iconChar = "b";
            int textColor = ColorAssist.getText();
            int textAlpha = 255;
            int colorWithAlpha = ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
            int orangeColor = ColorAssist.getClientColor();
            float bindWidth = fontModule.getStringWidth(bind);
            float bindBoxWidth = bindWidth + 6;
            Calculate.scale(matrix, centerX, centerY, 1, 1, () -> {
                      categoryIcon.drawRainbowString(matrix, iconChar, getX() + 7.5f, centerY + 2.5f, ColorAssist.getClientColor(), ColorAssist.getClientColor2());
                rectangle.render(ShapeProperties.create(matrix, getX() + 18F, centerY + 2.5, 2, 2)
                        .round(2 / 2f)
                        .color(new Color(87, 87, 90, 255).getRGB())
                        .build());
              
                fontMediuim.drawString(matrix, name, getX() + 22, centerY + 2.5, ThemeManager.textColor.getColor());
              
                  
                        rectangle.render(ShapeProperties.create(matrix, getX() + getWidth() - bindBoxWidth - 7.5f, centerY - 1.5f, bindBoxWidth, 10F)
                                .round(2f)
                                        .thickness(2)
                                        .outlineColor(new Color(33, 33, 33, 255).getRGB())
                                .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(), ColorAssist.getClientColor(), ColorAssist.getClientColor2())
                                .build());

               
                Fonts.getSize(13, Fonts.Type.SuisseIntlMedium).drawString(matrix, bind, getX() + getWidth() - bindWidth - 11, centerY + 2, ThemeManager.textColor.getColor());
           
            });
            int width = (int) fontModule.getStringWidth(name + bind) + 25;
            maxWidth = Math.max(width, maxWidth);
            offset += 11;
        } else {
            for (Module module : keysList) {
                java.lang.String bind = StringHelper.getBindName(module.getKey());
                float centerY = getY() + offset;
                float animation = module.getAnimation().getOutput().floatValue();
                java.lang.String iconChar;
                switch (module.getCategory()) {
                    case COMBAT ->iconChar =  "b";
                    case MOVEMENT -> iconChar = "d";
                    case RENDER -> iconChar = "e";
                    case PLAYER -> iconChar = "c";
                    case MISC -> iconChar = "f";


                    default -> iconChar = module.getCategory().getReadableName().substring(0, 1);
                }
                int textColor = ColorAssist.getText();
                int textAlpha = 255;
                int colorWithAlpha = ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
                int orangeColor = ColorAssist.getClientColor();
                float bindWidth = fontModule.getStringWidth(bind);
                float bindBoxWidth = bindWidth + 6;
                Calculate.scale(matrix, centerX, centerY, 1, animation, () -> {
                    categoryIcon.drawRainbowString(matrix, iconChar, getX() + 7.5f, centerY + 2.5f, ColorAssist.getClientColor(), ColorAssist.getClientColor2());
                    
                    rectangle.render(ShapeProperties.create(matrix, getX() + 18F, centerY + 2.5, 2, 2)
                            .round(2 / 2f)
                            .color(new Color(87, 87, 90, 255).getRGB())
                            .build());
                    
                    fontMediuim.drawString(matrix, module.getName(), getX() + 22.5f, centerY + 2, ThemeManager.textColor.getColor());



                    rectangle.render(ShapeProperties.create(matrix, getX() + getWidth() - bindBoxWidth - 7.5f, centerY - 1.5f, bindBoxWidth - 1, 10F)
                            .round(2f)
                                    .thickness(2)
                                    .outlineColor(new Color(33, 33, 33, 255).getRGB())
                            .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(), ColorAssist.getClientColor(), ColorAssist.getClientColor2())
                            .build());
                    Fonts.getSize(13, Fonts.Type.SuisseIntlMedium).drawString(matrix, bind, getX() + getWidth() - bindWidth - 11, centerY + 2, ThemeManager.textColor.getColor());


                });
                float width = fontModule.getStringWidth(module.getName() + bind) + 25;
                maxWidth = (int) Math.max(width, maxWidth);
                offset += (int) (animation * 11);
            }
        }
        setWidth(maxWidth + 21);
        setHeight(offset + 3);
    }
}