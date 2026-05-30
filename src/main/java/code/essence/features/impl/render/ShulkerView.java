package code.essence.features.impl.render;

import code.essence.features.module.setting.implement.*;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.client.Instance;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.joml.Vector4d;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.display.render.geometry.Render2D;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.math.projection.Projection;
import code.essence.events.render.DrawEvent;
import code.essence.events.container.HandledScreenEvent;
import net.minecraft.screen.slot.Slot;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.Comparator;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShulkerView extends Module {
    public static ShulkerView getInstance() {
        return Instance.get(ShulkerView.class);
    }
    
    Identifier TEXTURE = Identifier.of("textures/container.png");
    
    BooleanSetting showGroundItems = new BooleanSetting("Показывать на земле", "Показывать шалкеры, лежащие на земле").setValue(true);

    public ShulkerView() {
        super("ShulkerView", "ShulkerView", ModuleCategory.RENDER);
        setup(showGroundItems);
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (!showGroundItems.isValue()) return;
        
        DrawContext context = e.getDrawContext();
        MatrixStack matrix = context.getMatrices();
        RenderSystem.disableDepthTest();
        matrix.push();
        matrix.translate(0, 0, -1000);
        
        List<Entity> entities = PlayerInteractionHelper.streamEntities()
                .sorted(Comparator.comparing(ent -> ent instanceof ItemEntity item && item.getStack().getName().getContent().toString().equals("empty")))
                .toList();
        
        for (Entity entity : entities) {
            if (entity instanceof ItemEntity item) {
                Vector4d vec4d = Projection.getVector4D(entity);
                ItemStack stack = item.getStack();
                ContainerComponent compoundTag = stack.get(DataComponentTypes.CONTAINER);
                List<ItemStack> list = compoundTag != null ? compoundTag.stream().toList() : List.of();
                if (Projection.cantSee(vec4d)) continue;
                if (!list.isEmpty()) {
                    drawShulkerBox(context, stack, list, vec4d);
                }
            }
        }
        
        matrix.pop();
        RenderSystem.enableDepthTest();
    }

    @EventHandler
    public void onHandledScreen(HandledScreenEvent e) {
        Slot hoverSlot = e.getSlotHover();
        if (hoverSlot == null || !hoverSlot.hasStack()) return;
        
        ItemStack stack = hoverSlot.getStack();
        ContainerComponent compoundTag = stack.get(DataComponentTypes.CONTAINER);
        if (compoundTag == null) return;
        
        List<ItemStack> list = compoundTag.stream().toList();
        if (list.isEmpty()) return;
        
        DrawContext context = e.getDrawContext();
        double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
        
        drawShulkerBoxAtPosition(context, stack, list, (int)mouseX, (int)mouseY);
    }

    private void drawShulkerBox(DrawContext context, ItemStack itemStack, List<ItemStack> stacks, Vector4d vec) {
        MatrixStack matrix = context.getMatrices();
        int width = 176;
        int height = 67;
        int color = ColorAssist.multBright(ColorAssist.replAlpha(((BlockItem) itemStack.getItem()).getBlock().getDefaultMapColor().color, 1F), 1);
        RenderSystem.disableDepthTest();
        matrix.push();
        matrix.translate(Projection.centerX(vec) - (double) width / 4, vec.w + 2, 0);
        matrix.scale(0.5F, 0.5F, 1);
        context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, 0, 0, 0, 0, width, height, width, height, color);
        int posX = 7;
        int posY = 6;
        for (ItemStack stack : stacks.stream().toList()) {
            Render2D.defaultDrawStack(context, stack, posX, posY, false, true, 1);
            posX += 18;
            if (posX >= 165) {
                posY += 18;
                posX = 7;
            }
        }
        matrix.pop();
    }

    private void drawShulkerBoxAtPosition(DrawContext context, ItemStack itemStack, List<ItemStack> stacks, int mouseX, int mouseY) {
        MatrixStack matrix = context.getMatrices();
        int width = 176;
        int height = 67;
        int color = ColorAssist.multBright(ColorAssist.replAlpha(((BlockItem) itemStack.getItem()).getBlock().getDefaultMapColor().color, 1F), 1);
        RenderSystem.disableDepthTest();
        matrix.push();
        
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        float scale = 0.8F;
        int scaledWidth = (int)(width * scale);
        int scaledHeight = (int)(height * scale);
        
        int tooltipHeight = 20;
        int posX = mouseX + 10;
        int posY = mouseY + tooltipHeight + 5;
        
        if (posX + scaledWidth > screenWidth) {
            posX = mouseX - scaledWidth - 10;
        }
        if (posY + scaledHeight > screenHeight) {
            posY = mouseY - scaledHeight - tooltipHeight - 15;
        }
        if (posX < 0) posX = 10;
        if (posY < 0) posY = 10;
        
        matrix.translate(posX, posY - 100, 300);
        matrix.scale(scale, scale, 1);
        context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, 0, 0, 0, 0, width, height, width, height, color);
        int itemPosX = 7;
        int itemPosY = 6;
        for (ItemStack stack : stacks.stream().toList()) {
            Render2D.defaultDrawStack(context, stack, itemPosX, itemPosY, false, true, 1);
            itemPosX += 18;
            if (itemPosX >= 165) {
                itemPosY += 18;
                itemPosX = 7;
            }
        }
        matrix.pop();
    }
}

