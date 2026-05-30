package code.essence.features.impl.movement.autoMystGlider.handers;

import code.essence.features.impl.movement.autoMystGlider.parsers.SpookyParser;
import code.essence.features.impl.movement.autoMystGlider.receivers.AutoMystGliderModule;
import code.essence.features.impl.movement.autoMystGlider.states.ConnectionState;
import code.essence.features.impl.movement.autoMystGlider.states.GlidePhase;
import code.essence.features.impl.movement.autoMystGlider.states.GlideState;
import code.essence.utils.client.chat.ChatMessage;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.interactions.inv.InventoryTask;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * @author nikitavodolaz
 * @since 04.02.2026
 */

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConnectionHandler implements QuickImports {
    final GlideState glideState;
    final ConnectionState connection;
    final AutoMystGliderModule module;

    long startTime = -1, elytraChangeTime;
    boolean elytraSwaped = false;

    // AT_HOME state
    boolean homeTeleported = false;
    long homeTpTime = -1;
    long lastDropTime = -1;

    // WAITING_COORDS state
    boolean eventDelaySent = false;
    long eventDelayTime = 0;

    // HUB_TO_NEXT state
    boolean apiFetched = false;
    boolean apiReady = false;
    int nextAnarchy = -1;
    long apiFetchTime = 0;

    public ConnectionHandler(GlideState glideState, StealHandler stealHandler, AutoMystGliderModule module) {
        this.glideState = glideState;
        this.module = module;
        this.connection = new ConnectionState(glideState, stealHandler);
    }

    // Для логирования фаз раз в 5 сек (чтобы не спамить)
    long lastPhaseLogTime = 0;

    public void onTick() {
        GlidePhase phase = connection.getPhase();

        // Логируем текущую фазу каждые 5 секунд
        if (System.currentTimeMillis() - lastPhaseLogTime > 5000) {
            long elapsed = System.currentTimeMillis() - connection.getPhaseStartTime();
            ChatMessage.brandmessage("[AutoMyst] Фаза: " + phase + " | connected=" + connection.isConnected() + " | elapsed=" + (elapsed / 1000) + "s");
            lastPhaseLogTime = System.currentTimeMillis();
        }

        switch (phase) {
            case FLYING -> handleFlying();
            case WAITING_REOPEN -> handleWaitingReopen();
            case LOOTING -> {} // StealHandler handles this
            case HUB_TO_HOME -> handleHubToHome();
            case AT_HOME -> handleAtHome();
            case HUB_TO_NEXT -> handleHubToNext();
            case WAITING_COORDS -> handleWaitingCoords();
        }
    }

    // ===================== FLYING =====================
    // Летим + ноклип. /hub только когда: в блоках, на 4+ блока ниже цели и прошла 1 сек

    private void handleFlying() {
        if (connection.successfully() && isPlayerInBlocks() && isPlayer4BlocksBelowTarget() && connection.isConnected()) {
            if (startTime == -1) {
                startTime = System.currentTimeMillis();
            }

            if (System.currentTimeMillis() - startTime >= 700) {
                // Сохраняем текущую анархию и оставшееся время ДО /hub
                connection.setAnarchy(SpookyParser.parseAnarchy());
                connection.setRemainingTime(SpookyParser.parseRemainingTime());
                connection.getStealHandler().onDisconnect();
                connection.hub(GlidePhase.WAITING_REOPEN);

                ChatMessage.brandmessage("Фаза: ожидание реопена (ан " + connection.getAnarchy() + ", ~" + (connection.getRemainingTime() / 1000) + " сек)");
                startTime = -1;
            }
        } else {
            startTime = -1;
        }
    }

    // ===================== WAITING_REOPEN =====================
    // В хабе, ждём. Когда время вышло → /an{текущая} → LOOTING

    private void handleWaitingReopen() {
        if (System.currentTimeMillis() - connection.getLeaveTime() > connection.getRemainingTime()) {
            if (connection.getAnarchy() != -1) {
                connection.getStealHandler().onConnect();
                connection.joinAnarchy(connection.getAnarchy(), GlidePhase.LOOTING);
                ChatMessage.brandmessage("Фаза: заходим на ан" + connection.getAnarchy() + " для лута");
            }
        }
    }

    // ===================== HUB_TO_HOME =====================
    // Залутали → в хабе. Через 3 сек → /an{home} → AT_HOME

    private void handleHubToHome() {
        long elapsed = System.currentTimeMillis() - connection.getPhaseStartTime();

        if (elapsed > 3000) {
            int homeAnarchy = getHomeAnarchy();
            if (homeAnarchy != -1) {
                connection.joinAnarchy(homeAnarchy, GlidePhase.AT_HOME);
                homeTeleported = false;
                homeTpTime = -1;
                lastDropTime = -1;
                ChatMessage.brandmessage("Фаза: заходим на домашнюю ан" + homeAnarchy);
            }
        }
    }

    // ===================== AT_HOME =====================
    // На домашней анархии: /home → скидываем ресы → /hub → HUB_TO_NEXT

    private void handleAtHome() {
        long elapsed = System.currentTimeMillis() - connection.getPhaseStartTime();

        // Ждём 3 сек после захода для загрузки мира
        if (elapsed < 3000) return;

        // Телепортируемся домой
        if (!homeTeleported) {
            String home = module.getHomeName().getText();
            mc.getNetworkHandler().sendCommand("home " + home);
            homeTeleported = true;
            homeTpTime = System.currentTimeMillis();
            ChatMessage.brandmessage("Фаза: телепортируемся /home " + home);
            return;
        }

        // Ждём 3 сек для телепорта
        if (System.currentTimeMillis() - homeTpTime < 3000) return;

        // Скидываем ресы в сундук
        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
            // Shift-click из инвентаря игрока в сундук
            for (Slot slot : handler.slots) {
                if (slot.inventory != mc.player.getInventory()) continue;
                if (!slot.hasStack()) continue;

                if (System.currentTimeMillis() - lastDropTime > 150) {
                    InventoryTask.clickSlot(slot, 0, SlotActionType.QUICK_MOVE, true);
                    lastDropTime = System.currentTimeMillis();
                    break;
                }
            }

            // Проверяем: все слоты игрока пустые?
            boolean allEmpty = handler.slots.stream()
                    .filter(s -> s.inventory == mc.player.getInventory())
                    .noneMatch(Slot::hasStack);

            if (allEmpty) {
                mc.currentScreen.close();
                goToNextPhase();
            }
        } else if (mc.currentScreen == null) {
            // Ищем ближайший сундук и открываем
            BlockPos chest = findNearestChest();
            if (chest != null && System.currentTimeMillis() - lastDropTime > 800) {
                Vec3d center = Vec3d.ofCenter(chest).add(0.2, 0.375, 0.2);
                Direction side = Direction.getFacing(
                        center.x - mc.player.getX(),
                        center.y - mc.player.getY(),
                        center.z - mc.player.getZ()
                ).getOpposite();

                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(center, side, chest, false));
                mc.player.swingHand(Hand.MAIN_HAND);
                lastDropTime = System.currentTimeMillis();
            } else if (chest == null) {
                // Нет сундука — просто идём дальше
                goToNextPhase();
            }
        }

        // Таймаут 30 сек
        if (System.currentTimeMillis() - homeTpTime > 30000) {
            if (mc.currentScreen != null) mc.currentScreen.close();
            goToNextPhase();
        }
    }

    private void goToNextPhase() {
        apiFetched = false;
        apiReady = false;
        nextAnarchy = -1;
        connection.hub(GlidePhase.HUB_TO_NEXT);
        ChatMessage.brandmessage("Фаза: ресы скинуты, ищем следующий ивент");
    }

    // ===================== WAITING_COORDS =====================
    // На анархии, ждём координаты. При заходе пишем /event delay

    boolean rtpSent = false;
    boolean slotClicked = false;

    private void handleWaitingCoords() {
        long elapsed = System.currentTimeMillis() - connection.getPhaseStartTime();

        // Ждём 3 сек после захода для загрузки мира
        if (elapsed < 3000) return;

        // Шаг 1: /event delay + /rtp
        if (!eventDelaySent) {
            mc.getNetworkHandler().sendCommand("event delay");
            mc.getNetworkHandler().sendCommand("rtp");
            eventDelaySent = true;
            eventDelayTime = System.currentTimeMillis();
            rtpSent = false;
            slotClicked = false;
            ChatMessage.brandmessage("[AutoMyst] Отправлено /event delay и /rtp, ждём координаты...");
            return;
        }

        // Шаг 2: Через 500 ms после команд кликаем по 15 слоту (до прихода координат)
        // Слот 15 (1-based) = index 14 в PlayerScreenHandler (первый ряд инвентаря, 6-я ячейка)
        if (!slotClicked && (System.currentTimeMillis() - eventDelayTime) > 100) {
            InventoryTask.clickSlot(13, 0, SlotActionType.QUICK_MOVE, false);
            slotClicked = true;
            ChatMessage.brandmessage("[AutoMyst] Кликнули по слоту 15");
        }
    }

    // ===================== HUB_TO_NEXT =====================
    // В хабе: грузим API → ждём → /an{next} → WAITING_COORDS

    private void handleHubToNext() {
        long elapsed = System.currentTimeMillis() - connection.getPhaseStartTime();

        // Через 2 сек после хаба — запрашиваем API
        if (elapsed > 2000 && !apiFetched) {
            apiFetched = true;
            apiFetchTime = System.currentTimeMillis();
            ChatMessage.brandmessage("[AutoMyst] Запрашиваем API...");
            new Thread(() -> {
                SpookyParser.fetchFromApi();
                nextAnarchy = SpookyParser.getCachedAnarchy();
                apiReady = true;
                ChatMessage.brandmessage("[AutoMyst] API ответил: анархия=" + nextAnarchy);
            }, "SpookyAPI-Fetch").start();
        }

        // API ответило, есть ивент → сразу заходим на анархию
        if (apiReady && nextAnarchy != -1) {
            ChatMessage.brandmessage("[AutoMyst] Заходим на ан" + nextAnarchy + ", ждём координаты из чата");
            eventDelaySent = false;
            rtpSent = false;
            slotClicked = false;
            connection.joinAnarchy(nextAnarchy, GlidePhase.WAITING_COORDS);
            apiFetched = false;
            apiReady = false;
        }

        // API не нашло ивентов — переспрашиваем каждые 30 сек
        if (apiReady && nextAnarchy == -1) {
            long sinceApi = System.currentTimeMillis() - apiFetchTime;
            if (sinceApi > 30_000) {
                apiFetched = false;
                apiReady = false;
                ChatMessage.brandmessage("[AutoMyst] Нет ивентов, повторный запрос API...");
            }
        }
    }

    // ===================== HELPERS =====================

    private int getHomeAnarchy() {
        try {
            return Integer.parseInt(module.getAnarchyText().getText());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean isPlayerInBlocks() {
        BlockPos playerPos = mc.player.getBlockPos();
        return mc.player.getWorld().getBlockState(playerPos).isSolid();
    }

    /** Игрок находится на 4 или более блоков ниже целевой точки (Y цели - 4). */
    private boolean isPlayer4BlocksBelowTarget() {
        double targetY = glideState.vec3d().getY();
        double playerY = mc.player.getY();
        return playerY <= targetY - 3;
    }

    private BlockPos findNearestChest() {
        BlockPos playerPos = mc.player.getBlockPos();
        int range = 5;
        BlockPos.Mutable nearest = new BlockPos.Mutable(0, 0, 0);
        double nearestDistSq = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.iterate(playerPos.add(-range, -range, -range), playerPos.add(range, range, range))) {
            var state = mc.world.getBlockState(pos);
            if (state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST) || state.isOf(Blocks.ENDER_CHEST)) {
                double distSq = mc.player.squaredDistanceTo(Vec3d.ofCenter(pos));
                if (distSq < nearestDistSq && distSq <= 20.25) {
                    nearestDistSq = distSq;
                    nearest.set(pos);
                }
            }
        }
        return nearestDistSq < Double.MAX_VALUE ? nearest.toImmutable() : null;
    }

    public void reload() {
        connection.reload();
        elytraSwaped = false;
        elytraChangeTime = -1;
        homeTeleported = false;
        homeTpTime = -1;
        lastDropTime = -1;
        apiFetched = false;
        apiReady = false;
        nextAnarchy = -1;
        eventDelaySent = false;
        rtpSent = false;
        slotClicked = false;
    }
}
