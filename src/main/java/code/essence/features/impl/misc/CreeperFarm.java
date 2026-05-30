/*
package code.essence.features.impl.misc;

import code.essence.events.block.BlockUpdateEvent;
import code.essence.events.player.InputEvent;
import code.essence.events.player.TickEvent;
import code.essence.events.render.DrawEvent;
import code.essence.events.packet.PacketEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.BooleanSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.display.render.font.FontRenderer;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.features.aura.rotations.impl.SPAngle;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.features.aura.warp.Turns;
import code.essence.utils.features.aura.warp.TurnsConfig;
import code.essence.utils.features.aura.warp.TurnsConnection;
import code.essence.utils.input.MoveUtil;
import code.essence.utils.math.task.TaskPriority;
import code.essence.utils.math.time.StopWatch;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import code.essence.utils.client.chat.ChatMessage;


import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class CreeperFarm extends Module implements QuickImports {

    private final BooleanSetting autoJump = new BooleanSetting("Автоматически прыгать", "Прыгать при движении");
    private final BooleanSetting toggleOnFullInventory = new BooleanSetting("Отключаться при полном инвентаре", "Выключить при заполненном инвентаре");
    private final BooleanSetting autoReconnect = new BooleanSetting("Автоматически перезаходить", "Переподключение по таймеру");
    private final BooleanSetting autoSwap = new BooleanSetting("Автоматически свапать на меч", "Переключаться на меч для атаки");
    private final SliderSettings reconnectDelay = new SliderSettings("Секунд для перезахода", "Задержка перед .rct")
            .setValue(60).range(10, 600).step(10).visible(() -> autoReconnect.isValue());

    private static final Direction[] HORIZONTAL_FACES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private final StopWatch timeNoActions = new StopWatch();
    private final StopWatch timeNoUpdateLoadsTemp = new StopWatch();
    private final StopWatch timeNoUpdateFreeRay = new StopWatch();
    private boolean holdJump;
    private boolean holdSneak;
    private boolean holdOnlyForward;
    private boolean canReadBlockChanges;
    private final List<Vec3d> tempListRemoveBlocksPoses = new ArrayList<>();
    private long tempListUpdatesCount;
    private Vec3d updatedLoadedLocRayPos;
    private Vec3d updatedFreeRayEndVec;
    private final Vector3f pos1F = new Vector3f(-Float.MAX_VALUE + 1F, -Float.MAX_VALUE + 1F, -Float.MAX_VALUE + 1F);
    private final Vector3f pos2F = new Vector3f(Float.MAX_VALUE - 1F, Float.MAX_VALUE - 1F, Float.MAX_VALUE - 1F);

    @Setter
    @Getter
    private Vector3f pos1 = new Vector3f(-Float.MAX_VALUE + 1F, -Float.MAX_VALUE + 1F, -Float.MAX_VALUE + 1F);
    @Setter
    @Getter
    private Vector3f pos2 = new Vector3f(Float.MAX_VALUE - 1F, Float.MAX_VALUE - 1F, Float.MAX_VALUE - 1F);

    final SPAngle spAngle = new SPAngle();


    private long enabledAtMs = 0L;
    private int pickedGunpowder = 0;
    private int creepersKilled = 0;
    private int lastGunpowderCount = 0;
    private final Set<Integer> prevCreeperIds = new HashSet<>();
    private Vec3d goalMovementVec;
    private Vec3d calculatedGoalPartH = null;
    private final List<Vec3d> oldPositions = new ArrayList<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public CreeperFarm() {
        super("CreeperFarm", "Бот для автоматического убийства криперов", ModuleCategory.MISC);
        autoJump.setValue(true);
        autoReconnect.setValue(true);
        autoSwap.setValue(true);
        toggleOnFullInventory.setValue(true);
    }

    public static CreeperFarm getInstance() {
        return code.essence.utils.client.Instance.get(CreeperFarm.class);
    }

    public void resetPos1() {
        pos1 = new Vector3f(-Float.MAX_VALUE + 1F, -Float.MAX_VALUE + 1F, -Float.MAX_VALUE + 1F);
    }

    public void resetPos2() {
        pos2 = new Vector3f(Float.MAX_VALUE - 1F, Float.MAX_VALUE - 1F, Float.MAX_VALUE - 1F);
    }

    private long waitActionsMS() {
        return reconnectDelay.getInt() * 1_000L;
    }

    private long waitLoadsTempMS() {
        return 4_000L;
    }

    private long waitUpdateFreeRayMS() {
        return 2_000L;
    }

    private int tempBlockListCap() {
        return 8;
    }

    @Override
    public void activate() {
        timeNoActions.reset();
        canReadBlockChanges = false;
        updateTempPosListOnReset();
        enabledAtMs = System.currentTimeMillis();
        lastGunpowderCount = countItemInInventory(Items.GUNPOWDER);
        pickedGunpowder = 0;
        creepersKilled = 0;
        super.activate();
    }

    @Override
    public void deactivate() {
        timeNoActions.reset();
        canReadBlockChanges = false;
        updateTempPosListOnReset();
        enabledAtMs = 0L;
        super.deactivate();
    }

    private boolean containsInPos(float x, float z) {
        float minX = Math.min(pos1.x, pos2.x);
        float minZ = Math.min(pos1.z, pos2.z);
        float maxX = Math.max(pos1.x, pos2.x);
        float maxZ = Math.max(pos1.z, pos2.z);
        return x > minX && z > minZ && maxX > x && maxZ > z;
    }


    private float[] rotations(Vec3d targetVec) {
        Vec3d playerPos = mc.player.getEyePos();
        double x = targetVec.x - playerPos.x;
        double y = targetVec.y - (playerPos.y + 0.5);
        double z = targetVec.z - playerPos.z;
        double distanceXZ = Math.sqrt(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * (180 / Math.PI)) - 90.0F;
        float pitch = (float) (-(Math.atan2(y, distanceXZ) * (180 / Math.PI)));
        return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.clamp(pitch, -90f, 90f)};
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (pos1.equals(pos1F) || pos2.equals(pos2F)) {
            ChatMessage.brandmessage("Вы не установили необходимые точки для работы CreeperFarm. Используйте .help creeper");
            switchState();
            return;
        }

        holdOnlyForward = false;
        holdJump = false;
        holdSneak = false;
        canReadBlockChanges = false;
        if (mc.player == null || mc.world == null) {
            updateTempPosListOnReset();
            return;
        }

        int curGun = countItemInInventory(Items.GUNPOWDER);
        if (curGun > lastGunpowderCount) pickedGunpowder += (curGun - lastGunpowderCount);
        lastGunpowderCount = curGun;

        if (mc.player.age < 10) {
            timeNoActions.reset();
            updateTempPosListOnReset();
            return;
        }
        canReadBlockChanges = true;

        List<Entity> all = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            if (entity != null) all.add(entity);
        }
        all.sort(Comparator.comparingDouble(a -> a.squaredDistanceTo(mc.player)));

        List<CreeperEntity> creepers = new ArrayList<>();
        for (Entity entity : all) {
            if (entity instanceof CreeperEntity creeper && creeperIsValid(creeper)) {
                creepers.add(creeper);
            }
        }

        Set<Integer> currentIds = new HashSet<>();
        for (CreeperEntity c : creepers) currentIds.add(c.getId());
        if (!prevCreeperIds.isEmpty()) {
            for (Integer id : new HashSet<>(prevCreeperIds)) {
                if (!currentIds.contains(id)) creepersKilled++;
            }
        }
        prevCreeperIds.clear();
        prevCreeperIds.addAll(currentIds);

        List<ItemEntity> items = new ArrayList<>();
        for (Entity ent : all) {
            if (ent instanceof ItemEntity itemEntity && itemIsFarmValid(itemEntity)) {
                items.add(itemEntity);
            }
        }

        if (anyBlowKillSelf(creepers, 10)) {
            if (mc.player != null) mc.player.networkHandler.sendChatMessage(".rct");
            return;
        }

        Entity movementTargetEntity = entityTarget(creepers, items);
        if (movementTargetEntity != null) timeNoActions.reset();

        updateAttack(creepers, 2, false);

        Float currentMoveYaw = null;
        Vec3d tempGoalMovementVec = findBaseMovementGoalVec(movementTargetEntity, creepers);
        if (tempGoalMovementVec != null) goalMovementVec = tempGoalMovementVec;

        if (tempGoalMovementVec == null
                && (goalMovementVec == null || (int) goalMovementVec.distanceTo(mc.player.getPos()) < 5)) {
            float minX = Math.min(pos1.x, pos2.x);
            float minZ = Math.min(pos1.z, pos2.z);
            float maxX = Math.max(pos1.x, pos2.x);
            float maxZ = Math.max(pos1.z, pos2.z);
            float diffX = minX - maxX;
            float diffZ = minZ - maxZ;
            float areaDistance = MathHelper.sqrt(diffX * diffX + diffZ * diffZ);
            float minDistance = areaDistance / 4F;

            if (goalMovementVec != null) {
                List<Vec3d> vectors = new ArrayList<>();
                int i = 0;
                while (goalMovementVec.distanceTo(mc.player.getPos()) < minDistance && i <= 8) {
                    vectors.add(getNearUpdatedBlocksByPluginVec(0, 0));
                    i++;
                }
                if (!vectors.isEmpty()) {
                    vectors.sort(Comparator.comparingDouble(v -> goalMovementVec.distanceTo(mc.player.getPos())));
                    goalMovementVec = vectors.get(0);
                } else {
                    goalMovementVec = getNearUpdatedBlocksByPluginVec(0, 0);
                }
            } else {
                goalMovementVec = getNearUpdatedBlocksByPluginVec(0, 0);
            }
        }

        if (goalMovementVec != null) {
            currentMoveYaw = getYawMovementToGoalVec(goalMovementVec, 1);
        }

        if (goalMovementVec != null && currentMoveYaw != null) {
            Turns currentAngle = TurnsConnection.INSTANCE.getCurrentAngle();
            Turns baseAngle = currentAngle != null ? currentAngle : new Turns(mc.player.getYaw(), mc.player.getPitch());
            float[] rot = rotations(goalMovementVec);
            Turns targetAngle = new Turns(rot[0], rot[1]);
            Turns turns = spAngle.limitAngleChange(baseAngle, targetAngle, goalMovementVec, null);
            TurnsConnection.INSTANCE.setRotation(turns);

            boolean canForward = MathAngle.computeAngleDifference(mc.player.getYaw(), currentMoveYaw.floatValue()) < 45.F && mc.player.squaredDistanceTo(goalMovementVec) > .5D;
            boolean canJump = canForward && mc.player.getMovementSpeed() > .14F && !mc.player.horizontalCollision && mc.player.getActiveHand() != Hand.MAIN_HAND;
            if (canForward) {
                this.holdOnlyForward = true;
                if (canJump) {
                    holdJump = true;
                }
            }
        }

        if (toggleOnFullInventory.isValue()) {
            int emptySlots = 0;
            for (ItemStack stack : mc.player.getInventory().main) {
                if (stack.isEmpty()) emptySlots++;
            }
            if (emptySlots == 0) {
                ChatMessage.brandmessage("/hub");
                switchState();
            }
        }

        if (autoReconnect.isValue() && timeNoActions.finished(waitActionsMS())) {
            if (mc.player != null) mc.player.networkHandler.sendChatMessage(".rct");
        }
    }

    @EventHandler
    public void onInput(InputEvent e) {
        if (holdOnlyForward) {
            e.setForward(1);
            e.setStrafe(0);
        }
        if (holdJump && autoJump.isValue()) e.setJump(true);
        if (holdSneak) e.setSneak(true);
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (e == null || e.getDrawContext() == null) return;
        var matrix = e.getDrawContext().getMatrices();
        long elapsed = enabledAtMs > 0L ? (System.currentTimeMillis() - enabledAtMs) : 0L;
        long sec = elapsed / 1000L;
        long minutes = sec / 60L;
        long seconds = sec % 60L;
        String line1 = "Поднято пороха: " + pickedGunpowder;
        String line2 = String.format("Время работы: %02d:%02d", minutes, seconds);
        String line3 = "Убито криперов: " + creepersKilled;
        FontRenderer font = Fonts.getSize(16, Fonts.Type.SuisseIntlSemiBold);
        font.drawString(matrix, line1, 440f, 290f, -1);
        font.drawString(matrix, line2, 440f, 280f, -1);
        font.drawString(matrix, line3, 440f, 270f, -1);
    }

    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent e) {
        if (!canReadBlockChanges || mc.player == null || e.type() != BlockUpdateEvent.Type.UPDATE) return;
        BlockState state = e.state();
        BlockPos pos = e.pos();
        if (state.getBlock() == Blocks.AIR) {
            int yR = 2;
            boolean yNear = (pos.getY() + yR > mc.player.getY() + 1.F) && (pos.getY() - yR < mc.player.getY() + 1.F);
            if (yNear) {
                BlockState stateOld = mc.world.getBlockState(pos);
                if (stateOld.getBlock() == Blocks.STONE || stateOld.getBlock() == Blocks.BEDROCK) {
                    updateTempPosListFromPacket(pos);
                }
            }
        }
    }

    private Entity entityTarget(List<CreeperEntity> creepers, List<ItemEntity> items) {
        for (CreeperEntity creeper : creepers) {
            if (mc.player.squaredDistanceTo(creeper) < 55 * 55) return creeper;
        }
        for (ItemEntity itemEntity : items) {
            double d = mc.player.squaredDistanceTo(itemEntity);
            if (d < 54 * 54 && d > 0.09) return itemEntity;
        }
        for (CreeperEntity creeper : creepers) {
            if (mc.player.squaredDistanceTo(creeper) < 192 * 192) return creeper;
        }
        for (ItemEntity itemEntity : items) {
            double d = mc.player.squaredDistanceTo(itemEntity);
            if (d < 192 * 192 && d > 0.09) return itemEntity;
        }
        return null;
    }

    private Vec3d findBaseMovementGoalVec(Entity targetToMovement, List<CreeperEntity> allCreepers) {
        if (targetToMovement == null) return null;

        if (targetToMovement instanceof CreeperEntity creeper) {
            float blowupNHandleDistance = 4.9F;
            if (mc.player.distanceTo(creeper) < blowupNHandleDistance
                    || allCreepers.stream().anyMatch(filter -> mc.player.distanceTo(filter) < blowupNHandleDistance)) {
                int rangeXZ = 5;
                BlockPos self = mc.player.getBlockPos();
                for (int xOff = -rangeXZ; xOff < rangeXZ; xOff++) {
                    for (int zOff = -rangeXZ; zOff < rangeXZ; zOff++) {
                        BlockPos mutable = self.add(xOff, 0, zOff);
                        if (mc.world.getBlockState(mutable).getBlock() == Blocks.AIR) {
                            Vec3d centerXZ = new Vec3d(mutable.getX() + 0.5, mutable.getY(), mutable.getZ() + 0.5);
                            boolean safe = true;
                            for (CreeperEntity filter : allCreepers) {
                                if (filter.getPos().distanceTo(centerXZ) <= blowupNHandleDistance) {
                                    safe = false;
                                    break;
                                }
                            }
                            if (safe) {
                                holdSneak = false;
                                return centerXZ;
                            }
                        }
                    }
                }
            }
            return creeper.getPos();
        }

        if (targetToMovement instanceof ItemEntity itemEntity) {
            return itemEntity.getPos();
        }
        return null;
    }

    private Float getYawMovementToGoalVec(@Nullable Vec3d goal, int scanRayRange) {
        if (goal == null) return null;
        float predictValue = 1.5F;
        Vec3d scanAtVec = mc.player.getPos().add(mc.player.getVelocity().x * predictValue, 0.001, mc.player.getVelocity().z * predictValue);
        List<Vec3d> nearableSideRays = new ArrayList<>();
        int radialStepScan = 45;
        double offsetAABB = 0.5;

        for (float yaw = 0.F; yaw < 360.F; yaw += radialStepScan) {
            double radianYaw = Math.toRadians(yaw);
            Vec3d finishRayVec = scanAtVec.add(-Math.sin(radianYaw) * scanRayRange, 0, Math.cos(radianYaw) * scanRayRange);
            var result = mc.world.raycast(new RaycastContext(scanAtVec, finishRayVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player));
            if (result != null && result.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) continue;
            finishRayVec = result != null ? result.getPos() : finishRayVec;
            if (finishRayVec.lengthSquared() < 1) continue;

            boolean loseCheck = false;
            for (Direction direction : HORIZONTAL_FACES) {
                Vec3d hOffsetCheck = new Vec3d(direction.getOffsetX(), 0, direction.getOffsetZ()).multiply(offsetAABB);
                if (mc.world.getBlockState(BlockPos.ofFloored(finishRayVec.add(hOffsetCheck))).getBlock() != Blocks.AIR) {
                    loseCheck = true;
                    break;
                }
            }
            if (!loseCheck) nearableSideRays.add(finishRayVec);
        }

        if (nearableSideRays.isEmpty()) return null;

        calculatedGoalPartH = nearableSideRays.get(0);
        nearableSideRays.removeIf(n -> oldPositions.stream().anyMatch(old -> old.squaredDistanceTo(n) < 0.15 * 0.15));

        if (nearableSideRays.size() > 1) {
            nearableSideRays.sort(Comparator.comparingDouble(a -> a.distanceTo(goal)));
        }
        if (!nearableSideRays.isEmpty()) calculatedGoalPartH = nearableSideRays.get(0);
        if (calculatedGoalPartH == null) return null;

        if (oldPositions.size() > 40) oldPositions.remove(0);
        oldPositions.add(calculatedGoalPartH);

        return MathAngle.fromVec3d(calculatedGoalPartH.subtract(scanAtVec)).getYaw();
    }

    private void updateAttack(List<CreeperEntity> creepers, int multiAttackMax, boolean critHits) {
        if (creepers.isEmpty()) return;
        List<CreeperEntity> list = creepers.stream().filter(c -> mc.player.distanceTo(c) < 9).toList();
        if (list.isEmpty()) return;

        CreeperEntity first = list.get(0);

        if (autoSwap.isValue()) {
            int swordSlot = -1;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (!stack.isEmpty() && stack.getItem() instanceof SwordItem) {
                    swordSlot = i;
                    break;
                }
            }
            if (swordSlot >= 0 && mc.player.getInventory().selectedSlot != swordSlot) {
                mc.player.getInventory().selectedSlot = swordSlot;
            }
        }

        if (critHits) {
            holdJump = !mc.player.isUsingItem();
            if (timeNoActions.finished(200) && !timeNoActions.finished(50)) holdSneak = true;
        }
        if (shouldAttack(first, 50)) {
            mc.interactionManager.attackEntity(mc.player, first);
            mc.player.swingHand(Hand.MAIN_HAND);
            timeNoActions.reset();
            if (multiAttackMax > 0) {
                int dop = 0;
                for (CreeperEntity other : list) {
                    if (other.getId() == first.getId()) continue;
                    if (shouldAttack(other, -60)) {
                        mc.interactionManager.attackEntity(mc.player, other);
                        mc.player.swingHand(Hand.MAIN_HAND);
                        dop++;
                        timeNoActions.reset();
                        if (dop > multiAttackMax) break;
                    }
                }
            }
        }
    }

    private boolean shouldAttack(CreeperEntity creeper, long cooldownMs) {
        if (mc.player.getAttackCooldownProgress(0) < 0.9f) return false;
        return true;
    }

    private boolean creeperIsValid(CreeperEntity creeper) {
        if (!creeper.isAlive() || (int) creeper.getY() != (int) mc.player.getY()) return false;
        if (!entityReachableSeenFloat(creeper)) return false;
        return mc.player.canSee(creeper) && containsInPos((float) creeper.getX(), (float) creeper.getZ());
    }

    private boolean itemIsFarmValid(ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getStack();
        if (stack.isEmpty()) return false;
        if (stack.getItem() != Items.GUNPOWDER && stack.getItem() != Items.EXPERIENCE_BOTTLE) return false;
        if ((int) itemEntity.getY() != (int) mc.player.getY()) return false;
        if (!entityReachableSeenFloat(itemEntity)) return false;
        return containsInPos((float) itemEntity.getX(), (float) itemEntity.getZ());
    }

    private boolean entityReachableSeenFloat(Entity entity) {
        Vec3d from = mc.player.getEyePos();
        Vec3d to = entity.getPos().add(0, 1, 0);
        var result = mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return result == null || result.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK;
    }

    private boolean anyBlowKillSelf(List<CreeperEntity> creepers, int ticksPreDeath) {
        for (CreeperEntity creeper : creepers) {
            if (mc.player.distanceTo(creeper) >= 4.8) continue;
            if (creeper.isIgnited()) return true;
        }
        return false;
    }

    private void updateTempPosListFromPacket(BlockPos addIn) {
        if (addIn == null) return;
        int maxSize = tempBlockListCap();
        while (tempListRemoveBlocksPoses.size() >= Math.max(maxSize, 1)) {
            tempListRemoveBlocksPoses.remove(0);
        }
        tempListRemoveBlocksPoses.add(new Vec3d(addIn.getX() + 0.5, addIn.getY() + 0.5, addIn.getZ() + 0.5));
        tempListUpdatesCount++;
        timeNoUpdateLoadsTemp.reset();
    }

    private void updateTempPosListOnReset() {
        if (!tempListRemoveBlocksPoses.isEmpty() && !canReadBlockChanges) {
            tempListRemoveBlocksPoses.clear();
            tempListUpdatesCount = 0;
        }
    }

    private Vec3d getNearUpdatedBlocksByPluginVec(float yawAccuracy, int maxHRayRange) {
        float minX = Math.min(pos1.x, pos2.x);
        float minZ = Math.min(pos1.z, pos2.z);
        float maxX = Math.max(pos1.x, pos2.x);
        float maxZ = Math.max(pos1.z, pos2.z);
        float gaussianX = (float) secureRandom.nextGaussian();
        float gaussianZ = (float) secureRandom.nextGaussian();
        float clampedX = Math.min(1, Math.max(0, (gaussianX + 3) / 6f));
        float clampedZ = Math.min(1, Math.max(0, (gaussianZ + 3) / 6f));
        float x = minX + clampedX * (maxX - minX);
        float z = minZ + clampedZ * (maxZ - minZ);
        return new Vec3d(x, mc.player.getY(), z);
    }

    private int countItemInInventory(net.minecraft.item.Item item) {
        if (mc.player == null) return 0;
        int total = 0;
        for (ItemStack stack : mc.player.getInventory().main) {
            if (stack != null && !stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
*/
