package code.essence.features.impl.player.autoMine.controllers;

import code.essence.events.player.InputEvent;
import code.essence.events.player.RotationUpdateEvent;
import code.essence.events.render.DrawEvent;
import code.essence.events.render.WorldRenderEvent;
import code.essence.features.impl.misc.creeperFarm.WalkDirectionUtil;
import code.essence.features.impl.movement.autoMystGlider.parsers.SpookyParser;
import code.essence.features.impl.player.autoMine.api.MineApiParser;
import code.essence.features.impl.player.autoMine.state.AutoMineState;
import code.essence.utils.client.chat.ChatMessage;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.features.aura.warp.TurnsConnection;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import static java.lang.Math.abs;

/**
 * Главный контроллер автомайна со стейт-машиной.
 *
 * @author nikitavodolaz
 * @since 08.02.2026
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoMineController implements QuickImports {
    final RegionController regionController;
    final MineController mineController;
    final MineApiParser apiParser;
    final RepairController repairController;

    AutoMineState state = AutoMineState.WAITING_FOR_MINE;

    long stateStartTime = 0;
    long lastCommandTime = 0;

    static final int MINE_TIME_THRESHOLD = 12;

    static final long ANARCHY_LOAD_DELAY = 3000;
    static final long WARP_DELAY = 7000;
    static final long COMMAND_COOLDOWN = 1000;

    /** Радиус (блоки) — если игрок дальше от шахты, варпимся. */
    static final double PROXIMITY_RADIUS = 7.0;

    String currentAnarchy = null;

    boolean connectMessageSent = false;

    boolean alreadyOnAnarchy = false;

    int noTargetTicks = 0;
    static final int NO_TARGET_THRESHOLD = 40;

    public AutoMineController() {
        regionController = new RegionController(RegionController.createRegion(
                new BlockPos(-33, 76, -7),
                new BlockPos(-54, 85, -28)
        ));
        mineController = new MineController(regionController);
        apiParser = new MineApiParser();
        repairController = new RepairController();
    }

    public void onTick() {
        apiParser.fetchAsync();

        switch (state) {
            case WAITING_FOR_MINE -> tickWaitingForMine();
            case JOINING_ANARCHY -> tickJoiningAnarchy();
            case WAITING_ANARCHY_LOAD -> tickWaitingAnarchyLoad();
            case WARPING_TO_MINE -> tickWarpingToMine();
            case WAITING_WARP -> tickWaitingWarp();
            case RUNNING_TO_CENTER -> tickRunningToCenter();
            case MINING -> tickMining();
            case MINE_FINISHED -> tickMineFinished();
            case REPAIRING -> tickRepairing();
        }
    }

    private void tickWaitingForMine() {
        if (apiParser.isMineReady(MINE_TIME_THRESHOLD)) {
            String anarchyCmd = apiParser.getAnarchyCommand();
            if (anarchyCmd != null) {
                if (alreadyOnAnarchy) {
                    applyAnarchyLoadDone();
                    return;
                }
                if (!connectMessageSent) {
                    String mineType = apiParser.getMineType() != null ? apiParser.getMineType() : "?";
                    int timeSec = apiParser.getTimeSeconds();
                    ChatMessage.brandmessage("Подключение: " + anarchyCmd + " (" + mineType + "), шахта через " + timeSec + " сек");
                    connectMessageSent = true;
                }
                setState(AutoMineState.JOINING_ANARCHY);
                return;
            }
        } else {
            connectMessageSent = false;
        }

        boolean inRegion = isPlayerInRegion();
        if (inRegion) {
            regionController.onTick();
            if (regionController.getTargetBlock() != null) {
                setState(AutoMineState.MINING);
            }
        }
    }

    private void tickJoiningAnarchy() {
        String anarchyCmd = apiParser.getAnarchyCommand();

        if (anarchyCmd == null) {
            setState(AutoMineState.WAITING_FOR_MINE);
            return;
        }

        if (canSendCommand()) {
            sendCommand(anarchyCmd);
            currentAnarchy = anarchyCmd;
            setState(AutoMineState.WAITING_ANARCHY_LOAD);
        }
    }

    private void tickWaitingAnarchyLoad() {
        if (getStateElapsed() >= ANARCHY_LOAD_DELAY) {
            applyAnarchyLoadDone();
        }
    }

    /**
     * Вызывается при получении "Вы уже подключены на этот сервер!" —
     * сразу переходим к следующему шагу, не ждём таймер.
     */
    public void notifyAlreadyConnected() {
        alreadyOnAnarchy = true;
        if (state == AutoMineState.WAITING_ANARCHY_LOAD) {
            applyAnarchyLoadDone();
        }
    }

    private void applyAnarchyLoadDone() {
        if (isPlayerInRegion()) {
            setState(AutoMineState.MINING);
        } else {
            setState(AutoMineState.WARPING_TO_MINE);
        }
    }

    private void tickWarpingToMine() {
        if (isPlayerInRegion()) {
            setState(AutoMineState.MINING);
            return;
        }
        if (canSendCommand()) {
            sendCommand("/warp mine");
            setState(AutoMineState.WAITING_WARP);
        }
    }

    private void tickWaitingWarp() {
        if (getStateElapsed() >= WARP_DELAY) {
            setState(AutoMineState.RUNNING_TO_CENTER);
        }
    }

    private void tickRunningToCenter() {
        Vec3d center = regionController.getRegionState().getCenterVec();
        double dist = center.subtract(mc.player.getPos()).horizontalLength();

        if (dist < 11) {
            setState(AutoMineState.MINING);
        }

        regionController.onTick();
    }

    private void tickMining() {
        if (repairController.needsRepair()) {
            setState(AutoMineState.REPAIRING);
            repairController.startRepair();
            return;
        }

        if (!isPlayerNearRegion()) {
            setState(AutoMineState.WARPING_TO_MINE);
            return;
        }

        regionController.onTick();
        mineController.onTick();

        if (regionController.getTargetBlock() == null) {
            noTargetTicks++;
        } else {
            noTargetTicks = 0;
        }

        if (noTargetTicks >= NO_TARGET_THRESHOLD) {
            setState(AutoMineState.MINE_FINISHED);
        }
    }

    private void tickRepairing() {
        if (repairController.tickRepair()) {
            repairController.reset();
            setState(AutoMineState.MINING);
        }
    }

    private void tickMineFinished() {
        noTargetTicks = 0;
        currentAnarchy = null;
        connectMessageSent = false;
        alreadyOnAnarchy = false;
        setState(AutoMineState.WAITING_FOR_MINE);
    }

    public void onInput(InputEvent inputEvent) {
        switch (state) {
            case RUNNING_TO_CENTER -> {
                Vec3d center = regionController.getRegionState().getCenterVec();
                Vec3d direction = center.subtract(mc.player.getPos());
                float yaw = TurnsConnection.INSTANCE.getRotation().getYaw();
                PlayerInput pi = WalkDirectionUtil.fromVec3d(direction, yaw);
                inputEvent.setDirectional(pi.forward(), pi.backward(), pi.left(), pi.right());
            }
            case MINING -> mineController.onInput(inputEvent);
            default -> {
            }
        }
    }

    public void onRotationUpdate(RotationUpdateEvent event) {
        switch (state) {
            case RUNNING_TO_CENTER -> {
                Vec3d center = regionController.getRegionState().getCenterVec();
                code.essence.utils.features.aura.warp.Turns turns =
                        code.essence.utils.features.aura.utils.MathAngle.calculateAngle(center);
                TurnsConnection.INSTANCE.rotateTo(turns,
                        new code.essence.utils.features.aura.warp.TurnsConfig(
                                mineController.getSpAngle(), true, false),
                        code.essence.utils.math.task.TaskPriority.HIGH_IMPORTANCE_1,
                        code.essence.Essence.getInstance().getModuleProvider()
                                .get(code.essence.features.impl.player.autoMine.receivers.AutoMineModule.class));
            }
            case REPAIRING -> { /* pitch задаётся напрямую в RepairController */ }
            case MINING -> mineController.onRotationUpdate(event);
        }
    }

    public void onWorldRender(WorldRenderEvent event) {
        regionController.onWorldRender(event);
        if (state == AutoMineState.MINING) {
            mineController.onWorldRender(event);
        }
    }

    public void onDraw(DrawEvent drawEvent) {
        if (state == AutoMineState.MINING) {
            mineController.onDraw(drawEvent);
        }
    }

    public void onWorldChange() {
        regionController.onWorldChange();
        mineController.onWorldChange();

        if (state == AutoMineState.WAITING_ANARCHY_LOAD) {
            stateStartTime = System.currentTimeMillis();
        }
    }

    private void setState(AutoMineState newState) {
        state = newState;
        stateStartTime = System.currentTimeMillis();
    }

    private long getStateElapsed() {
        return System.currentTimeMillis() - stateStartTime;
    }

    private boolean canSendCommand() {
        return System.currentTimeMillis() - lastCommandTime >= COMMAND_COOLDOWN;
    }

    private void sendCommand(String command) {
        if (mc.player != null && mc.getNetworkHandler() != null) {
            String cmd = command.startsWith("/") ? command.substring(1) : command;
            mc.getNetworkHandler().sendCommand(cmd);
            lastCommandTime = System.currentTimeMillis();
        }
    }

    private boolean isPlayerInRegion() {
        if (mc.player == null) return false;

        Vec3d playerPos = mc.player.getPos();
        Vec3d center = regionController.getRegionState().getCenterVec();

        double dist = abs(center.subtract(playerPos).horizontalLength());
        return dist < 13;
    }

    /**
     * Игрок рядом с шахтой — в пределах PROXIMITY_RADIUS блоков от границ региона.
     */
    private boolean isPlayerNearRegion() {
        if (mc.player == null) return false;

        double px = mc.player.getX();
        double pz = mc.player.getZ();

        BlockPos min = regionController.getRegionState().getMin();
        BlockPos max = regionController.getRegionState().getMax();

        double minX = Math.min(min.getX(), max.getX());
        double maxX = Math.max(min.getX(), max.getX());
        double minZ = Math.min(min.getZ(), max.getZ());
        double maxZ = Math.max(min.getZ(), max.getZ());

        double dx = px < minX ? minX - px : px > maxX ? px - maxX : 0;
        double dz = pz < minZ ? minZ - pz : pz > maxZ ? pz - maxZ : 0;

        double dist = Math.sqrt(dx * dx + dz * dz);
        return dist <= PROXIMITY_RADIUS;
    }
}
