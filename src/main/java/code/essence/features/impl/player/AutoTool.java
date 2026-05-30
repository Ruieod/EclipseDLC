package code.essence.features.impl.player;


import code.essence.utils.interactions.inv.InventoryTask;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.math.time.StopWatch;
import code.essence.utils.math.script.Script;
import code.essence.events.block.BlockBreakingEvent;
import code.essence.events.keyboard.HotBarScrollEvent;
import code.essence.events.player.HotBarUpdateEvent;
import code.essence.events.player.TickEvent;
import code.essence.events.render.ItemRendererEvent;

import java.util.Comparator;
import java.util.Objects;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoTool extends Module {
    private final StopWatch swap = new StopWatch(), breaking = new StopWatch();
    private final Script script = new Script(), swapBackScript = new Script();
    private ItemStack renderStack;
    private BlockPos lastBreakPos;
    private int previousSelectedSlot = -1; 

    public AutoTool() {
        super("AutoTool", "AutoTool", ModuleCategory.PLAYER);
    }

    @EventHandler
    public void onItemRenderer(ItemRendererEvent e) {
        if (renderStack != null && e.getHand().equals(Hand.MAIN_HAND) && Objects.equals(mc.player, e.getPlayer())) {
            e.setStack(renderStack);
        }
    }

    @EventHandler
    public void onHotBarUpdate(HotBarUpdateEvent e) {
        if (!swapBackScript.isFinished()) e.cancel();
    }

    @EventHandler
    public void onHotBarScroll(HotBarScrollEvent e) {
        if (!swapBackScript.isFinished()) e.cancel();
    }

    @EventHandler
    
    public void onBlockBreaking(BlockBreakingEvent e) {
        breaking.reset();
        lastBreakPos = e.blockPos();
        if (!mc.player.isCreative() && swapBackScript.isFinished() && swap.finished(350)) {
            Slot currentBestSlot = findBestTool(lastBreakPos);
            if (currentBestSlot != null && currentBestSlot != InventoryTask.mainHandSlot()) {
                
                int hotbarSlot = getHotbarSlotFromSlot(currentBestSlot);
                
                if (hotbarSlot != -1) {
                    
                    if (previousSelectedSlot == -1) {
                        previousSelectedSlot = mc.player.getInventory().selectedSlot;
                    }
                    InventoryTask.switchTo(hotbarSlot);
                    swapBackScript.cleanup().addTickStep(0, () -> {
                        if (previousSelectedSlot != -1) {
                            InventoryTask.switchTo(previousSelectedSlot);
                            previousSelectedSlot = -1;
                        }
                    });
                } else {
                    
                    renderStack = mc.player.getMainHandStack();
                    InventoryTask.swapHand(currentBestSlot, Hand.MAIN_HAND, true);
                    swapBackScript.cleanup().addTickStep(0, () -> InventoryTask.swapHand(currentBestSlot, Hand.MAIN_HAND, true, true));
                }
                swap.reset();
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        script.update();
        if (!swapBackScript.isFinished() && swap.finished(350)) {
            Slot currentBestSlot = findBestTool(lastBreakPos);
            if (currentBestSlot != InventoryTask.mainHandSlot() || breaking.finished(100)) {
                
                if (currentBestSlot == InventoryTask.mainHandSlot() || breaking.finished(100)) {
                    if (previousSelectedSlot != -1) {
                        InventoryTask.switchTo(previousSelectedSlot);
                        previousSelectedSlot = -1;
                    }
                }
                script.cleanup().addTickStep(4, () -> renderStack = null);
                swapBackScript.update();
                swap.reset();
            }
        }
    }

    private Slot findBestTool(BlockPos blockPos) {
        BlockState state = mc.world.getBlockState(blockPos);
        if (PlayerInteractionHelper.isAir(state)) return InventoryTask.mainHandSlot();
        
        
        Slot bestHotbarTool = null;
        double bestHotbarSpeed = 0;
        
        for (int i = 0; i < 9; i++) {
            int savedSelectedSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = i;
            Slot hotbarSlot = InventoryTask.mainHandSlot();
            mc.player.getInventory().selectedSlot = savedSelectedSlot;
            
            if (hotbarSlot != null && !hotbarSlot.getStack().isEmpty()) {
                double speed = hotbarSlot.getStack().getMiningSpeedMultiplier(state);
                if (speed != 1 && speed > bestHotbarSpeed) {
                    bestHotbarSpeed = speed;
                    bestHotbarTool = hotbarSlot;
                }
            }
        }

        if (bestHotbarTool != null && bestHotbarSpeed > 1) {
            return bestHotbarTool;
        }
        
        return InventoryTask.slots().sorted(Comparator.comparing(slot -> slot.equals(InventoryTask.mainHandSlot())))
                .filter(s -> s.getStack().getMiningSpeedMultiplier(state) != 1).max(Comparator.comparingDouble(s -> s.getStack().getMiningSpeedMultiplier(state))).orElse(null);
    }
    
    
    private int getHotbarSlotFromSlot(Slot slot) {
        if (slot == null || mc.player == null) return -1;
        
        
        for (int i = 0; i < 9; i++) {
            int savedSelectedSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = i;
            Slot hotbarSlot = InventoryTask.mainHandSlot();
            mc.player.getInventory().selectedSlot = savedSelectedSlot;
            
            if (slot.equals(hotbarSlot)) {
                return i;
            }
        }
        
        return -1;
    }
}