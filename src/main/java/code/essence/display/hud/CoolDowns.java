package code.essence.display.hud;

import code.essence.common.animation.implement.Decelerate;
import code.essence.common.animation.implement.EaseOut;
import code.essence.features.impl.render.Hud;
import code.essence.utils.display.render.post.KawaseBlur;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.registry.Registries;
import code.essence.utils.client.managers.api.draggable.AbstractDraggable;
import code.essence.common.animation.Animation;
import code.essence.common.animation.Direction;
import code.essence.utils.display.render.font.FontRenderer;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.client.Instance;
import code.essence.utils.math.time.StopWatch;
import code.essence.utils.client.chat.StringHelper;
import code.essence.utils.display.render.geometry.Render2D;
import code.essence.events.packet.PacketEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CoolDowns extends AbstractDraggable {
    public static CoolDowns getInstance() {
        return Instance.getDraggable(CoolDowns.class);
    }

    public final List<CoolDown> list = new ArrayList<>();
    private long lastItemChange = 0;
    private int currentItemIndex = 0;
    private static final Item[] EXAMPLE_ITEMS = {
            Items.ENDER_EYE, Items.ENDER_PEARL, Items.SUGAR, Items.MACE, Items.ENCHANTED_GOLDEN_APPLE,
            Items.TRIDENT, Items.CROSSBOW, Items.DRIED_KELP, Items.NETHERITE_SCRAP
    };

    public CoolDowns() {
        super("Cooldowns", 10, 40, 80, 23, true);
        this.scaleAnimation = new EaseOut().setValue(1).setMs(100);
    }

    @Override
    public boolean visible() {
        return !list.isEmpty() || PlayerInteractionHelper.isChat(mc.currentScreen);
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
                selected.remove("Cooldowns");
                Hud.getInstance().interfaceSettings.setSelected(selected);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        list.removeIf(c -> c.anim.isFinished(Direction.BACKWARDS));
        list.stream().filter(c -> !Objects.requireNonNull(mc.player).getItemCooldownManager().isCoolingDown(c.item.getDefaultStack())).forEach(coolDown -> coolDown.anim.setDirection(Direction.BACKWARDS));
        if (list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastItemChange >= 1000) {
                currentItemIndex = (currentItemIndex + 1) % EXAMPLE_ITEMS.length;
                lastItemChange = currentTime;
            }
        }
    }

    @Override
    public void packet(PacketEvent e) {
        if (PlayerInteractionHelper.nullCheck()) return;
        switch (e.getPacket()) {
            case CooldownUpdateS2CPacket c -> {
                Item item = Registries.ITEM.get(c.cooldownGroup());
                list.stream().filter(coolDown -> coolDown.item.equals(item)).forEach(coolDown -> coolDown.anim.setDirection(Direction.BACKWARDS));
                if (c.cooldown() != 0) {
                    list.add(new CoolDown(item, new StopWatch().setMs(-c.cooldown() * 50L), new Decelerate().setMs(150).setValue(1.0F)));
                }
            }
            case PlayerRespawnS2CPacket p -> list.clear();
            default -> {}
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(15, Fonts.Type.SuisseIntlMedium);
        FontRenderer fontCoolDown = Fonts.getSize(13, Fonts.Type.SuisseIntlMedium);
        FontRenderer items = Fonts.getSize(12, Fonts.Type.SuisseIntlMedium);

        long activeCooldowns = list.stream().filter(c -> !c.anim.isFinished(Direction.BACKWARDS)).count();
        java.lang.String cooldownCountText = java.lang.String.valueOf(activeCooldowns);
        float textWidth = items.getStringWidth(cooldownCountText);
        float boxWidth = textWidth + 6;
        if (Hud.blur.isValue()) {
            Render2D.rectangleWithMask(matrix.peek().getPositionMatrix(),  getX(), getY(), getWidth(), getHeight(), 5.5f,0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment());

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
        
        Fonts.getSize(15, Fonts.Type.ESSENCE).drawString(matrix, "t", getX() + 8f, getY() + 9.9f, ThemeManager.textColor.getColor());
        font.drawString(matrix, getName(), getX() + 18, getY() + 9.5f, ThemeManager.textColor.getColor());
        FontRenderer essenceFont = Fonts.getSize(15, Fonts.Type.ESSENCE);
        essenceFont.drawString(matrix, "p", getX() + getWidth() - essenceFont.getStringWidth("p") - 8, getY() + 9.9f, ThemeManager.textColor.getColor());

        float centerX = getX() + getWidth() / 2.0F;
        int offset = 26;
        int maxWidth = 65;

        if (list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            float centerY = getY() + offset;
            Item item = EXAMPLE_ITEMS[currentItemIndex];
            java.lang.String name = "Example";
            java.lang.String duration = "**:**";
            int textColor = ThemeManager.textColor.getColor();
            int textAlpha = 255;
            int colorWithAlpha = ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
            int orangeColor = ColorAssist.getClientColor();
            float durationWidth = fontCoolDown.getStringWidth(duration);
            float durationBoxWidth = durationWidth + 6;
            Calculate.scale(matrix, centerX, centerY, 1, 1, () -> {
                Render2D.defaultDrawStack(context, item.getDefaultStack(), getX() + 7.5f, centerY - 1.5f, false,false, 0.5F);
                rectangle.render(ShapeProperties.create(matrix, getX() + 18F, centerY + 2.5, 2, 2)
                        .round(2 / 2f)
                        .color(new Color(87, 87, 90, 255).getRGB())
                        .build());
                fontCoolDown.drawString(matrix, name, getX() + 22.5f, centerY + 2.5, colorWithAlpha);
                rectangle.render(ShapeProperties.create(matrix, getX() + getWidth() - durationBoxWidth - 8, centerY - 1.5f, durationBoxWidth, 10F)
                        .round(2)
                        .thickness(2)
                        .outlineColor(new Color(33, 33, 33, 255).getRGB())
                        .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(), ColorAssist.getClientColor(), ColorAssist.getClientColor2())
                        .build());
                fontCoolDown.drawString(matrix, duration, getX() + getWidth() - durationWidth - 11, centerY + 2, ThemeManager.textColor.getColor());
            });
            int width = (int) fontCoolDown.getStringWidth(name + duration) + 30;
            maxWidth = Math.max(width, maxWidth);
            offset += 11;
        } else {
            for (CoolDown coolDown : list) {
                float animation = coolDown.anim.getOutput().floatValue();
                float centerY = getY() + offset;
                int time = (int) (-coolDown.time.elapsedTime() / 1000);
                java.lang.String name = coolDown.item.getDefaultStack().getName().getString();
                java.lang.String duration = StringHelper.getDuration(time);
                int textColor = ThemeManager.textColor.getColor();
                int textAlpha = 255;
                int colorWithAlpha = ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
                int orangeColor = ColorAssist.getClientColor();
                float durationWidth = fontCoolDown.getStringWidth(duration);
                float durationBoxWidth = durationWidth + 6;
                Calculate.scale(matrix, centerX, centerY, 1, animation, () -> {
                    Render2D.drawStack(matrix, coolDown.item.getDefaultStack(), getX() + 7.5f, centerY - 1.5f, false, 0.5F);
                    rectangle.render(ShapeProperties.create(matrix, getX() + 18F, centerY + 2.5, 2, 2)
                            .round(2 / 2f)
                            .color(new Color(87, 87, 90, 255).getRGB())
                            .build());
                    fontCoolDown.drawString(matrix, name, getX() + 22.5f, centerY + 2.5, colorWithAlpha);
                    rectangle.render(ShapeProperties.create(matrix, getX() + getWidth() - durationBoxWidth - 8, centerY - 1.5f, durationBoxWidth, 10F)
                            .round(2)
                            .thickness(2)
                            .outlineColor(new Color(33, 33, 33, 255).getRGB())
                            .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(), ColorAssist.getClientColor(), ColorAssist.getClientColor2())
                            .build());
                    fontCoolDown.drawString(matrix, duration, getX() + getWidth() - durationWidth - 11, centerY + 2, ThemeManager.textColor.getColor());
                });
                int width = (int) fontCoolDown.getStringWidth(name + duration) + 30;
                maxWidth = Math.max(width, maxWidth);
                offset += (int) (11 * animation);
            }
        }
        setWidth(maxWidth + 21);
        setHeight(offset + 3);
    }

    public record CoolDown(Item item, StopWatch time, Animation anim) {}
}