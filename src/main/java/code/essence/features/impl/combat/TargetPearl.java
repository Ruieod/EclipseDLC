package code.essence.features.impl.combat;

import code.essence.features.module.setting.implement.RadioSetting;
import code.essence.utils.features.aura.warp.Turns;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.interactions.inv.InventoryFlowManager;
import code.essence.utils.interactions.inv.InventoryTask;
import code.essence.utils.interactions.simulate.PlayerSimulation;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.client.managers.event.types.EventType;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.BindSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.common.repository.friend.FriendUtils;
import code.essence.utils.math.time.StopWatch;
import code.essence.utils.math.task.TaskPriority;
import code.essence.utils.math.script.Script;
import code.essence.events.player.EntitySpawnEvent;
import code.essence.events.player.PostMotionEvent;
import code.essence.events.player.RotationUpdateEvent;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.features.aura.warp.TurnsConfig;
import code.essence.utils.features.aura.warp.TurnsConnection;
import code.essence.utils.features.aura.rotations.impl.SnapAngle;
import code.essence.features.impl.render.Prediction;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.IntStream;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TargetPearl extends Module {
    StopWatch stopWatch = new StopWatch();
    Script script = new Script();



    RadioSetting modeSetting = new RadioSetting("Mode", "When will target pearl work",
            new String[]{"Bind", "Always"}, "Always");

    RadioSetting targetSetting = new RadioSetting("Targets", "Targets for which pearls will be thrown",
            new String[]{"Aura Target", "All"}, "Aura Target");

    BindSetting throwSetting = new BindSetting("Throw","Throw Key").visible(()-> modeSetting.get().equals("Bind"));

    SliderSettings distanceSetting = new SliderSettings("Distance", "Target Pearl Trigger Distance")
            .setValue(10).range(5, 15);

    public TargetPearl() {
        super("TargetPearl","TargetPearl", ModuleCategory.COMBAT);
        setup(modeSetting, targetSetting, throwSetting, distanceSetting);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent e) {
        if (e.getEntity() instanceof EnderPearlEntity pearl) mc.world.getPlayers().stream().filter(p -> p.distanceTo(pearl) <= 3)
                .min(Comparator.comparingDouble(p -> p.distanceTo(pearl))).ifPresent(pearl::setOwner);
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (e.getType() == EventType.PRE) {
            LivingEntity target = Aura.getInstance().getLastTarget();
            Slot slot = InventoryTask.getSlot(Items.ENDER_PEARL);

            if (slot == null || !stopWatch.finished(1000)) return;
            if (modeSetting.get().equals("Bind") && !PlayerInteractionHelper.isKey(throwSetting)) return;
            if (PlayerInteractionHelper.streamEntities().filter(EnderPearlEntity.class::isInstance).map(EnderPearlEntity.class::cast)
                    .anyMatch(pearl -> Objects.equals(pearl.getOwner(), mc.player))) {
                stopWatch.reset();
                return;
            }

            Prediction prediction = Prediction.getInstance();
            PlayerInteractionHelper.streamEntities().filter(EnderPearlEntity.class::isInstance).map(EnderPearlEntity.class::cast)
                    .filter(pearl -> !FriendUtils.isFriend(pearl.getOwner()) && (targetSetting.get().equals("All") || (target != null && target.equals(pearl.getOwner()))))
                    .min(Comparator.comparingDouble(pearl -> TurnsConnection.computeRotationDifference(MathAngle.cameraAngle(), MathAngle.calculateAngle(prediction.calcTrajectory(pearl).getPos()))))
                    .ifPresent(pearl -> {
                        HitResult targetResult = prediction.calcTrajectory(pearl);
                        if (targetResult == null || mc.player.getPos().distanceTo(targetResult.getPos()) <= distanceSetting.getInt()) return;
                        Vec3d eyePos = mc.player.getEyePos().add(mc.player.getPos().subtract(PlayerSimulation.simulateLocalPlayer(1).pos));
                        float yaw = MathAngle.fromVec3d(targetResult.getPos().subtract(eyePos)).getYaw();
                        IntStream.range(-89, 89).mapToObj(pitch -> new Turns(yaw, pitch)).filter(angle -> {
                            HitResult playerResult = prediction.checkTrajectory(angle.toVector(), new EnderPearlEntity(mc.world, mc.player, slot.getStack()), 1.5);
                            return playerResult != null && playerResult.getPos().distanceTo(targetResult.getPos()) <= 3F;
                        }).max(Comparator.comparingDouble(Turns::getPitch)).ifPresent(angle -> {
                            TurnsConnection.INSTANCE.rotateTo(new Turns.VecRotation(angle, angle.toVector()), mc.player, 1, new TurnsConfig(new SnapAngle(),true,true), TaskPriority.HIGH_IMPORTANCE_3, this);
                            InventoryFlowManager.unPressMoveKeys();
                            script.cleanup().addTickStep(0, () -> {
                                InventoryTask.swapAndUse(Items.ENDER_PEARL, angle, false);
                                InventoryFlowManager.enableMoveKeys();
                            });
                            pearl.setOwner(null);
                            stopWatch.reset();
                        });
                    });
        }
    }

    @EventHandler
    public void onPostMotion(PostMotionEvent e) {
        script.update();
    }
}