package code.essence.display.hud;

import code.essence.utils.display.render.post.KawaseBlur;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.interactions.inv.InventoryTask;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import code.essence.utils.client.managers.api.draggable.AbstractDraggable;
import code.essence.features.impl.misc.ServerHelper;
import code.essence.features.impl.render.Hud;
import code.essence.common.animation.Animation;
import code.essence.common.animation.Direction;
import code.essence.common.animation.implement.Decelerate;
import code.essence.common.animation.implement.EaseOut;
import code.essence.utils.display.render.font.FontRenderer;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.client.chat.StringHelper;
import code.essence.utils.display.render.geometry.Render2D;
import code.essence.utils.theme.ThemeManager;
import com.mojang.blaze3d.systems.RenderSystem;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class BindsHw extends AbstractDraggable {
    private final ServerHelper serverHelper;
    private final List<BindInfo> binds = new ArrayList<>();
    private final Map<Item, Long> cooldownStartTimes = new HashMap<>();
    private final Set<Item> activeCooldowns = new HashSet<>();
    private static final int BINDS_PER_ROW = 5;
    private float width;
    private float height;
    private long lastBindChange = 0;
    private java.lang.String currentRandomKey1 = "None";
    private java.lang.String currentRandomKey2 = "None";
    private java.lang.String currentRandomKey3 = "None";
    private int currentItemIndex1 = 0;
    private int currentItemIndex2 = 1;
    private int currentItemIndex3 = 2;
    private static final Item[] EXAMPLE_ITEMS = {Items.ENDER_EYE, Items.SUGAR, Items.DRIED_KELP, Items.NETHERITE_SCRAP};
    private final Animation animation = new Decelerate().setMs(300).setValue(1);

    private static class BindInfo {
        ServerHelper.KeyBind keyBind;
        java.lang.String displayName;
        int color;
        java.lang.String searchName;

        BindInfo(ServerHelper.KeyBind keyBind, java.lang.String displayName, int color, java.lang.String searchName) {
            this.keyBind = keyBind;
            this.displayName = displayName;
            this.color = color;
            this.searchName = searchName;
        }
    }

    
    private static class BindData {
        ItemStack stack;
        String keyName;
        float buttonWidth;
        int color;
        boolean onCooldown;

        BindData(ItemStack stack, String keyName, float buttonWidth, int color, boolean onCooldown) {
            this.stack = stack;
            this.keyName = keyName;
            this.buttonWidth = buttonWidth;
            this.color = color;
            this.onCooldown = onCooldown;
        }
    }

    public BindsHw() {
        super("Binds", 10, 180, 150, 40, true);
        this.serverHelper = ServerHelper.getInstance();
        this.scaleAnimation = new EaseOut().setValue(1).setMs(100);
        initializeBinds();
    }

    private void initializeBinds() {
        for (ServerHelper.KeyBind keyBind : serverHelper.getKeyBindings()) {
            java.lang.String name = keyBind.setting().getName();
            java.lang.String searchName = getSearchNameForBind(name);
            int color = getColorForBind(name);
            binds.add(new BindInfo(keyBind, name, color, searchName));
        }
    }

    private java.lang.String getSearchNameForBind(java.lang.String name) {
        return switch (name) {
            case "Хлопушка" -> "хлопушка";
            case "Святая вода" -> "святая";
            case "Снотворное" -> "снотворное";
            case "Зелье гнева" -> "гнева";
            case "Зелье паладина" -> "паладина";
            case "Зелье ассасина" -> "ассасина";
            case "Зелье радиации" -> "радиации";
            default -> null;
        };
    }

    private int getColorForBind(java.lang.String name) {
        return switch (name) {
            case "Хлопушка" -> 0xFF5D00;
            case "Святая вода" -> 0x00C200;
            case "Снотворное" -> 0xFFFFFF;
            case "Зелье гнева" -> 0x5CF7FF;
            case "Зелье паладина" -> 0x00FF00;
            case "Зелье ассасина" -> 0xFFFB00;
            case "Зелье радиации" -> 0xFF00DE;
            default -> -1;
        };
    }

    private ItemStack createColoredPotion(Item item, int color) {
        ItemStack stack = new ItemStack(item);
        if (color != -1 && item == Items.SPLASH_POTION) {
            stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.of(color), List.of(), Optional.empty()));
        }
        return stack;
    }

    @Override
    public boolean visible() {
        return Hud.getInstance().interfaceSettings.isSelected("Binds") && Hud.getInstance().state && !animation.isFinished(Direction.BACKWARDS);
    }

    @Override
    public void tick() {
        if (PlayerInteractionHelper.nullCheck()) {
            animation.setDirection(Direction.BACKWARDS);
            return;
        }
        List<BindInfo> activeBinds = binds.stream().filter(this::isBindActive).toList();
        if (!activeBinds.isEmpty()) {
            animation.setDirection(Direction.FORWARDS);
        } else if (PlayerInteractionHelper.isChat(mc.currentScreen)) {
            animation.setDirection(Direction.FORWARDS);
        } else {
            animation.setDirection(Direction.BACKWARDS);
        }
        activeCooldowns.clear();
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (!itemStack.isEmpty() && mc.player.getItemCooldownManager().isCoolingDown(itemStack)) {
                Item item = itemStack.getItem();
                if (!cooldownStartTimes.containsKey(item)) {
                    cooldownStartTimes.put(item, currentTime);
                }
                activeCooldowns.add(item);
            }
        }
        cooldownStartTimes.keySet().removeIf(item -> !activeCooldowns.contains(item));
        if (activeBinds.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            if (currentTime - lastBindChange >= 1000) {
                List<java.lang.String> availableKeys = List.of("A", "B", "C", "D", "E");
                currentRandomKey1 = availableKeys.get(new Random().nextInt(availableKeys.size()));
                currentRandomKey2 = availableKeys.get(new Random().nextInt(availableKeys.size()));
                currentRandomKey3 = availableKeys.get(new Random().nextInt(availableKeys.size()));
                currentItemIndex1 = (currentItemIndex1 + 1) % EXAMPLE_ITEMS.length;
                currentItemIndex2 = (currentItemIndex2 + 1) % EXAMPLE_ITEMS.length;
                currentItemIndex3 = (currentItemIndex3 + 1) % EXAMPLE_ITEMS.length;
                lastBindChange = currentTime;
            }
        }
    }

    private boolean isBindActive(BindInfo bind) {
        if (PlayerInteractionHelper.nullCheck() || !bind.keyBind.setting().isVisible() || bind.keyBind.setting().getKey() == -1 || bind.keyBind.setting().getKey() == 0) {
            return false;
        }
        
        
        
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == bind.keyBind.item()) {
        if (bind.searchName != null && bind.keyBind.item() == Items.SPLASH_POTION) {
                    String cleanName = InventoryTask.getCleanName(stack.getName());
                    if (cleanName.contains(bind.searchName.toLowerCase())) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isItemOnCooldown(BindInfo bind) {
        return activeCooldowns.contains(bind.keyBind.item());
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (!Hud.getInstance().interfaceSettings.isSelected("Binds") || !Hud.getInstance().state || animation.isFinished(Direction.BACKWARDS)) return;
        MatrixStack matrix = context.getMatrices();
        float animationValue = animation.getOutput().floatValue();
        if (animationValue <= 0) return;

        List<BindInfo> activeBinds = binds.stream().filter(this::isBindActive).collect(Collectors.toList());
        float padding = 4; 
        float iconSize = 16;
        float buttonHeight = 16;
        float horizontalSpacing = 2; 

        FontRenderer fontKey = Fonts.getSize(13, Fonts.Type.SuisseIntlMedium);
        int maxBindsPerRow = 10;

        List<BindData> bindDataList = new ArrayList<>();

        
        if (activeBinds.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            
            String[] keys = {currentRandomKey1, currentRandomKey2, currentRandomKey3};
            ItemStack[] stacks = {new ItemStack(EXAMPLE_ITEMS[currentItemIndex1]),
                    new ItemStack(EXAMPLE_ITEMS[currentItemIndex2]),
                    new ItemStack(EXAMPLE_ITEMS[currentItemIndex3])};

            for (int i = 0; i < Math.min(3, maxBindsPerRow); i++) {
                float keyWidth = fontKey.getStringWidth(keys[i]) + 8; 
                float buttonWidth = Math.max(iconSize, keyWidth); 

                bindDataList.add(new BindData(
                        stacks[i],
                        keys[i],
                        buttonWidth,
                        -1, 
                        false 
                ));
            }
        } else {
            
            for (int i = 0; i < Math.min(activeBinds.size(), maxBindsPerRow); i++) {
                BindInfo bind = activeBinds.get(i);
                ItemStack stack = createColoredPotion(bind.keyBind.item(), bind.color);
                String keyName = StringHelper.getBindName(bind.keyBind.setting().getKey());

                float keyWidth = fontKey.getStringWidth(keyName) + 8; 
                float buttonWidth = Math.max(iconSize, keyWidth); 

                bindDataList.add(new BindData(
                        stack,
                        keyName,
                        buttonWidth,
                        bind.color,
                        isItemOnCooldown(bind)
                ));
            }
        }

        
        float totalWidth = 0;
        float maxBindHeight = 0;

        for (int i = 0; i < bindDataList.size(); i++) {
            BindData bindData = bindDataList.get(i);
            float bindWidth = Math.max(iconSize, bindData.buttonWidth) + padding * 2; 
            float bindHeight = padding + iconSize + padding + padding + buttonHeight + padding;

            totalWidth += bindWidth;
            if (i < bindDataList.size() - 1) {
                totalWidth += horizontalSpacing; 
            }

            maxBindHeight = Math.max(maxBindHeight, bindHeight);
        }

        if (bindDataList.isEmpty()) {
            totalWidth = 0;
            maxBindHeight = 0;
        }

        float posX = getX();
        float posY = getY();

        
        float currentX = posX;
        for (int i = 0; i < bindDataList.size(); i++) {
            BindData bindData = bindDataList.get(i);
            float bindWidth = Math.max(iconSize, bindData.buttonWidth) + padding * 2;
            float bindHeight = maxBindHeight - 3.5f;

            
            if (Hud.blur.isValue()) {
                Render2D.rectangleWithMask(matrix.peek().getPositionMatrix(), currentX, posY, bindWidth, bindHeight, 4.5f,0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment());

            }
            rectangle.render(ShapeProperties.create(matrix, currentX, posY, bindWidth, bindHeight)
                    .round(4.5f)
                    .outlineColor(new Color(33, 33, 33, 255).getRGB())
                    .color(ThemeManager.BackgroundGui.getColor())
                    .build());

            
            float bindCenterX = currentX + bindWidth / 2;

            
            float iconBgWidth = bindData.buttonWidth; 
            float iconBgX = bindCenterX - iconBgWidth / 2;
            float iconBgY = posY + padding;

            
            rectangle.render(ShapeProperties.create(matrix, iconBgX, iconBgY, iconBgWidth, iconSize)
                    .round(3f)
                    .softness(1)
                    .outlineColor(new Color(33, 33, 33, 255).getRGB())
                    .color(ThemeManager.BackgroundSettings.getColor())
                    .build());

            
            float itemX = bindCenterX - iconSize / 2; 
            float itemY = iconBgY;

            RenderSystem.disableDepthTest();
            matrix.push();
            matrix.translate(itemX, itemY, 0);
            Render2D.defaultDrawStack(context, bindData.stack, +0.3f, 0, false, false, 0.83f);
            matrix.pop();
            RenderSystem.enableDepthTest();

            
            if (bindData.onCooldown) {
                rectangle.render(ShapeProperties.create(matrix, itemX, itemY, iconSize, iconSize)
                        .round(5f)
                        .color(new Color(0, 0, 0, 100).getRGB())
                        .build());
            }

            
            float buttonX = bindCenterX - bindData.buttonWidth / 2;
            float buttonY = posY + padding + iconSize + padding;

            
            rectangle.render(ShapeProperties.create(matrix, buttonX, buttonY, bindData.buttonWidth, buttonHeight)
                    .round(2.5f)
                    .softness(1)
                    .outlineColor(new Color(33, 33, 33, 255).getRGB())
                    .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(),
                            ColorAssist.getClientColor(), ColorAssist.getClientColor2())
                    .build());

            
            float textX = bindCenterX - fontKey.getStringWidth(bindData.keyName) / 2;
            float textY = buttonY + (buttonHeight - fontKey.getStringHeight(bindData.keyName)) / 2 + 4;

            matrix.push();
            matrix.translate(0, 0, 200);
            fontKey.drawString(matrix, bindData.keyName, textX, textY + 3, Color.WHITE.getRGB());
            matrix.pop();

            currentX += bindWidth + horizontalSpacing;
        }

        
        setWidth((int) totalWidth);
        setHeight((int) maxBindHeight);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            FontRenderer essenceFont = Fonts.getSize(15, Fonts.Type.ESSENCE);
            float pX = getX() + getWidth() - essenceFont.getStringWidth("p") - 5;
            float pY = getY() + 6.5f;
            float pWidth = essenceFont.getStringWidth("p");
            float pHeight = essenceFont.getStringHeight("p");

            if (mouseX >= pX && mouseX <= pX + pWidth && mouseY >= pY && mouseY <= pY + pHeight) {
                java.util.List<String> selected = new java.util.ArrayList<>(Hud.getInstance().interfaceSettings.getSelected());
                selected.remove("Binds");
                Hud.getInstance().interfaceSettings.setSelected(selected);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}