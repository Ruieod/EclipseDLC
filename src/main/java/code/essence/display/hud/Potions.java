package code.essence.display.hud;

import code.essence.common.animation.implement.Decelerate;
import code.essence.common.animation.implement.EaseOut;
import code.essence.features.impl.render.Hud;
import code.essence.utils.display.render.post.KawaseBlur;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Formatting;
import code.essence.utils.client.managers.api.draggable.AbstractDraggable;
import code.essence.common.animation.Animation;
import code.essence.common.animation.Direction;
import code.essence.utils.display.render.font.FontRenderer;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.display.render.geometry.Render2D;
import code.essence.events.packet.PacketEvent;
import code.essence.utils.client.sound.SoundManager;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import java.awt.*;

public class Potions extends AbstractDraggable {
    private final List<Potion> list = new ArrayList<>();
    private static final RegistryEntry<StatusEffect>[] NEGATIVE_EFFECTS = new RegistryEntry[] {
            StatusEffects.POISON, StatusEffects.WITHER, StatusEffects.NAUSEA, StatusEffects.BLINDNESS,
            StatusEffects.HUNGER, StatusEffects.SLOWNESS, StatusEffects.MINING_FATIGUE, StatusEffects.INSTANT_DAMAGE,
            StatusEffects.WEAKNESS, StatusEffects.LEVITATION, StatusEffects.UNLUCK, StatusEffects.BAD_OMEN
    };
    private long lastEffectChange = 0;
    private RegistryEntry<StatusEffect> currentRandomEffect = StatusEffects.SPEED;
    
    private final Map<String, StatusEffectInstance> activeEffects = new HashMap<>();
    private final Map<String, Boolean> warnedEffects = new HashMap<>();

