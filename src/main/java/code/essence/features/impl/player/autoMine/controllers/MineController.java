package code.essence.features.impl.player.autoMine.controllers;

import code.essence.Essence;
import code.essence.events.player.InputEvent;
import code.essence.events.player.RotationUpdateEvent;
import code.essence.events.render.DrawEvent;
import code.essence.events.render.WorldRenderEvent;
import code.essence.features.impl.misc.creeperFarm.WalkDirectionUtil;
import code.essence.features.impl.player.autoMine.mine.MineMovementBrainer;
import code.essence.features.impl.player.autoMine.mine.state.MineState;
import code.essence.features.impl.player.autoMine.receivers.AutoMineModule;
import code.essence.features.impl.player.autoMine.region.utils.WorldUtility;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.geometry.Render3D;
import code.essence.utils.features.aura.rotations.impl.SPAngle;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.features.aura.warp.Turns;
import code.essence.utils.features.aura.warp.TurnsConfig;
import code.essence.utils.features.aura.warp.TurnsConnection;
import code.essence.utils.interactions.inv.InventoryResult;
import code.essence.utils.interactions.inv.InventoryTask;
import code.essence.utils.interactions.inv.InventoryToolkit;
import code.essence.utils.math.task.TaskPriority;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * @author nikitavodolaz
 * @since 09.02.2026
 */

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MineController implements QuickImports {
    final RegionController regionController;
    final MineState mineState;
    final MineMovementBrainer brainer;
    final SPAngle spAngle = new SPAngle();

    long lastSwapTime = -1, lastBreakUpdate = -1;
    BlockPos lastBreakingBlockPos = null;

    public MineController(RegionController regionController) {
        this.regionController = regionController;
        this.mineState = new MineState();
        this.brainer = new MineMovementBrainer();
    }

    public void onWorldRender(WorldRenderEvent worldRenderEvent) {

    }

    public void onDraw(DrawEvent drawEvent) {
        double x = (double) mc.getWindow().getScaledWidth() / 2 + 10;
        double y = (double) mc.getWindow().getScaledHeight() / 2 + 38; // Ниже основного HUD

        // Статус копки
        String status = brainer.isPathClear() ? "Walking" : "Digging";
        if (brainer.isStuck()) status = "STUCK — Digging";
        if (regionController.getTargetBlock() == null) status = "No target";

 /*       Fonts.getSize(12, Fonts.Type.REGULAR).drawString(drawEvent.getDrawContext().getMatrices(),
                "Dig: " + status + " | Duration: " + mineState.getDuration(),
                x, y, -1);*/
    }

    public void onTick() {
        mineState.onTick();
        swapPickaxe();

        BlockPos targetBlock = regionController.getTargetBlock();
        brainer.update(targetBlock);

        // Нет таргета — отпускаем ЛКМ и не копаем
        if (targetBlock == null) {
            mc.options.attackKey.setPressed(false);
            return;
        }

        BlockPos blockToLookAt = getLookTarget(targetBlock);
        BlockPos blockToMine = null;
        Direction hitSide = Direction.UP;

        if (mc.crosshairTarget instanceof BlockHitResult blockHitResult) {
            BlockPos hitPos = blockHitResult.getBlockPos();
            double hitDist = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(hitPos));
            double targetDist = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(targetBlock));

            boolean shouldMine = false;

            // 1) Смотрим на препятствие
            if (brainer.getObstacleBlock() != null && hitPos.equals(brainer.getObstacleBlock())) {
                shouldMine = true;
            }
            // 2) Смотрим на таргет-руду
            else if (hitPos.equals(targetBlock)) {
                shouldMine = true;
            }
            // 3) Смотрим на ЛЮБУЮ валидную руду рядом с игроком — ломаем!
            else if (hitDist < 5.0 && RegionController.isValidBlockState(WorldUtility.fromBlockPos(hitPos))) {
                shouldMine = true;
            }
            // 4) Смотрим на блок рядом с таргетом (в пределах 2 блоков) — для первого блока
            else if (hitDist < 5.0 && isNearTarget(hitPos, targetBlock)) {
                shouldMine = true;
            }
            // 5) Stuck или копаем препятствие — ломаем что перед глазами
            else if (hitDist < 5.0 && (brainer.isShouldDigObstacle() || brainer.isStuck())) {
                shouldMine = true;
            }
            // 6) Смотрим на твёрдый блок который БЛИЖЕ чем target — это препятствие
            else if (hitDist < targetDist && hitDist < 5.0 && !WorldUtility.fromBlockPos(hitPos).isAir()) {
                shouldMine = true;
            }
            // 7) Таргет очень близко и мы смотрим на что-то твёрдое — ломаем
            else if (targetDist < 4.5 && hitDist < 5.0 && !WorldUtility.fromBlockPos(hitPos).isAir()) {
                shouldMine = true;
            }

            if (shouldMine) {
                blockToMine = hitPos.toImmutable();
                hitSide = blockHitResult.getSide();
                lastBreakingBlockPos = blockToMine;
                lastBreakUpdate = System.currentTimeMillis();
            }
        } else {
            // crosshairTarget == null (чат/инвентарь открыт) — используем последний блок или таргет
            if (lastBreakingBlockPos != null && System.currentTimeMillis() - lastBreakUpdate < 500) {
                blockToMine = lastBreakingBlockPos;
            } else {
                blockToMine = blockToLookAt;
            }
        }

        if (blockToMine != null && mc.world != null && !mc.world.getBlockState(blockToMine).isAir()) {
            mc.options.attackKey.setPressed(true);
            mc.interactionManager.updateBlockBreakingProgress(blockToMine, hitSide);
            mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            if (System.currentTimeMillis() - lastBreakUpdate > 200) {
                mc.options.attackKey.setPressed(false);
            }
        }
    }

    /**
     * Проверяет, находится ли hitPos рядом с targetBlock (в пределах 2 блоков).
     */
    private boolean isNearTarget(BlockPos hitPos, BlockPos targetBlock) {
        return Math.abs(hitPos.getX() - targetBlock.getX()) <= 2
                && Math.abs(hitPos.getY() - targetBlock.getY()) <= 2
                && Math.abs(hitPos.getZ() - targetBlock.getZ()) <= 2;
    }

    public void onInput(InputEvent inputEvent) {
        BlockPos targetBlock = regionController.getTargetBlock();
        if (targetBlock == null) return;

        BlockPos obstacle = brainer.getObstacleBlock();

        if (obstacle != null && brainer.isShouldDigObstacle()) {
            float clientYaw = TurnsConnection.INSTANCE.getRotation().getYaw();
            Vec3d toObstacle = Vec3d.ofCenter(obstacle).subtract(mc.player.getPos());
            PlayerInput pi = WalkDirectionUtil.fromVec3d(toObstacle, clientYaw);
            inputEvent.setDirectional(pi.forward(), false, false, false);
            return;
        }

        BlockPos moveTarget = brainer.getMoveTarget();
        if (moveTarget == null) moveTarget = targetBlock;

        float clientYaw = TurnsConnection.INSTANCE.getRotation().getYaw();
        Vec3d targetDirection = Vec3d.ofCenter(moveTarget).subtract(mc.player.getPos());
        PlayerInput playerInput = WalkDirectionUtil.fromVec3d(targetDirection, clientYaw);
        inputEvent.setDirectional(playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right());
    }

    public void onRotationUpdate(RotationUpdateEvent rotationUpdateEvent) {
        BlockPos targetBlock = regionController.getTargetBlock();
        if (targetBlock == null) return;

        BlockPos lookAt = getLookTarget(targetBlock);

        Turns turns = MathAngle.calculateAngle(Vec3d.ofCenter(lookAt));
        TurnsConnection.INSTANCE.rotateTo(turns, new TurnsConfig(spAngle, true, false),
                TaskPriority.HIGH_IMPORTANCE_1,
                Essence.getInstance().getModuleProvider().get(AutoMineModule.class));
    }

    private BlockPos getLookTarget(BlockPos targetBlock) {
        BlockPos obstacle = brainer.getObstacleBlock();

        if (obstacle != null && brainer.isShouldDigObstacle()) {
            return obstacle;
        }

        return targetBlock;
    }

    public void onWorldChange() {
        brainer.reset();
        lastBreakingBlockPos = null;
    }

    private void swapPickaxe() {
        if (!(mc.player.getMainHandStack().getItem() instanceof PickaxeItem) && System.currentTimeMillis() - lastSwapTime > 2000) {
            InventoryResult inventoryResult = InventoryToolkit.findInInventory(itemStack -> itemStack.getItem() instanceof PickaxeItem);

            if (inventoryResult == null || inventoryResult.slot() == -1 || inventoryResult.slot() - 36 == mc.player.getInventory().selectedSlot)
                return;

            if (inventoryResult.slot() - 36 != 0) {
                InventoryTask.swapSlots(inventoryResult.slot(), 0);
            }

            if (inventoryResult.slot() - 36 < 9 && inventoryResult.slot() - 36 != mc.player.getInventory().selectedSlot) {
                mc.player.getInventory().selectedSlot = Math.clamp(inventoryResult.slot() - 36, 0, 8);
            }

            lastSwapTime = System.currentTimeMillis();
        }
    }
}