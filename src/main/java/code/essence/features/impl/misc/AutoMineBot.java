package code.essence.features.impl.misc;

import code.essence.display.hud.Notifications;
import code.essence.events.packet.PacketEvent;
import code.essence.events.player.InputEvent;
import code.essence.events.player.TickEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.client.chat.ChatMessage;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.math.time.StopWatch;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static code.essence.utils.display.interfaces.QuickImports.mc;

public class AutoMineBot extends Module {
    private static final String API_URL = "http://85.208.139.128:8000/api/mines/nearest";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final StopWatch commandDelay = new StopWatch();
    private final StopWatch warpDelay = new StopWatch();
    private final StopWatch replaceCommandDelay = new StopWatch();
    private final StopWatch updateTimer = new StopWatch();
    private final StopWatch doneBuildingTimer = new StopWatch();
    private final StopWatch baritoneWorkTimer = new StopWatch();
    private final StopWatch anarchyTimer = new StopWatch();

    private static final long COMMAND_DELAY_MS = 500L;
    private static final long WARP_DELAY_MS = 7100L;
    private static final long REPLACE_COMMAND_INTERVAL_MS = 1000L;
    private static final long UPDATE_INTERVAL_MS = 2000L;
    private static final long DONE_BUILDING_TIMEOUT_MS = 5000L;
    private static final long BARITONE_WORK_TIMEOUT_MS = 2000L;
    private static final long ANARCHY_TIMEOUT_MS = 40000L;
    private static final double NEARBY_RADIUS = 30.0;


    private static final BlockPos SEL_1 = new BlockPos(-96, 84, 38);
    private static final BlockPos SEL_2 = new BlockPos(-74, 77, 16);

    private enum State {
        IDLE,
        SETTING_SEL,
        GETTING_ANARCHY,
        TELEPORTING,
        WAITING_WARP,
        WARPING,
        WAITING_REPLACE,
        REPLACING,
        WAITING_DONE,
        SWITCHING
    }

    private static final Direction[] HORIZONTAL_FACES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private State currentState = State.IDLE;
    private String currentAnarchy = "";
    private boolean sel1Set = false;
    private boolean sel2Set = false;
    private boolean baritoneWorking = false;
    private boolean doneBuildingReceived = false;
    private long lastDoneBuildingTime = 0;

    /** Движение с обходом препятствий (как в CreeperFarm). */
    private Vec3d goalMovementVec = null;
    private Vec3d calculatedGoalPartH = null;
    private final List<Vec3d> oldPositions = new ArrayList<>();
    private boolean holdForward = false;
    private boolean holdJump = false;

    public AutoMineBot() {
        super("AutoMineBot", "AutoMineBot", ModuleCategory.MISC);
        commandDelay.reset();
        warpDelay.reset();
        replaceCommandDelay.reset();
        updateTimer.reset();
    }

    @Override
    public void activate() {
        super.activate();
        ChatMessage.brandmessage("AutoMineBot включен. Начинаю работу...");
        currentState = State.SETTING_SEL;
        currentAnarchy = "";
        sel1Set = false;
        sel2Set = false;
        baritoneWorking = false;
        doneBuildingReceived = false;
        lastDoneBuildingTime = 0;
        commandDelay.reset();
        warpDelay.reset();
        replaceCommandDelay.reset();
        updateTimer.reset();
        doneBuildingTimer.reset();
        baritoneWorkTimer.reset();
        anarchyTimer.reset();
    }

    @Override
    public void deactivate() {
        super.deactivate();
        ChatMessage.brandmessage("AutoMineBot выключен.");
        currentState = State.IDLE;
    }

