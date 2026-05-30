package code.essence.features.impl.combat;


import code.essence.common.repository.friend.FriendUtils;
import code.essence.events.player.MotionEvent;
import code.essence.events.player.PlayerVelocityStrafeEvent;
import code.essence.features.impl.movement.GuiMove;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.MultiSelectSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.interactions.inv.InventoryFlowManager;
import code.essence.utils.interactions.inv.InventoryTask;
import code.essence.utils.interactions.inv.InventoryToolkit;
import code.essence.utils.math.script.Script;
import code.essence.events.packet.PacketEvent;
import code.essence.events.player.EntitySpawnEvent;
import code.essence.events.player.TickEvent;
import code.essence.utils.client.managers.event.EventHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoExplosion extends Module {
    private final Script script = new Script();
    private BlockPos obsPosition;

    private final MultiSelectSetting protections = new MultiSelectSetting("Защищать", "Что не взрывать")
            .value("Себя", "Друзей", "Ресурсы")
            .selected("Себя", "Друзей", "Ресурсы");

    private final SliderSettings itemRange = new SliderSettings("Дистанция до ресурсов", "Минимальное расстояние до ресурсов")
            .range(1.0f, 12.0f)
            .setValue(6.0f).visible(() -> protections.isSelected("Ресурсы"));

    public AutoExplosion() {
        super("AutoExplosion", "AutoExplosion", ModuleCategory.COMBAT);
        setup(protections, itemRange);
    }

    @Override
    public void activate() {
        obsPosition = null;
        super.activate();
    }

    private float x;
    private float y;
    private boolean rot = false;

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof PlayerInteractBlockC2SPacket interact
                && interact.getSequence() != 0
                && script.isFinished()
                && InventoryFlowManager.script.isFinished()) {

            script.addTickStep(0, () -> {
                BlockPos interactPos = interact.getBlockHitResult().getBlockPos();
                BlockPos spawnPos = interactPos.offset(interact.getBlockHitResult().getSide());
                BlockPos obsPos = mc.world.getBlockState(spawnPos).getBlock().equals(Blocks.OBSIDIAN) ? spawnPos
                        : mc.world.getBlockState(interactPos).getBlock().equals(Blocks.OBSIDIAN) ? interactPos : null;

                Slot crystal = InventoryTask.getSlot(Items.END_CRYSTAL);

                if (obsPos != null && crystal != null && isSafePosition(obsPos)) {
                    BlockPos crystalPos = obsPos.up();
                    if (!mc.world.isAir(crystalPos)) return;

                    InventoryFlowManager.addTask(() -> {
                        obsPosition = obsPos;
                        int previousSlot = mc.player.getInventory().selectedSlot;

                        long swapTime = System.currentTimeMillis();

                        InventoryToolkit.switchTo(crystal.getIndex());
                        script.addTickStep(1, () -> {
                            if (mc.player.getInventory().selectedSlot != crystal.getIndex()) return;

                            Vec3d target = crystalPos.toCenterPos().add(0, 0.0, 0);

                            x =  (float) Math.toDegrees(Math.atan2(target.z - mc.player.getZ(), target.x - mc.player.getX())) - 90f;
                            y = (float) -Math.toDegrees(Math.atan2(target.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose())), Math.sqrt(target.x - mc.player.getX() * target.x - mc.player.getX() + target.z - mc.player.getZ() * target.z - mc.player.getZ())));

                            rot = true;

                            script.addTickStep(0, () -> {
                                if (System.currentTimeMillis() - swapTime >= 15) {
                                    PlayerInteractionHelper.sendSequencedPacket(i -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(obsPos.toCenterPos(), Direction.UP, obsPos, false), i));


                                    InventoryToolkit.switchTo(previousSlot);
                                    script.cleanup().addTickStep(6, () -> obsPosition = null);
                                }
                            });
                        });
                    });
                }
            });
        }
    }


    @EventHandler
    public void onMotion(MotionEvent e) {
        if (rot) {
            e.setYaw(x);
            e.setPitch(y);
            rot = false;
        }else{
            x=mc.player.getYaw();
            y=mc.player.getPitch();
        }
    }

    @EventHandler
    public void onPlayerVelocityStrafe(PlayerVelocityStrafeEvent e) {

    }


    private Vec3d fixVelocity(Vec3d currVelocity, Vec3d movementInput, float speed) {
        float yaw = mc.player.getYaw();
        double d = movementInput.lengthSquared();

        if (d < 1.0E-7) {
            return Vec3d.ZERO;
        } else {
            Vec3d vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).multiply(speed);

            float f = MathHelper.sin(yaw * 0.017453292f);
            float g = MathHelper.cos(yaw * 0.017453292f);

            return new Vec3d(vec3d.getX() * g - vec3d.getZ() * f, vec3d.getY(), vec3d.getZ() * g + vec3d.getX() * f);
        }


    }


    @EventHandler
    public void onEntitySpawnEvent(EntitySpawnEvent e) {
        if (e.getEntity() instanceof EndCrystalEntity crystal && obsPosition != null && obsPosition.equals(crystal.getBlockPos().down())) {
            if (isSafeToDamage(crystal)) {
                mc.interactionManager.attackEntity(mc.player, crystal);
            }
            obsPosition = null;
            script.cleanup();
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        script.update();
    }

    private boolean isSafePosition(BlockPos pos) {
        if (protections.isSelected("Себя")) {
            if (mc.player.getY() > pos.getY()) {
                return false;
            }
        }

        if (protections.isSelected("Друзей")) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                if (FriendUtils.isFriend(player)) {
                    if (player.getY() > pos.getY()) {
                        return false;
                    }
                }
            }
        }

        if (protections.isSelected("Ресурсы")) {
            Vec3d crystalPos = pos.up().toCenterPos();
            double range = itemRange.getValue();
            Box box = new Box(crystalPos.x - range, crystalPos.y - range, crystalPos.z - range, crystalPos.x + range, crystalPos.y + range, crystalPos.z + range);
            List<Entity> entities = mc.world.getOtherEntities(mc.player, box);

            for (Entity entity : entities) {
                if (entity instanceof ItemEntity) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isSafeToDamage(EndCrystalEntity crystal) {
        BlockPos crystalBlock = crystal.getBlockPos().down();

        if (protections.isSelected("Себя")) {
            if (mc.player.getY() > crystalBlock.getY()) {
                return false;
            }
        }

        if (protections.isSelected("Друзей")) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                if (FriendUtils.isFriend(player)) {
                    if (player.getY() > crystalBlock.getY()) {
                        return false;
                    }
                }
            }
        }

        if (protections.isSelected("Ресурсы")) {
            Vec3d crystalPos = crystal.getPos();
            double range = itemRange.getValue();
            Box box = new Box(crystalPos.x - range, crystalPos.y - range, crystalPos.z - range, crystalPos.x + range, crystalPos.y + range, crystalPos.z + range);
            List<Entity> entities = mc.world.getOtherEntities(mc.player, box);

            for (Entity entity : entities) {
                if (entity instanceof ItemEntity) {
                    return false;
                }
            }
        }

        return true;
    }
}