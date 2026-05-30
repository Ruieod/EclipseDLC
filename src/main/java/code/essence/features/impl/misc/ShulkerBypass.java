package code.essence.features.impl.misc;

import code.essence.events.keyboard.KeyEvent;
import code.essence.events.player.TickEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.BindSetting;
import code.essence.utils.client.chat.ChatMessage;
import code.essence.utils.client.managers.event.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

public class ShulkerBypass extends Module {


    public ShulkerBypass() {
        super("ShulkerBypass", ModuleCategory.MISC);
    }

    @EventHandler
    public void onKey(KeyEvent event) {

        if (!(mc.currentScreen instanceof ShulkerBoxScreen screen)) return;
        if (!(mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler)) return;

        Slot hoveredSlot = getSlotUnderMouse(screen);
        if (hoveredSlot == null || !hoveredSlot.hasStack()) return;

        int emptySlot = findEmptyHotbarOrInventorySlot();
        if (emptySlot == -1) return;

        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, hoveredSlot.id, emptySlot, SlotActionType.SWAP, mc.player);
    }

    private Slot getSlotUnderMouse(HandledScreen<?> screen) {
        double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();

        for (Slot slot : screen.getScreenHandler().slots) {
            if (isPointOverSlot(slot, mouseX, mouseY, screen)) {
                return slot;
            }
        }
        return null;
    }

    private boolean isPointOverSlot(Slot slot, double pointX, double pointY, HandledScreen<?> screen) {
        int x = screen.x + slot.x;
        int y = screen.y + slot.y;
        return pointX >= x - 1 && pointX < x + 16 + 1 && pointY >= y - 1 && pointY < y + 16 + 1;
    }

    private int findEmptyHotbarOrInventorySlot() {
        for (Slot slot : mc.player.currentScreenHandler.slots) {
            if (!slot.hasStack() && slot.inventory == mc.player.getInventory()) {
                return slot.getIndex();
            }
        }
        return -1;
    }
}