    @EventHandler
    public void onInput(InputEvent e) {
        if (holdForward) {
            e.setForward(1);
            e.setStrafe(0);
        }
        if (holdJump) e.setJump(true);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        holdForward = false;
        holdJump = false;

        // Движение к sel-области с обходом препятствий (в WARPING/WAITING_WARP)
        if (currentState == State.WARPING || currentState == State.WAITING_WARP) {
            Vec3d selCenter = Vec3d.ofCenter(SEL_1).add(Vec3d.ofCenter(SEL_2)).multiply(0.5);
            goalMovementVec = selCenter;
            moveToGoal(goalMovementVec);
        }

        switch (currentState) {
            case IDLE:
                break;

            case SETTING_SEL:
                if (!sel1Set) {
                    sendCommand("#sel 1 -96 84 38");
                    sendCommand("#set smoothLook true");
                    sendCommand("#set smoothLookTicks 20");
                    sel1Set = true;
                    updateTimer.reset();
                } else if (!sel2Set && updateTimer.finished(500L)) {
                    sendCommand("#sel 2 -74 77 16");
                    sendCommand("#set blocksToAvoidBreaking andesite,stone_bricks");
                    sel2Set = true;
                    updateTimer.reset();
                } else if (sel1Set && sel2Set && updateTimer.finished(500L)) {
                    currentState = State.GETTING_ANARCHY;
                    updateTimer.reset();
                }
                break;

            case GETTING_ANARCHY:
                if (updateTimer.finished(UPDATE_INTERVAL_MS)) {
                    updateTimer.reset();
                    fetchNearestAnarchy();
                }
                break;

            case TELEPORTING:
                if (commandDelay.finished(COMMAND_DELAY_MS)) {
                    commandDelay.reset();
                    currentState = State.WAITING_WARP;
                    sendCommand("#set allowPlace false");
                    sendCommand("/warp mine");
                    warpDelay.reset();

                    anarchyTimer.reset();
                }
                break;

            case WAITING_WARP:

                if (isNearbySel()) {

                    currentState = State.REPLACING;
                    replaceCommandDelay.reset();
                } else {

                    currentState = State.WARPING;
                }
                break;

            case WARPING:
                if (warpDelay.finished(WARP_DELAY_MS)) {
                    warpDelay.reset();
                    currentState = State.REPLACING;
                    replaceCommandDelay.reset();
                }
                break;

            case REPLACING:

                if (anarchyTimer.finished(ANARCHY_TIMEOUT_MS)) {
                    currentState = State.SWITCHING;
                    anarchyTimer.reset();
                    break;
                }


                if (baritoneWorking && baritoneWorkTimer.finished(BARITONE_WORK_TIMEOUT_MS)) {
                    baritoneWorking = false;
                    baritoneWorkTimer.reset();
                }


                if (replaceCommandDelay.finished(REPLACE_COMMAND_INTERVAL_MS)) {
                    replaceCommandDelay.reset();
                    if (!baritoneWorking) {
                        sendCommand("#sel replace minecraft:diamond_ore minecraft:air");

                        baritoneWorking = true;
                        baritoneWorkTimer.reset();
                    }
                }

                break;

            case WAITING_DONE:

                if (anarchyTimer.finished(ANARCHY_TIMEOUT_MS)) {
                    currentState = State.SWITCHING;
                    anarchyTimer.reset();
                    break;
                }


                if (doneBuildingReceived && lastDoneBuildingTime > 0) {
                    long timeSinceLastDone = System.currentTimeMillis() - lastDoneBuildingTime;
                    if (timeSinceLastDone > DONE_BUILDING_TIMEOUT_MS) {

                        currentState = State.SWITCHING;
                        doneBuildingReceived = false;
                        baritoneWorking = false;
                        lastDoneBuildingTime = 0;
                    }
                }
                break;

            case SWITCHING:
                if (updateTimer.finished(1000L)) {
                    updateTimer.reset();
                    currentState = State.GETTING_ANARCHY;
                    currentAnarchy = "";
                }
                break;
        }
    }

