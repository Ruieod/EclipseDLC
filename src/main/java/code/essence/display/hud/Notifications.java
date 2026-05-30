package code.essence.display.hud;

import code.essence.common.animation.implement.Decelerate;
import code.essence.common.animation.implement.EaseOut;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.display.render.geometry.Render2D;
import code.essence.utils.display.render.post.KawaseBlur;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import code.essence.utils.client.managers.api.draggable.AbstractDraggable;
import code.essence.common.animation.Animation;
import code.essence.common.animation.Direction;
import code.essence.utils.display.render.font.FontRenderer;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.client.sound.SoundManager;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.client.Instance;
import code.essence.events.container.SetScreenEvent;
import code.essence.events.packet.PacketEvent;
import code.essence.features.impl.render.Hud;
import code.essence.utils.theme.ThemeManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Notifications extends AbstractDraggable {
    public static Notifications getInstance() {
        return Instance.getDraggable(Notifications.class);
    }

    private final List<Notification> list = new ArrayList<>();
    private final List<Stack> stacks = new ArrayList<>();
    private long lastItemChange = 0;
    private int currentItemIndex = 0;
    private static final Item[] EXAMPLE_ITEMS = {
            Items.ENDER_EYE, Items.ENDER_PEARL, Items.SUGAR, Items.MACE, Items.ENCHANTED_GOLDEN_APPLE,
            Items.TRIDENT, Items.CROSSBOW, Items.DRIED_KELP, Items.NETHERITE_SCRAP, Items.DIAMOND,
            Items.NETHERITE_INGOT, Items.EMERALD, Items.GOLD_INGOT, Items.IRON_INGOT
    };

    public Notifications() {
        super("Notifications", 0, 350, 120, 14, true);
        this.scaleAnimation = new EaseOut().setValue(1).setMs(100);
    }

    @Override
    public void tick() {
        list.forEach(notif -> {
            if (System.currentTimeMillis() > notif.removeTime || (notif.text.getString().contains("essencepenit.fun") && !PlayerInteractionHelper.isChat(mc.currentScreen)))
                notif.anim.setDirection(Direction.BACKWARDS);
        });
        list.removeIf(notif -> notif.anim.isFinished(Direction.BACKWARDS));

        if (list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastItemChange >= 1000) {
                currentItemIndex = (currentItemIndex + 1) % EXAMPLE_ITEMS.length;
                lastItemChange = currentTime;
            }
        }

        while (!stacks.isEmpty()) {
            addTextIfNotEmpty(TypePickUp.INVENTORY, "Подняты предметы: ");
            addTextIfNotEmpty(TypePickUp.SHULKER_INVENTORY, "Сложены предметы в шалкер: ");
            addTextIfNotEmpty(TypePickUp.SHULKER, "Поднят шалкер с: ");
        }
    }

    @Override
    public void packet(PacketEvent e) {
        if (!PlayerInteractionHelper.nullCheck()) switch (e.getPacket()) {
            case ItemPickupAnimationS2CPacket item when Hud.getInstance().notificationSettings.isSelected("Поднятии предмета") && item.getCollectorEntityId() == Objects.requireNonNull(mc.player).getId() && Objects.requireNonNull(mc.world).getEntityById(item.getEntityId()) instanceof ItemEntity entity -> {
                ItemStack itemStack = entity.getStack();
                ContainerComponent component = itemStack.get(DataComponentTypes.CONTAINER);
                if (component == null) {
                    Text itemText = itemStack.getName();
                    if (itemText.getContent().toString().equals("empty")) {
                        MutableText text = Text.empty().append(itemText);
                        if (itemStack.getCount() > 1) text.append(Formatting.RESET + " [" + Formatting.RED + itemStack.getCount() + Formatting.GRAY + "x" + Formatting.RESET + "]");
                        stacks.add(new Stack(TypePickUp.INVENTORY, text));
                    }
                } else component.stream().filter(s -> s.getName().getContent().toString().equals("empty")).forEach(stack -> {
                    MutableText text = Text.empty().append(stack.getName());
                    if (stack.getCount() > 1) text.append(Formatting.RESET + " [" + Formatting.RED + stack.getCount() + Formatting.GRAY + "x" + Formatting.RESET + "]");
                    stacks.add(new Stack(TypePickUp.SHULKER, text));
                });
            }
            case ScreenHandlerSlotUpdateS2CPacket slot when Hud.getInstance().notificationSettings.isSelected("Поднятии предмета") -> {
                int slotId = slot.getSlot();
                ContainerComponent updatedContainer = slot.getStack().get(DataComponentTypes.CONTAINER);
                if (updatedContainer != null && slotId < Objects.requireNonNull(mc.player).currentScreenHandler.slots.size() && slot.getSyncId() == 0) {
                    ContainerComponent currentContainer = mc.player.currentScreenHandler.getSlot(slotId).getStack().get(DataComponentTypes.CONTAINER);
                    if (currentContainer != null) updatedContainer.stream().filter(stack -> currentContainer.stream().noneMatch(s -> Objects.equals(s.getComponents(), stack.getComponents()) && s.toString().equals(stack.toString()))).forEach(stack -> {
                        MutableText text = Text.empty().append(stack.getName());
                        stacks.add(new Stack(TypePickUp.SHULKER_INVENTORY, text));
                    });
                }
            }
            default -> {}
        }
    }

    @Override
    public void setScreen(SetScreenEvent e) {
        if (e.getScreen() instanceof ChatScreen) {
            addList("essencepenit.fun", 99999999);
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(15, Fonts.Type.SuisseIntlMedium);
        FontRenderer icon = Fonts.getSize(15, Fonts.Type.ESSENCE);

        float offsetY = 0;
        float offsetX = 5;
        for (Notification notification : list) {
            float anim = notification.anim.getOutput().floatValue();
            float textWidth = font.getStringWidth(notification.text.getString());
            float totalWidth = 32 + textWidth + 12;
            float startY = getY() + offsetY;
            float startX = getX() + (getWidth() - totalWidth) / 2;

            float centerX = startX + (totalWidth - 9) / 2f;
            float centerY = startY + (getHeight() + 4) / 2f;
            
            Calculate.setAlpha(anim, () -> {
                Calculate.scale(matrix, centerX, centerY, anim, () -> {
                    if (Hud.blur.isValue()) {
                        Render2D.rectangleWithMask(matrix.peek().getPositionMatrix(), startX, startY, totalWidth - 9, getHeight() + 4, 4f,0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment());
                    }
                    rectangle.render(ShapeProperties.create(matrix, startX, startY, totalWidth-9, getHeight() + 4)
                            .round(4f)
                            .softness(1)
                            .outlineColor(new Color(33, 33, 33, 255).getRGB())
                            .color(ThemeManager.BackgroundGui.getColor())
                            .build());

                    rectangle.render(ShapeProperties.create(matrix, startX + 19.5, startY + 3, totalWidth - 32, getHeight() -2)
                            .round(2.5f)
                            .softness(2)
                            .outlineColor(new Color(33, 33, 33, 255).getRGB())
                            .color(ThemeManager.BackgroundSettings.getColor())
                            .build());


                float squareSize = 14f;
                float squareX = startX +2;
                float squareY = startY - 1.87f + (getHeight() - squareSize) / 2f;

                int squareColor = notification.off ? ThemeManager.offModuleColor.getColor() : ColorAssist.getClientColor();
                int squareColor2 = notification.off ? ThemeManager.offModuleColor.getColor() : ColorAssist.getClientColor2();
                
                rectangle.render(ShapeProperties.create(matrix, squareX+ .5, squareY +4 , squareSize, squareSize)
                        .round(3.5f)
                        .color(squareColor, squareColor2, squareColor, squareColor2)
                        .build());

                float aX = squareX + (squareSize - Fonts.getSize(14, Fonts.Type.ICONS).getStringWidth("а")) / 2f;
                float aY = squareY + (squareSize - Fonts.getSize(14, Fonts.Type.ICONS).getStringHeight("а")) / 2f;

                if (notification.effectIcon != null) {
                    int iconAlpha = notification.off ? (int) (255 * 0.48f) : 255;
                    int iconColor = ColorAssist.rgba(255, 255, 255, iconAlpha);
                    float iconSize = 10f;
                    float iconX = squareX + .5f + (squareSize - iconSize) / 2f;
                    float iconY = squareY + 4f + (squareSize - iconSize) / 2f;
                    Render2D.drawSprite(matrix, mc.getStatusEffectSpriteManager().getSprite(notification.effectIcon), iconX, iconY, iconSize, (int) iconSize, iconColor);
                } else if (notification.icon != null) {
                    
                    matrix.push();
                    matrix.translate(0, 0, 100); 
                    Render2D.defaultDrawStack(context, notification.icon, squareX + 1, squareY + 3, false, false, 0.7F);
                    matrix.pop();
                } else if (list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
                    Item item = EXAMPLE_ITEMS[currentItemIndex];
                    Render2D.drawStack(matrix, item.getDefaultStack(), squareX + 2, squareY + 2, false, 0.5F);
                } else {
                    Fonts.getSize(20, Fonts.Type.ESSENCE).drawString(matrix, "a", aX -3, aY + 11, ThemeManager.textColor.getColor());
                }

                float grayAreaStart = startX + 28;
                float grayAreaWidth = totalWidth - 32;
                float textX = grayAreaStart + (grayAreaWidth - textWidth) / 2;
                float textY = startY + (getHeight() + 12 - font.getStringHeight(notification.text.getString())) / 2;
                
                int textColor;
                if (notification.off) {
                    int backgroundColor = ThemeManager.BackgroundGui.getColor();
                    int r = ColorAssist.getRed(backgroundColor);
                    int g = ColorAssist.getGreen(backgroundColor);
                    int b = ColorAssist.getBlue(backgroundColor);
                    float brightness = (r + g + b) / 3.0f;
                    if (brightness < 128) {
                        textColor = ColorAssist.rgba(137, 137, 140, 255);
                    } else {
                        textColor = ColorAssist.rgba(135, 135, 135, 255);
                    }
                } else {
                    textColor = ThemeManager.textColor.getColor();
                }
                
                font.drawString(matrix, notification.text.getString(), textX-8.5, textY+3.5, textColor);

                if (!notification.isExpired()) {
                    float progress;
                    long elapsed = System.currentTimeMillis() - notification.startTime;
                    long totalTime = notification.removeTime - notification.startTime;
                    progress = 1.0f - Math.min(1.0f, (float) elapsed / totalTime);
                    float progressWidth = totalWidth * progress;

                }
                });
            });
            offsetY += (getHeight() + 4 + 3) * anim;
        }
    }

    private void addTextIfNotEmpty(TypePickUp type, String prefix) {
        MutableText text = Text.empty();
        List<Stack> list = stacks.stream().filter(stack -> stack.type.equals(type)).toList();
        for (int i = 0, size = list.size(); i < size; i++) {
            Stack stack = list.get(i);
            if (stack.type != type) continue;
            text.append(stack.text);
            stacks.remove(stack);
            if (text.getString().length() > 150) break;
            if (i + 1 != size) text.append(" , ");
        }
        if (!text.equals(Text.empty())) addList(Text.empty().append(prefix).append(text), 8000);
    }

    public void addList(String text, long removeTime) {
        addList(text, removeTime, null);
    }

    public void addList(Text text, long removeTime) {
        addList(text, removeTime, null);
    }

    public void addList(String text, long removeTime, SoundEvent sound) {
        addList(Text.empty().append(text), removeTime, sound);
    }

    public void addList(String text, long removeTime, boolean off) {
        addList(Text.empty().append(text), removeTime, null, off);
    }

    public void addList(Text text, long removeTime, SoundEvent sound) {
        addList(text, removeTime, sound, false);
    }

    public void addList(Text text, long removeTime, SoundEvent sound, boolean off) {
        addList(text, removeTime, sound, off, null);
    }
    
    public void addList(Text text, long removeTime, SoundEvent sound, boolean off, ItemStack icon) {
        addList(text, removeTime, sound, off, icon, null);
    }
    
    public void addList(Text text, long removeTime, SoundEvent sound, boolean off, ItemStack icon, RegistryEntry<StatusEffect> effectIcon) {
        list.add(new Notification(text, new Decelerate().setMs(100).setValue(1), System.currentTimeMillis(), System.currentTimeMillis() + removeTime, off, icon, effectIcon));
        if (list.size() > 12) list.removeFirst();
        list.sort(Comparator.comparingDouble(notif -> -notif.removeTime));
        if (sound != null) SoundManager.playSound(sound);
    }

    public record Notification(Text text, Animation anim, long startTime, long removeTime, boolean off, ItemStack icon, RegistryEntry<StatusEffect> effectIcon) {
        public Notification(Text text, Animation anim, long startTime, long removeTime, boolean off) {
            this(text, anim, startTime, removeTime, off, null, null);
        }
        
        public Notification(Text text, Animation anim, long startTime, long removeTime, boolean off, ItemStack icon) {
            this(text, anim, startTime, removeTime, off, icon, null);
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > removeTime;
        }
    }

    public record Stack(TypePickUp type, MutableText text) {}

    public enum TypePickUp {
        INVENTORY, SHULKER, SHULKER_INVENTORY
    }
}