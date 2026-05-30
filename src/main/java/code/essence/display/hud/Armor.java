package code.essence.display.hud;

import code.essence.common.animation.implement.EaseOut;
import code.essence.features.impl.render.Hud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import code.essence.utils.client.managers.api.draggable.AbstractDraggable;
import code.essence.utils.display.render.font.FontRenderer;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.geometry.Render2D;

import java.util.ArrayList;
import java.util.List;

public class Armor extends AbstractDraggable {
    private final List<ItemStack> armorStacks = new ArrayList<>();

    public Armor() {
        super("Armor", 10, 120, 80, 15, true);
        this.scaleAnimation = new EaseOut().setValue(1).setMs(100);
    }

    @Override
    public boolean visible() {
        return Hud.getInstance().interfaceSettings.isSelected("Armor") && Hud.getInstance().state;
    }

    @Override
    public void tick() {
        if (mc.player == null) return;
        armorStacks.clear();
        armorStacks.add(mc.player.getEquippedStack(EquipmentSlot.HEAD));
        armorStacks.add(mc.player.getEquippedStack(EquipmentSlot.CHEST));
        armorStacks.add(mc.player.getEquippedStack(EquipmentSlot.LEGS));
        armorStacks.add(mc.player.getEquippedStack(EquipmentSlot.FEET));
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (!visible()) return;

        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(12, Fonts.Type.SuisseIntlMedium);

        int slotSize = 18;
        int spacing = 2;
        int totalWidth = (slotSize + spacing) * 4 - spacing;
        int totalHeight = slotSize + 2;

        setWidth(totalWidth);
        setHeight(totalHeight);

        float x = getX();
        float y = getY();
        
        for (int i = 0; i < 4; i++) {
            float slotX = x + i * (slotSize + spacing);
            float slotY = y;
            
            ItemStack stack = i < armorStacks.size() ? armorStacks.get(i) : ItemStack.EMPTY;

            if (!stack.isEmpty()) {
                Render2D.defaultDrawStack(context, stack, slotX, slotY, true, true, 1.0F);
            }
        }
    }
}