    @EventHandler
    public void onPacket(PacketEvent e) {

        if (e.getType() != PacketEvent.Type.RECEIVE) return;

        if (e.getPacket() instanceof GameMessageS2CPacket gameMessage) {
            String message = gameMessage.content().getString();


            if (message.contains("[Baritone]")) {

                if (message.contains("Filling now")) {
                    if (currentState == State.REPLACING) {
                        baritoneWorking = true;
                        baritoneWorkTimer.reset();
                    }
                }


                if (message.contains("Done building")) {
                    long currentTime = System.currentTimeMillis();
                    baritoneWorking = false;

                    if (currentState == State.REPLACING) {

                        currentState = State.WAITING_DONE;
                        doneBuildingReceived = true;
                        lastDoneBuildingTime = currentTime;
                        doneBuildingTimer.reset();
                    } else if (currentState == State.WAITING_DONE) {

                        lastDoneBuildingTime = currentTime;
                        doneBuildingTimer.reset();
                    }
                }
            }
        }
    }

    private boolean isNearbySel() {
        if (mc.player == null) return false;
        BlockPos playerPos = mc.player.getBlockPos();
        Vec3d playerVec = mc.player.getPos();


        double distToSel1 = playerVec.distanceTo(Vec3d.ofCenter(SEL_1));

        double distToSel2 = playerVec.distanceTo(Vec3d.ofCenter(SEL_2));


        boolean inSel1Area = distToSel1 <= NEARBY_RADIUS;
        boolean inSel2Area = distToSel2 <= NEARBY_RADIUS;


        int minX = Math.min(SEL_1.getX(), SEL_2.getX());
        int maxX = Math.max(SEL_1.getX(), SEL_2.getX());
        int minY = Math.min(SEL_1.getY(), SEL_2.getY());
        int maxY = Math.max(SEL_1.getY(), SEL_2.getY());
        int minZ = Math.min(SEL_1.getZ(), SEL_2.getZ());
        int maxZ = Math.max(SEL_1.getZ(), SEL_2.getZ());

        boolean inSelArea = playerPos.getX() >= minX - NEARBY_RADIUS && playerPos.getX() <= maxX + NEARBY_RADIUS &&
                playerPos.getY() >= minY - NEARBY_RADIUS && playerPos.getY() <= maxY + NEARBY_RADIUS &&
                playerPos.getZ() >= minZ - NEARBY_RADIUS && playerPos.getZ() <= maxZ + NEARBY_RADIUS;

        return inSel1Area || inSel2Area || inSelArea;
    }