    public Potions() {
        super("Potions", 200, 40, 80, 23, true);
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
               List<String> selected = new ArrayList<>(Hud.getInstance().interfaceSettings.getSelected());
                selected.remove("Potions");
                Hud.getInstance().interfaceSettings.setSelected(selected);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        list.removeIf(p -> p.anim.isFinished(Direction.BACKWARDS));
        list.forEach(p -> p.effect.update(mc.player, null));
        
        if (Hud.getInstance().notificationSettings.isSelected("Заканчивающемся зелье") && !PlayerInteractionHelper.nullCheck()) {
            List<String> effectsToRemove = new ArrayList<>();
            for (Map.Entry<String, StatusEffectInstance> entry : activeEffects.entrySet()) {
                String effectKey = entry.getKey();
                StatusEffectInstance trackedEffect = entry.getValue();
                
                boolean stillActive = false;
                if (mc.player != null) {
                    for (StatusEffectInstance activeEffect : mc.player.getStatusEffects()) {
                        if (activeEffect.getEffectType().getIdAsString().equals(trackedEffect.getEffectType().getIdAsString())) {
                            stillActive = true;
                            activeEffects.put(effectKey, activeEffect);
                            
                            int remainingTicks = activeEffect.getDuration();
                            int warningTicks = 200;
                            boolean alreadyWarned = warnedEffects.getOrDefault(effectKey, false);
                            
                            if (remainingTicks <= warningTicks && remainingTicks > 0 && !alreadyWarned) {
                                sendPotionEndingSoonNotification(activeEffect);
                                warnedEffects.put(effectKey, true);
                            }
                            
                            if (remainingTicks > warningTicks) {
                                warnedEffects.remove(effectKey);
                            }
                            
                            break;
                        }
                    }
                }
                
                if (!stillActive) {
                    sendPotionEndedNotification(trackedEffect);
                    effectsToRemove.add(effectKey);
                    warnedEffects.remove(effectKey);
                }
            }
            effectsToRemove.forEach(activeEffects::remove);
        }
        
        if (list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastEffectChange >= 1000) {
                List<RegistryEntry<StatusEffect>> effects = new ArrayList<>();
                for (Identifier id : Registries.STATUS_EFFECT.getIds()) {
                    Registries.STATUS_EFFECT.getEntry(id).ifPresent(effects::add);
                }
                if (!effects.isEmpty()) {
                    currentRandomEffect = effects.get(new Random().nextInt(effects.size()));
                    lastEffectChange = currentTime;
                }
            }
        }
    }

    @Override
    public void packet(PacketEvent e) {
        switch (e.getPacket()) {
            case EntityStatusEffectS2CPacket effect -> {
                if (!PlayerInteractionHelper.nullCheck() && effect.getEntityId() == Objects.requireNonNull(mc.player).getId()) {
                    RegistryEntry<StatusEffect> effectId = effect.getEffectId();
                    String effectKey = effectId.getIdAsString();
                    
                    
                    boolean isNewEffect = !activeEffects.containsKey(effectKey);
                    
                    list.stream().filter(p -> p.effect.getEffectType().getIdAsString().equals(effectKey)).forEach(s -> s.anim.setDirection(Direction.BACKWARDS));
                    StatusEffectInstance newEffect = new StatusEffectInstance(effectId, effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon());
                    list.add(new Potion(newEffect, new Decelerate().setMs(150).setValue(1.0F)));
                    
                    
                    activeEffects.put(effectKey, newEffect);
                    
                    
                    if (isNewEffect && Hud.getInstance().notificationSettings.isSelected("Полученном зелье")) {
                        sendPotionReceivedNotification(newEffect);
                    }
                }
            }
            case RemoveEntityStatusEffectS2CPacket effect -> {
                String effectKey = effect.effect().getIdAsString();
                list.stream().filter(s -> s.effect.getEffectType().getIdAsString().equals(effectKey)).forEach(s -> s.anim.setDirection(Direction.BACKWARDS));
                
                StatusEffectInstance removedEffect = activeEffects.remove(effectKey);
                if (removedEffect != null && Hud.getInstance().notificationSettings.isSelected("Заканчивающемся зелье")) {
                    sendPotionEndedNotification(removedEffect);
                }
                warnedEffects.remove(effectKey);
            }
            case PlayerRespawnS2CPacket p -> {
                list.clear();
                activeEffects.clear();
                warnedEffects.clear();
            }
            case GameJoinS2CPacket p -> {
                list.clear();
                activeEffects.clear();
                warnedEffects.clear();
            }
            default -> {}
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(15, Fonts.Type.SuisseIntlMedium);

        FontRenderer fontPotion = Fonts.getSize(13, Fonts.Type.SuisseIntlMedium);
        FontRenderer fontlevel = Fonts.getSize(11, Fonts.Type.SuisseIntlMedium);
        FontRenderer items = Fonts.getSize(12, Fonts.Type.SuisseIntlMedium);

        long activeEffects = list.stream().filter(p -> !p.anim.isFinished(Direction.BACKWARDS)).count();
        String effectCountText = String.valueOf(activeEffects);
        float textWidth = items.getStringWidth(effectCountText);
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

        Fonts.getSize(15, Fonts.Type.ESSENCE).drawString(matrix, "r", getX() + 8f, getY() + 9.5f, ThemeManager.textColor.getColor());
        font.drawString(matrix, getName(), getX() + 18, getY() + 9.5f, ThemeManager.textColor.getColor());
        FontRenderer essenceFont = Fonts.getSize(15, Fonts.Type.ESSENCE);
        essenceFont.drawString(matrix, "p", getX() + getWidth() - essenceFont.getStringWidth("p") - 8, getY() + 9.9f, ThemeManager.textColor.getColor());

        float centerX = getX() + getWidth() / 2.0F;
        int offset = 26;
        int maxWidth = 65;

        if (list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            float centerY = getY() + offset;
            String name = "Example";
            String duration = "**:**";
            int textColor = ColorAssist.getText();
            int textAlpha = 255;
            int colorWithAlpha = ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
            int iconColor = ColorAssist.rgba(255, 255, 255, textAlpha);
            int orangeColor = new Color(255, 101, 57, 255).getRGB();
            int colorWithAlphaRectangle = ColorAssist.rgba((textColor >> 16) & 205, (textColor >> 8) & 205, textColor & 205, textAlpha - 125);
            float durationWidth = fontPotion.getStringWidth(duration);
            float durationBoxWidth = durationWidth + 6;
            Calculate.scale(matrix, centerX, centerY, 1, 1, () -> {
                Render2D.drawSprite(matrix, mc.getStatusEffectSpriteManager().getSprite(currentRandomEffect), getX() + 7.5F, (int) centerY - 0.5f, 8, 8, iconColor);

                rectangle.render(ShapeProperties.create(matrix, getX() + 18F, centerY + 2.5, 2, 2)
                        .round(2 / 2f)
                        .color(new Color(87, 87, 90, 255).getRGB())
                        .build());
                fontPotion.drawString(matrix, name, getX() + 22.5f, centerY + 2.5, ThemeManager.textColor.getColor());

                rectangle.render(ShapeProperties.create(matrix, getX() + getWidth() - durationBoxWidth - 8, centerY - 1.5f, durationBoxWidth, 10F)
                        .round(2)
                        .thickness(2)
                        .outlineColor(new Color(33, 33, 33, 255).getRGB())
                        .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(), ColorAssist.getClientColor(), ColorAssist.getClientColor2())
                        .build());
                fontPotion.drawString(matrix, duration, getX() + getWidth() - durationWidth - 11, centerY + 2, ThemeManager.textColor.getColor());
            });
            int width = (int) fontPotion.getStringWidth(name + duration) + 30;
            maxWidth = Math.max(width, maxWidth);
            offset += 11;
        } else {
            for (Potion potion : list) {
                StatusEffectInstance effect = potion.effect;
                float animation = potion.anim.getOutput().floatValue();
                float centerY = getY() + offset;
                int amplifier = effect.getAmplifier();
                String name = effect.getEffectType().value().getName().getString();
                String duration = getDuration(effect);
                String lvl = amplifier > 0 ? Formatting.RED + " " + (amplifier + 1) + Formatting.RESET : "";
                boolean isBadEffect = isBadEffect(effect.getEffectType());
                int textColor = isBadEffect ? ColorAssist.rgba(255, 85, 75, 255) : ThemeManager.textColor.getColor();
                int textAlpha = 255;
                if (effect.getDuration() <= 200 && effect.getDuration() > 0) {
                    double output = 0.5 + 0.5 * Math.cos(2 * Math.PI * (System.currentTimeMillis() % 700) / 700.0);
                    textAlpha = (int) (100 + (155 * output));
                } else if (effect.getDuration() == 0) {
                    textAlpha = 0;
                }
                int colorWithAlpha = isBadEffect ? ColorAssist.rgba(255, 85, 75, textAlpha) : ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
                int iconColor = ColorAssist.rgba(255, 255, 255, textAlpha);
                int orangeColor = new Color(255, 101, 57, 255).getRGB();
                int colorWithAlphaRectangle = isBadEffect ? ColorAssist.rgba(255, 85, 75, textAlpha - 125) : ColorAssist.rgba((textColor >> 16) & 205, (textColor >> 8) & 205, textColor & 205, textAlpha - 125);
                float durationWidth = fontPotion.getStringWidth(duration);
                float durationBoxWidth = durationWidth + 6;
                Calculate.scale(matrix, centerX, centerY, 1, animation, () -> {
                    Render2D.drawSprite(matrix, mc.getStatusEffectSpriteManager().getSprite(effect.getEffectType()), getX() + 7.5F, (int) centerY - 0.5f, 8, 8, iconColor);
                    rectangle.render(ShapeProperties.create(matrix, getX() + 18F, centerY + 2.5, 2, 2)
                            .round(2 / 2f)
                            .color(new Color(87, 87, 90, 255).getRGB())
                            .build());
                    fontPotion.drawString(matrix, name, getX() + 22.5f, centerY + 2.5, colorWithAlpha);
                    if (amplifier > 0) {
                        String level = " " + (amplifier + 1);
                        fontlevel.drawString(matrix, level, getX() + 22.5f + fontPotion.getStringWidth(name), centerY + 3.5, new Color(157, 157, 160, 255).getRGB());
                    }
                    rectangle.render(ShapeProperties.create(matrix, getX() + getWidth() - durationBoxWidth - 8, centerY - 1.5f, durationBoxWidth, 10F)
                            .round(2)
                            .thickness(2)
                                      .outlineColor(new Color(33, 33, 33, 255).getRGB())
                            .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(), ColorAssist.getClientColor(), ColorAssist.getClientColor2())
                            .build());
                    fontPotion.drawString(matrix, duration, getX() + getWidth() - durationWidth - 11, centerY + 2, ThemeManager.textColor.getColor());
                });
                int width = (int) fontPotion.getStringWidth(name + lvl + duration) + 30;
                maxWidth = Math.max(width, maxWidth);
                offset += (int) (11 * animation);
            }
        }
        setWidth(maxWidth + 21);
        setHeight(offset + 3);
    }

    private String getDuration(StatusEffectInstance pe) {
        int var1 = pe.getDuration();
        int mins = var1 / 1200;
        return pe.isInfinite() || mins > 60 ? "**:**" : mins + ":" + String.format("%02d", (var1 % 1200) / 20);
    }

    private boolean isBadEffect(RegistryEntry<StatusEffect> effect) {
        for (RegistryEntry<StatusEffect> negativeEffect : NEGATIVE_EFFECTS) {
            if (effect == negativeEffect) {
                return true;
            }
        }
        return false;
    }

    private void sendPotionReceivedNotification(StatusEffectInstance effect) {
        String effectName = effect.getEffectType().value().getName().getString();
        String message = effectName + " получен!";
        Notifications.getInstance().addList(Text.literal(message), 1000, SoundManager.ENABLE_MODULE, false, null, effect.getEffectType());
        SoundManager.playSound(SoundManager.ENABLE_MODULE);
    }
    
    private void sendPotionEndedNotification(StatusEffectInstance effect) {
        String effectName = effect.getEffectType().value().getName().getString();
        String message = effectName + " закончился!";
        Notifications.getInstance().addList(Text.literal(message), 1000, SoundManager.DISABLE_MODULE, false, null, effect.getEffectType());
        SoundManager.playSound(SoundManager.DISABLE_MODULE);
    }
    
    private void sendPotionEndingSoonNotification(StatusEffectInstance effect) {
        String effectName = effect.getEffectType().value().getName().getString();
        String message = effectName + " закончится через 10 секунд!";
        Notifications.getInstance().addList(Text.literal(message), 1000, null, false, null, effect.getEffectType());
    }
    

    private record Potion(StatusEffectInstance effect, Animation anim) {}
}