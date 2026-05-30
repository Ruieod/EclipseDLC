package code.essence.features.impl.movement;

import code.essence.events.player.PlayerTravelEvent;
import code.essence.events.player.SwimmingEvent;
import code.essence.events.player.TickEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.features.aura.warp.TurnsConnection;
import code.essence.utils.input.MoveUtil;
import code.essence.utils.client.logs.Logger;
import code.essence.utils.client.packet.network.Network;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;



import static code.essence.utils.display.interfaces.QuickImports.mc;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WaterSpeed extends Module {

    final SelectSetting modeSetting = new SelectSetting("Режим", "Выберите режим обхода")
            .value("Vanilla", "FunTime", "Wall", "Grim");

    final SelectSetting wallModeSetting = new SelectSetting("Режим от стен", "Режим отталкивания от стен")
            .value("MetaHvH", "FunTime")
            .visible(() -> modeSetting.isSelected("Wall"));

    final SliderSettings ftSpeedSetting = new SliderSettings("Скорость", "Скорость FunTime")
            .range(0.1f, 0.5f)
            .setValue(0.2f)
            .visible(() -> modeSetting.isSelected("FunTime"));

    double wallVerticalBoost = 0.05;
    double wallRadius;
    double wallBoost;

    public WaterSpeed() {
        super("WaterSpeed", "WaterSpeed", ModuleCategory.MOVEMENT);
        setup(modeSetting, wallModeSetting, ftSpeedSetting);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        
        boolean isVanillaServer = Network.isVanilla();
        
        if (isVanillaServer || mc.getNetworkHandler() != null) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == null || player == mc.player) continue;
                
                boolean isSpectator = player.isSpectator();
                
                
                PlayerListEntry entry = mc.getNetworkHandler() != null ? mc.getNetworkHandler().getPlayerListEntry(player.getUuid()) : null;
                if (entry != null) {
                    GameMode gameMode = entry.getGameMode();
                    isSpectator = gameMode == GameMode.SPECTATOR;
                }
                
                if (isVanillaServer || isSpectator) {
                    String playerName = player.getName().getString();
                    String status = isSpectator ? "Spectator" : (isVanillaServer ? "Vanilla сервер" : "");
                    if (!status.isEmpty()) {
                        Logger.info(String.format("[WaterSpeed] Игрок %s: %s (UUID: %s)", playerName, status, player.getUuid()));
                    }
                }
            }
        }

        switch (modeSetting.getSelected()) {
            case "Vanilla" -> handleVanilla();
            case "FunTime" -> handleFunTime();
            case "Wall" -> handleWall();
            case "Grim" -> handleGrim();
        }
    }

    @EventHandler
    public void onSwimming(SwimmingEvent e) {
        if (modeSetting.isSelected("FunTime")) {
            if (mc.options.jumpKey.isPressed()) {
                float pitch = TurnsConnection.INSTANCE.getRotation().getPitch();
                float boost = pitch >= 0 ? MathHelper.clamp(pitch / 45f, 1f, 2f) : 0.8f;
                e.getVector().y = 0.5 * boost;
            } else if (mc.options.sneakKey.isPressed()) {
                e.getVector().y = -0.8;
            }
        }
    }

    @EventHandler
    public void onTravel(PlayerTravelEvent e) {
        if (!e.isPre()) return;
        if (mc.player == null || !mc.player.isTouchingWater()) return;

        if (modeSetting.isSelected("Wall") && mc.player.horizontalCollision) {
            Vec3d motion = e.getMotion();
            e.setMotion(new Vec3d(motion.x * 1.1, motion.y, motion.z * 1.1));
        }
    }

    private void handleVanilla() {
        if (mc.player.isTouchingWater() && !mc.player.isSwimming()) {
            boolean forward = mc.options.forwardKey.isPressed();
            if (forward) {
                Vec3d velocity = mc.player.getVelocity();
                mc.player.setVelocity(
                        velocity.x * 1.00061,
                        velocity.y,
                        velocity.z * 1.00061
                );
            }
        }
    }

    private void handleFunTime() {
        if (mc.player.isTouchingWater() && mc.player.isOnGround()) {
            mc.player.jump();
            mc.player.setVelocity(
                    mc.player.getVelocity().x,
                    ftSpeedSetting.getValue(),
                    mc.player.getVelocity().z
            );
        }
    }

    private void handleWall() {
        if (wallModeSetting.isSelected("FunTime")) {
            wallRadius = 0.35;
            wallBoost = 0.05;
        } else if (wallModeSetting.isSelected("MetaHvH")) {
            wallRadius = 0.35;
            wallBoost = 0.4;
        }

        if (!mc.player.isTouchingWater()) return;
        if (!mc.player.horizontalCollision) return;
        if (!isWaterNearFeet()) return;

        Direction collisionFace = getCollisionFace();
        if (collisionFace == null) return;

        Vec3d pushDir = new Vec3d(
                -collisionFace.getOffsetX(),
                0,
                -collisionFace.getOffsetZ()
        );

        if (pushDir.lengthSquared() < 1.0E-6) return;

        double[] moveDir = calculateDirection(
                mc.player.input.movementForward,
                mc.player.input.movementSideways,
                wallBoost
        );

        Vec3d combined = new Vec3d(moveDir[0], 0, moveDir[1])
                .add(pushDir.normalize().multiply(wallBoost * 0.6));

        Vec3d velocity = mc.player.getVelocity();
        Vec3d result = velocity.add(combined);
        double vertical = Math.max(velocity.y, wallVerticalBoost);

        mc.player.setVelocity(result.x, vertical, result.z);
        mc.player.fallDistance = 0.0f;
    }

    private void handleGrim() {
        if (mc.player == null || mc.world == null) return;
        if (!mc.options.jumpKey.isPressed()) return;

        if (mc.player.isTouchingWater()) {
            BlockPos playerPos = mc.player.getBlockPos();

            double waterLevel = findWaterSurfaceLevel(playerPos);

            double playerEyeY = mc.player.getY() + mc.player.getStandingEyeHeight();

            if (playerEyeY >= waterLevel - 0.35 && playerEyeY <= waterLevel + 0.35) {
                mc.player.setVelocity(
                        mc.player.getVelocity().x,
                        2.5,
                        mc.player.getVelocity().z
                );
                MoveUtil.setMotion(MoveUtil.getMotion() * 5.0);
            } else if (playerEyeY < waterLevel - 0.35) {

                mc.player.setVelocity(
                        mc.player.getVelocity().x,
                        0.12,
                        mc.player.getVelocity().z
                );
            }
        }
    }

    private double findWaterSurfaceLevel(BlockPos startPos) {
        BlockPos checkPos = startPos;

        for (int i = 0; i < 5; i++) {
            FluidState fluidState = mc.world.getFluidState(checkPos);
            FluidState aboveFluid = mc.world.getFluidState(checkPos.up());

            if (fluidState.isIn(FluidTags.WATER) && !aboveFluid.isIn(FluidTags.WATER)) {
                return checkPos.getY() + fluidState.getHeight(mc.world, checkPos);
            }

            checkPos = checkPos.up();
        }

        FluidState fluidState = mc.world.getFluidState(startPos);
        if (fluidState.isIn(FluidTags.WATER)) {
            return startPos.getY() + fluidState.getHeight(mc.world, startPos);
        }

        return startPos.getY() + 1.0;
    }

    private Direction getCollisionFace() {
        Box box = mc.player.getBoundingBox();

        for (Direction dir : Direction.Type.HORIZONTAL) {
            Box shifted = box.offset(
                    dir.getOffsetX() * 0.05,
                    0,
                    dir.getOffsetZ() * 0.05
            );

            boolean hasCollision = mc.world.getBlockCollisions(mc.player, shifted).iterator().hasNext();
            if (hasCollision) {
                return dir;
            }
        }

        return null;
    }

    private boolean isWaterNearFeet() {
        Box box = mc.player.getBoundingBox();

        int minX = MathHelper.floor(box.minX - wallRadius);
        int maxX = MathHelper.floor(box.maxX + wallRadius);
        int minY = MathHelper.floor(box.minY - 0.2);
        int maxY = MathHelper.floor(box.minY + 0.2);
        int minZ = MathHelper.floor(box.minZ - wallRadius);
        int maxZ = MathHelper.floor(box.maxZ + wallRadius);

        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    mutablePos.set(x, y, z);
                    BlockState state = mc.world.getBlockState(mutablePos);

                    if (state.getFluidState().isIn(FluidTags.WATER) ||
                            state.getFluidState().isIn(FluidTags.LAVA)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private double[] calculateDirection(float forward, float sideways, double distance) {
        float yaw = mc.player.getYaw();
        float sinYaw = MathHelper.sin((float) Math.toRadians(yaw + 90));
        float cosYaw = MathHelper.cos((float) Math.toRadians(yaw + 90));

        double xMovement = forward * distance * cosYaw + sideways * distance * sinYaw;
        double zMovement = forward * distance * sinYaw - sideways * distance * cosYaw;

        return new double[]{xMovement, zMovement};
    }
}