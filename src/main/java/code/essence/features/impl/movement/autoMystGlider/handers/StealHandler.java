package code.essence.features.impl.movement.autoMystGlider.handers;

import code.essence.events.player.RotationUpdateEvent;
import code.essence.features.impl.movement.autoMystGlider.parsers.SpookyParser;
import code.essence.features.impl.movement.autoMystGlider.receivers.AutoMystGliderModule;
import code.essence.features.impl.movement.autoMystGlider.states.ConnectionState;
import code.essence.features.impl.movement.autoMystGlider.states.GlidePhase;
import code.essence.features.impl.movement.autoMystGlider.states.StealState;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.features.aura.rotations.impl.SPAngle;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.features.aura.warp.Turns;
import code.essence.utils.features.aura.warp.TurnsConfig;
import code.essence.utils.features.aura.warp.TurnsConnection;
import code.essence.utils.interactions.inv.InventoryTask;
import code.essence.utils.math.task.TaskPriority;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Stack;

public class StealHandler implements QuickImports {

    final AutoMystGliderModule module;
    final Stack<StealState> deque = new Stack<>();

    StealState stealState;
    long lastClickTime = -1;
    long lastQuickMoveTime = -1;
    long reconnectTime;

    boolean finished = false;

    public StealHandler(AutoMystGliderModule module) {
        this.module = module;
    }

    public void onTick() {
        if (deque.isEmpty() && !finished && !SpookyParser.isAnarchy(-1)) {
            updateChests(4);
        }

        if (!finished && allChestWasEmpty() && System.currentTimeMillis() - reconnectTime > 5000) {
            finished = true;

            ConnectionState connection = module.getAutoGliderController().getConnectionHandler().getConnection();
            connection.getStealHandler().onDisconnect();
            connection.hub(GlidePhase.HUB_TO_HOME);

            return;
        }

        if (deque.isEmpty()) return;

        applyState();
        if (stealState == null) return;

        if (!isGeneric(stealState.getBlockPos())) {
            deque.remove(stealState);
            stealState = null;
            return;
        }

        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
            stealGenericHandler(handler);

            if (isEmpty(handler)) {
                stealState.setEmpty(true);
                stealState = null;
                mc.currentScreen.close();
            }
        } else if (System.currentTimeMillis() - lastClickTime > 800) {
            openCurrentState();
            lastClickTime = System.currentTimeMillis();
        }
    }

    private void stealGenericHandler(GenericContainerScreenHandler handler) {
        for (Slot slot : handler.slots) {
            if (slot.inventory == mc.player.getInventory()) continue;
            if (!slot.hasStack()) continue;

            if (System.currentTimeMillis() - lastQuickMoveTime > 150) {
                InventoryTask.clickSlot(slot, 0, SlotActionType.QUICK_MOVE, true);
                lastQuickMoveTime = System.currentTimeMillis();
                break;
            }
        }
    }

    private void openCurrentState() {
        BlockPos pos = stealState.getBlockPos();
        Vec3d center = Vec3d.ofCenter(pos).add(0.2, 0.375, 0.2);
        Direction side = Direction.getFacing(center.x - mc.player.getX(), center.y - mc.player.getY(), center.z - mc.player.getZ()).getOpposite();

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(center, side, pos, false));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    public void onRotationUpdate(RotationUpdateEvent event) {
        if (stealState == null) return;

        if (!isGeneric(stealState.getBlockPos())) {
            deque.remove(stealState);
            stealState = null;
            return;
        }

        BlockPos pos = stealState.getBlockPos();

        Turns current = TurnsConnection.INSTANCE.getRotation();
        Turns target = MathAngle.calculateAngle(Vec3d.ofCenter(pos).add(0.2, 0.375, 0.2));
        Turns delta = MathAngle.calculateDelta(current, target);

        Turns end = new Turns(current.getYaw() + delta.getYaw(), current.getPitch() + delta.getPitch());

        TurnsConnection.INSTANCE.rotateTo(end, new TurnsConfig(new SPAngle(), true, false), TaskPriority.HIGH_IMPORTANCE_1, module);
    }

    private boolean allChestWasEmpty() {
        if (deque.isEmpty()) return false;

        for (StealState state : deque) {
            if (!state.isEmpty())
                return false;
        }

        return true;
    }

    private void updateChests(int range) {
        BlockPos base = mc.player.getBlockPos();

        for (BlockPos pos : BlockPos.iterate(base.add(-range, -range, -range), base.add(range, range, range))) {
            BlockState state = mc.world.getBlockState(pos);

            if (state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST) || state.isOf(Blocks.ENDER_CHEST)) {
                if (deque.stream().noneMatch(s -> s.getBlockPos().equals(pos))) {
                    deque.add(new StealState(pos.toImmutable()));
                }
            }
        }
    }

    private void applyState() {
        for (StealState state : deque) {
            if (!state.isEmpty()) {
                stealState = state;
                return;
            }
        }
        stealState = null;
    }

    private boolean isGeneric(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST) || state.isOf(Blocks.ENDER_CHEST);
    }

    private boolean isEmpty(GenericContainerScreenHandler handler) {
        return handler.slots.stream().filter(s -> s.inventory != mc.player.getInventory()).noneMatch(Slot::hasStack);
    }

    public void onConnect() {
        reconnectTime = System.currentTimeMillis();
    }

    public void onDisconnect() {
        reload();
    }

    public void reload() {
        deque.clear();
        stealState = null;
        finished = false;
    }
}
