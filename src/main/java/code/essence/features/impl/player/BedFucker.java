package code.essence.features.impl.player;


import code.essence.utils.features.aura.warp.TurnsConnection;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.client.managers.event.types.EventType;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.BooleanSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.display.render.geometry.Render3D;
import code.essence.utils.math.task.TaskPriority;
import code.essence.events.player.RotationUpdateEvent;
import code.essence.events.render.WorldRenderEvent;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.features.aura.warp.TurnsConfig;

import java.util.Comparator;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BedFucker extends Module {

    private BlockPos targetPos;
    private VoxelShape targetShape;

    private final BooleanSetting rotateSetting = new BooleanSetting("Поворот", "Поворачиваться к кровати").setValue(true);
    private final SliderSettings radiusSetting = new SliderSettings("Радиус", "Ломает кровати в радиусе").setValue(4).range(1, 6);

    public BedFucker() {
        super("BedFucker", ModuleCategory.PLAYER);
        setup(rotateSetting, radiusSetting);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (targetPos != null && targetShape != null && !targetShape.isEmpty()) {
            Render3D.drawShape(targetPos, targetShape, ColorAssist.getClientColor(), 2);
        }
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (e.getType() != EventType.PRE || mc.player == null || mc.world == null || mc.interactionManager == null) return;

        targetPos = mc.player.getBlockPos();
        int radius = radiusSetting.getInt();

        targetPos = PlayerInteractionHelper.getCube(mc.player.getBlockPos(), radius, radius, true)
                .stream()
                .filter(this::isBed)
                .min(Comparator.comparingDouble(p -> p.toCenterPos().squaredDistanceTo(mc.player.getPos())))
                .orElse(null);

        if (targetPos != null) {
            if (rotateSetting.isValue()) TurnsConnection.INSTANCE.rotateTo(MathAngle.calculateAngle(targetPos.toCenterPos()), TurnsConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
            BlockState state = mc.world.getBlockState(targetPos);
            targetShape = state.getOutlineShape(mc.world, targetPos);
            mc.interactionManager.updateBlockBreakingProgress(targetPos, Direction.UP);
            mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            targetShape = null;
        }
    }

    private boolean isBed(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.getBlock() instanceof BedBlock;
    }
}