    private void fetchNearestAnarchy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                String responseBody = response.body();
                                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                                boolean found = json.get("found").getAsBoolean();
                                if (found) {
                                    String mineInfo = json.get("mine").getAsString();

                                    Pattern pattern = Pattern.compile("/an(\\d+)");
                                    Matcher matcher = pattern.matcher(mineInfo);
                                    if (matcher.find()) {
                                        String anarchyNum = matcher.group(1);


                                        String timeToAutoMine = "";
                                        Pattern timePattern = Pattern.compile("(\\d{1,2}:\\d{2})");
                                        Matcher timeMatcher = timePattern.matcher(mineInfo);
                                        if (timeMatcher.find()) {
                                            timeToAutoMine = timeMatcher.group(1);
                                        }

                                        final String finalAnarchyNum = anarchyNum;
                                        final String finalTimeToAutoMine = timeToAutoMine;
                                        mc.execute(() -> {
                                            if (!finalAnarchyNum.equals(currentAnarchy)) {
                                                currentAnarchy = finalAnarchyNum;
                                                currentState = State.TELEPORTING;
                                                sendCommand("/an" + finalAnarchyNum);
                                                commandDelay.reset();
                                                if (!finalTimeToAutoMine.isEmpty()) {
                                                    ChatMessage.brandmessage("Телепортируюсь на анархию " + finalAnarchyNum + ", до авто-шахты " + finalTimeToAutoMine);
                                                    Notifications.getInstance().addList("Телепортируюсь на анархию " + finalAnarchyNum + ", до авто-шахты " + finalTimeToAutoMine + " секунд", 5000);
                                                } else {
                                                    ChatMessage.brandmessage("Телепортируюсь на анархию " + finalAnarchyNum);
                                                    Notifications.getInstance().addList("Телепортируюсь на анархию " + finalAnarchyNum, 3000);
                                                }
                                            }
                                        });
                                    }
                                }
                            } catch (Exception ex) {

                            }
                        }
                    })
                    .exceptionally(ex -> null);
        } catch (Exception ex) {

        }
    }

    /** Двигаемся к цели с обходом препятствий (как в CreeperFarm). */
    private void moveToGoal(Vec3d goal) {
        if (goal == null || mc.player == null || mc.world == null) return;

        Float moveYaw = getYawMovementToGoalVec(goal, 2);
        if (moveYaw == null) return;

        boolean canForward = MathAngle.computeAngleDifference(mc.player.getYaw(), moveYaw) < 45f
                && mc.player.squaredDistanceTo(goal) > 0.5;
        boolean canJump = canForward && mc.player.getMovementSpeed() > 0.14f
                && mc.player.horizontalCollision
                && mc.player.getActiveHand() != Hand.MAIN_HAND;

        if (canForward) {
            holdForward = true;
            float[] rot = rotationsToVec(goal);
            mc.player.setYaw(MathHelper.lerpAngleDegrees(0.35f, mc.player.getYaw(), rot[0]));
            if (canJump) holdJump = true;
        }
    }

    /** Находит свободное направление ближайшее к цели (рейкаст 360° с шагом 45°). */
    private Float getYawMovementToGoalVec(Vec3d goal, int scanRayRange) {
        if (goal == null) return null;
        float predictValue = 1.5f;
        Vec3d scanAt = mc.player.getPos().add(
                mc.player.getVelocity().x * predictValue, 0.001,
                mc.player.getVelocity().z * predictValue);
        List<Vec3d> freeRays = new ArrayList<>();
        int stepDeg = 45;
        double offsetAABB = 0.5;

        for (float yaw = 0f; yaw < 360f; yaw += stepDeg) {
            double rad = Math.toRadians(yaw);
            Vec3d end = scanAt.add(-Math.sin(rad) * scanRayRange, 0, Math.cos(rad) * scanRayRange);
            var result = mc.world.raycast(new RaycastContext(scanAt, end,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, mc.player));
            if (result != null && result.getType() == HitResult.Type.BLOCK) continue;
            Vec3d hitPos = result != null ? result.getPos() : end;
            if (hitPos.lengthSquared() < 1) continue;

            boolean blocked = false;
            for (Direction dir : HORIZONTAL_FACES) {
                Vec3d off = new Vec3d(dir.getOffsetX(), 0, dir.getOffsetZ()).multiply(offsetAABB);
                if (!mc.world.getBlockState(BlockPos.ofFloored(hitPos.add(off))).isAir()) {
                    blocked = true;
                    break;
                }
            }
            if (!blocked) freeRays.add(hitPos);
        }

        if (freeRays.isEmpty()) return null;

        calculatedGoalPartH = freeRays.get(0);
        freeRays.removeIf(n -> oldPositions.stream().anyMatch(old -> old.squaredDistanceTo(n) < 0.15 * 0.15));

        if (freeRays.size() > 1) {
            freeRays.sort(Comparator.comparingDouble(a -> a.distanceTo(goal)));
        }
        if (!freeRays.isEmpty()) calculatedGoalPartH = freeRays.get(0);
        if (calculatedGoalPartH == null) return null;

        if (oldPositions.size() > 40) oldPositions.remove(0);
        oldPositions.add(calculatedGoalPartH);

        return MathAngle.fromVec3d(calculatedGoalPartH.subtract(scanAt)).getYaw();
    }

    private float[] rotationsToVec(Vec3d target) {
        Vec3d eye = mc.player.getEyePos();
        double dx = target.x - eye.x;
        double dy = target.y - (eye.y + 0.5);
        double dz = target.z - eye.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90f;
        float pitch = (float) (-(Math.atan2(dy, distXZ) * (180.0 / Math.PI)));
        return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.clamp(pitch, -90f, 90f)};
    }

    private void sendCommand(String command) {
        if (mc.player != null && mc.player.networkHandler != null) {
            mc.player.networkHandler.sendChatMessage(command);
        }
    }
}