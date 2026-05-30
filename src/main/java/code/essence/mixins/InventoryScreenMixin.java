package code.essence.mixins;

import code.essence.display.widgets.ClearButtonWidget;
import code.essence.features.impl.movement.GuiMove;
import code.essence.mixins.IScreen;
import code.essence.utils.interactions.inv.InventoryTask;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static code.essence.utils.display.interfaces.QuickImports.mc;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends HandledScreen<PlayerScreenHandler> {
    @Unique
    private ClearButtonWidget deleteButton;
    
    @Unique
    public ClearButtonWidget getDeleteButton() {
        return deleteButton;
    }

    public InventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }




    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        
        if (!GuiMove.mode.isSelected("СпукиТайм")) {
            return;
        }
        

        InventoryScreen screen = (InventoryScreen) (Object) this;

        deleteButton = new ClearButtonWidget(0, 0, 12, 12, () -> {

            deleteAllItems();
        });
        
        IScreen iScreen = (IScreen) screen;
        iScreen.getDrawables().add(deleteButton);
        iScreen.getSelectables().add(deleteButton);
        iScreen.getChildren().add(deleteButton);
    }
    
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        
        if (deleteButton != null && GuiMove.mode.isSelected("СпукиТайм")) {
            InventoryScreen screen = (InventoryScreen) (Object) this;
            Screen screenBase = (Screen) screen;
            int guiLeft = (screenBase.width - this.backgroundWidth) / 2;
            int guiTop = (screenBase.height - 166) / 2;
            int buttonX = guiLeft + this.backgroundWidth - 12 - 5;
            int buttonY = guiTop + 5;
            deleteButton.setPosition(buttonX, buttonY);
        } else if (deleteButton != null && !GuiMove.mode.isSelected("СпукиТайм")) {
            
            InventoryScreen screen = (InventoryScreen) (Object) this;
            IScreen iScreen = (IScreen) screen;
            iScreen.getDrawables().remove(deleteButton);
            iScreen.getSelectables().remove(deleteButton);
            iScreen.getChildren().remove(deleteButton);
            deleteButton = null;
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addDropAllButton(CallbackInfo ci) {

        MinecraftClient mc = MinecraftClient.getInstance();
        InventoryScreen screen = (InventoryScreen) (Object) this;

        int x = screen.width / 2 - 40;
        int y = screen.height / 2 - 110;

        ButtonWidget dropAllButton = ButtonWidget.builder(
                Text.of("Выкинуть все"),
                button -> dropAllItems(mc)
        ).position(x, y).size(80, 20).build();

        screen.addDrawableChild(dropAllButton);
    }

    private void dropAllItems(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        if (player == null || player.currentScreenHandler == null) return;


        for (int i = 5; i <= 8; i++) {
            mc.interactionManager.clickSlot(
                    player.currentScreenHandler.syncId,
                    i,
                    1,
                    SlotActionType.THROW,
                    player
            );
        }


        mc.interactionManager.clickSlot(
                player.currentScreenHandler.syncId,
                45,
                1,
                SlotActionType.THROW,
                player
        );


        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                mc.interactionManager.clickSlot(
                        player.currentScreenHandler.syncId,
                        i,
                        1,
                        SlotActionType.THROW,
                        player
                );
            }
        }


        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                mc.interactionManager.clickSlot(
                        player.currentScreenHandler.syncId,
                        i + 36,
                        1,
                        SlotActionType.THROW,
                        player
                );
            }
        }
    }


    @Unique
    private static volatile boolean deleting = false;
    
    @Unique
    private static final ScheduledExecutorService clearExecutor = Executors.newSingleThreadScheduledExecutor();

    private void deleteAllItems() {
        if (deleting) return;
        deleting = true;
        
        
        for (int i = 0; i < 36; i++) {
            final int slot = i;
            clearExecutor.schedule(() -> {
                if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler != null) {
                    net.minecraft.item.ItemStack stack = mc.player.getInventory().getStack(slot);
                    if (stack.getItem() != Items.AIR) {
                        
                        int containerSlot = (slot < 9) ? (slot + 36) : slot;
                        mc.interactionManager.clickSlot(
                            mc.player.currentScreenHandler.syncId,
                            containerSlot,
                            45,
                            SlotActionType.SWAP,
                            mc.player
                        );
                    }
                }
            }, i * 80L, TimeUnit.MILLISECONDS);
        }
        
        
        clearExecutor.schedule(() -> {
            if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler != null) {
                net.minecraft.item.ItemStack stack = mc.player.getInventory().getStack(40);
                if (stack.getItem() != Items.AIR) {
                    mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        45,
                        45,
                        SlotActionType.SWAP,
                        mc.player
                    );
                }
            }
        }, 36 * 80L, TimeUnit.MILLISECONDS);
        
        
        clearExecutor.schedule(() -> {
            if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler != null) {
                net.minecraft.item.ItemStack stack = mc.player.getInventory().getStack(39);
                if (stack.getItem() != Items.AIR) {
                    mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        5,
                        45,
                        SlotActionType.SWAP,
                        mc.player
                    );
                }
            }
        }, 37 * 80L, TimeUnit.MILLISECONDS);
        
        
        clearExecutor.schedule(() -> {
            if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler != null) {
                net.minecraft.item.ItemStack stack = mc.player.getInventory().getStack(38);
                if (stack.getItem() != Items.AIR) {
                    mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        6,
                        45,
                        SlotActionType.SWAP,
                        mc.player
                    );
                }
            }
        }, 38 * 80L, TimeUnit.MILLISECONDS);
        
        
        clearExecutor.schedule(() -> {
            if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler != null) {
                net.minecraft.item.ItemStack stack = mc.player.getInventory().getStack(37);
                if (stack.getItem() != Items.AIR) {
                    mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        7,
                        45,
                        SlotActionType.SWAP,
                        mc.player
                    );
                }
            }
        }, 39 * 80L, TimeUnit.MILLISECONDS);
        
        
        clearExecutor.schedule(() -> {
            if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler != null) {
                net.minecraft.item.ItemStack stack = mc.player.getInventory().getStack(36);
                if (stack.getItem() != Items.AIR) {
                    mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        8,
                        45,
                        SlotActionType.SWAP,
                        mc.player
                    );
                }
            }
            deleting = false;
        }, 40 * 80L, TimeUnit.MILLISECONDS);
    }
}

