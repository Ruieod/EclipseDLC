package code.essence.utils.features.aura.rotations.impl;

import code.essence.Essence;
import code.essence.features.impl.combat.Aura;
import code.essence.utils.features.aura.rotations.constructor.RotateConstructor;
import code.essence.utils.features.aura.striking.StrikeManager;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.features.aura.warp.Turns;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;
import java.util.LinkedList;

public class HWAngle extends RotateConstructor {

    private static final SecureRandom RANDOM = new SecureRandom();

    // === ДИНАМИЧЕСКИЕ ПАРАМЕТРЫ (меняются каждую сессию и периодически) ===

    // Базовые скорости - рандомизируются при инициализации и периодически
    private float baseYawSpeed;
    private float basePitchSpeed;
    private float fastYawSpeed;
    private float fastPitchSpeed;

    // Пороги переключения скоростей
    private float yawThreshold;
    private float pitchThreshold;

    // Параметры jitter
    private float microJitterYaw;
    private float microJitterPitch;

    // История движений для сглаживания
    private final LinkedList<Float> yawHistory = new LinkedList<>();
    private final LinkedList<Float> pitchHistory = new LinkedList<>();
    private int historySize;

    // Состояние
    private long lastMoveTime = 0;
    private long sessionStartTime = 0;
    private long lastProfileChange = 0;
    private long lastPauseTime = 0;
    private long pauseDuration = 0;
    private boolean isPaused = false;

    // Фаза движения
    private int movePhase = 0;
    private float phaseProgress = 0;

    // Симуляция усталости (0.0 - свежий, 1.0 - уставший)
    private float fatigueLevel = 0f;

    // Текущий "профиль" игрока (разные стили наведения)
    private int currentProfile = 0;
    private float profileBlend = 0f;
    private int nextProfile = 0;

    // Случайные отклонения
    private float wanderYaw = 0f;
    private float wanderPitch = 0f;
    private long lastWanderUpdate = 0;

    // Счётчики для антипаттерна
    private int tickCounter = 0;
    private int attackCounter = 0;
    private float lastTotalDelta = 0f;

    // Случайные "ошибки"
    private float errorYaw = 0f;
    private float errorPitch = 0f;
    private long errorEndTime = 0;

    public HWAngle() {
        super("HolyWorld");
        initializeSession();
    }

    /**
     * Инициализация сессии с рандомными параметрами
     */
    private void initializeSession() {
        sessionStartTime = System.currentTimeMillis();
        lastProfileChange = sessionStartTime;
        fatigueLevel = 0f;

        // Выбираем случайный начальный профиль
        currentProfile = RANDOM.nextInt(4);
        nextProfile = (currentProfile + 1 + RANDOM.nextInt(3)) % 4;
        profileBlend = 0f;

        // Инициализируем параметры для текущего профиля
        regenerateParameters();
    }

    /**
     * Генерация новых параметров с рандомизацией
     */
    private void regenerateParameters() {
        // Профили имитируют разные "стили" игроков
        // 0 - агрессивный (быстрые движения, мало точности)
        // 1 - точный (медленные движения, высокая точность)
        // 2 - нервный (много jitter, средняя скорость)
        // 3 - расслабленный (плавные движения, паузы)

        float profileFactor = MathHelper.lerp(profileBlend, getProfileFactor(currentProfile), getProfileFactor(nextProfile));
        float jitterFactor = MathHelper.lerp(profileBlend, getJitterFactor(currentProfile), getJitterFactor(nextProfile));

        // Базовые значения с рандомизацией ±15%
        baseYawSpeed = (25f + randomRange(-5f, 8f)) * profileFactor;
        basePitchSpeed = (11f + randomRange(-3f, 4f)) * profileFactor;
        fastYawSpeed = (48f + randomRange(-8f, 12f)) * profileFactor;
        fastPitchSpeed = (22f + randomRange(-4f, 6f)) * profileFactor;

        // Пороги с рандомизацией
        yawThreshold = 40f + randomRange(-10f, 15f);
        pitchThreshold = 18f + randomRange(-5f, 7f);

        // Jitter с рандомизацией
        microJitterYaw = (1.5f + randomRange(-0.5f, 1.0f)) * jitterFactor;
        microJitterPitch = (0.7f + randomRange(-0.3f, 0.5f)) * jitterFactor;

        // Размер истории (влияет на плавность)
        historySize = 3 + RANDOM.nextInt(3);
    }

    private float getProfileFactor(int profile) {
        return switch (profile) {
            case 0 -> 1.25f + randomRange(-0.1f, 0.15f); // агрессивный
            case 1 -> 0.75f + randomRange(-0.1f, 0.1f);  // точный
            case 2 -> 1.0f + randomRange(-0.15f, 0.15f); // нервный
            case 3 -> 0.85f + randomRange(-0.1f, 0.1f);  // расслабленный
            default -> 1.0f;
        };
    }

    private float getJitterFactor(int profile) {
        return switch (profile) {
            case 0 -> 0.8f + randomRange(-0.1f, 0.2f);   // агрессивный - меньше jitter
            case 1 -> 0.6f + randomRange(-0.1f, 0.15f);  // точный - минимум jitter
            case 2 -> 1.8f + randomRange(-0.2f, 0.4f);   // нервный - много jitter
            case 3 -> 1.0f + randomRange(-0.15f, 0.2f);  // расслабленный - средний
            default -> 1.0f;
        };
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = Essence.getInstance().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        long currentTime = System.currentTimeMillis();

        tickCounter++;

        // === ПЕРИОДИЧЕСКОЕ ОБНОВЛЕНИЕ ПАРАМЕТРОВ ===
        updateDynamicState(currentTime);

        // === ПРОВЕРКА ПАУЗЫ ===
        if (isPaused) {
            if (currentTime < lastPauseTime + pauseDuration) {
                // Во время паузы только минимальный jitter
                float pauseJitterY = randomRange(-0.3f, 0.3f) * (1f + fatigueLevel);
                float pauseJitterP = randomRange(-0.15f, 0.15f) * (1f + fatigueLevel);
                return new Turns(
                        currentAngle.getYaw() + pauseJitterY,
                        MathHelper.clamp(currentAngle.getPitch() + pauseJitterP, -90f, 90f)
                ).adjustSensitivity();
            }
            isPaused = false;
        }

        // === СЛУЧАЙНЫЕ ПАУЗЫ ===
        if (shouldPause(currentTime)) {
            isPaused = true;
            lastPauseTime = currentTime;
            pauseDuration = 50 + RANDOM.nextInt(200); // 50-250ms пауза
            return currentAngle;
        }

        Turns delta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float totalDelta = (float) Math.hypot(yawDelta, pitchDelta);

        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);

        if (canAttack) attackCounter++;

        // Обновляем фазу движения
        updateMovePhase(totalDelta, canAttack);

        // === СЛУЧАЙНЫЕ ОШИБКИ ===
        updateRandomErrors(currentTime, totalDelta);

        // === WANDERING (блуждание прицела) ===
        updateWander(currentTime, canAttack);

        // Рассчитываем скорости с учётом усталости
        float fatigueMult = 1f - (fatigueLevel * 0.25f); // усталость снижает скорость до 25%
        float yawSpeed = calculateYawSpeed(yawDelta, totalDelta, canAttack) * fatigueMult;
        float pitchSpeed = calculatePitchSpeed(pitchDelta, totalDelta, canAttack) * fatigueMult;

        // Применяем easing
        float easedYawDelta = applyHumanEasing(yawDelta, yawSpeed, totalDelta);
        float easedPitchDelta = applyHumanEasing(pitchDelta, pitchSpeed, totalDelta);

        // Добавляем микро-дрожание с учётом усталости
        float fatigueJitterMult = 1f + (fatigueLevel * 0.8f); // усталость увеличивает jitter
        float jitterYaw = calculateMicroJitter(microJitterYaw * fatigueJitterMult, canAttack, currentTime, true);
        float jitterPitch = calculateMicroJitter(microJitterPitch * fatigueJitterMult, canAttack, currentTime, false);

        // Сглаживаем через историю
        easedYawDelta = smoothWithHistory(easedYawDelta, yawHistory);
        easedPitchDelta = smoothWithHistory(easedPitchDelta, pitchHistory);

        // Применяем "ошибку прицеливания"
        if (canAttack && totalDelta < 10.0f) {
            float overshoot = calculateOvershoot(totalDelta);
            easedYawDelta *= (1.0f + overshoot);
            easedPitchDelta *= (1.0f + overshoot);
        }

        // === ДОБАВЛЯЕМ СЛУЧАЙНЫЕ ОТКЛОНЕНИЯ ===
        easedYawDelta += errorYaw + wanderYaw;
        easedPitchDelta += errorPitch + wanderPitch;

        // Формируем итоговый угол
        Turns result = new Turns(
                currentAngle.getYaw() + easedYawDelta + jitterYaw,
                MathHelper.clamp(currentAngle.getPitch() + easedPitchDelta + jitterPitch, -90.0f, 90.0f)
        );

        lastMoveTime = currentTime;
        lastTotalDelta = totalDelta;

        return result.adjustSensitivity();
    }

    /**
     * Обновление динамического состояния (профили, усталость, параметры)
     */
    private void updateDynamicState(long currentTime) {
        long sessionDuration = currentTime - sessionStartTime;

        // Симуляция усталости - нарастает со временем
        // Максимум достигается примерно за 20-25 минут
        fatigueLevel = Math.min(1f, sessionDuration / (1000f * 60f * (20f + randomRange(0, 8))));

        // Смена профиля каждые 2-5 минут с плавным переходом
        long profileInterval = 120000 + RANDOM.nextInt(180000); // 2-5 минут
        if (currentTime - lastProfileChange > profileInterval) {
            currentProfile = nextProfile;
            nextProfile = (currentProfile + 1 + RANDOM.nextInt(3)) % 4;
            profileBlend = 0f;
            lastProfileChange = currentTime;
            regenerateParameters();
        } else {
            // Плавный переход между профилями
            float transitionDuration = 30000f; // 30 секунд на переход
            profileBlend = Math.min(1f, (currentTime - lastProfileChange) / transitionDuration);
        }

        // Периодическая лёгкая корректировка параметров (каждые 30-90 секунд)
        if (tickCounter % (600 + RANDOM.nextInt(1200)) == 0) {
            // Небольшая вариация параметров
            baseYawSpeed *= (0.95f + randomRange(0, 0.1f));
            basePitchSpeed *= (0.95f + randomRange(0, 0.1f));
            microJitterYaw *= (0.9f + randomRange(0, 0.2f));
            microJitterPitch *= (0.9f + randomRange(0, 0.2f));
        }
    }

    /**
     * Проверка необходимости паузы
     */
    private boolean shouldPause(long currentTime) {
        // Базовый шанс паузы ~0.3% за тик, увеличивается с усталостью
        float pauseChance = 0.003f + (fatigueLevel * 0.005f);

        // Увеличиваем шанс если долго не было пауз
        if (currentTime - lastPauseTime > 10000) { // более 10 секунд
            pauseChance *= 2f;
        }

        // Профиль "расслабленный" делает больше пауз
        if (currentProfile == 3) {
            pauseChance *= 1.5f;
        }

        return RANDOM.nextFloat() < pauseChance;
    }

    /**
     * Случайные ошибки в наведении
     */
    private void updateRandomErrors(long currentTime, float totalDelta) {
        if (currentTime > errorEndTime) {
            errorYaw = 0f;
            errorPitch = 0f;

            // Шанс новой ошибки ~0.5%, увеличивается с усталостью
            float errorChance = 0.005f + (fatigueLevel * 0.01f);
            if (RANDOM.nextFloat() < errorChance) {
                // Случайная ошибка - отклонение на 2-8 градусов
                float errorMagnitude = 2f + randomRange(0, 6f);
                float errorAngle = randomRange(0, 360f) * (float)(Math.PI / 180f);
                errorYaw = (float)(Math.cos(errorAngle) * errorMagnitude);
                errorPitch = (float)(Math.sin(errorAngle) * errorMagnitude * 0.5f);
                errorEndTime = currentTime + 100 + RANDOM.nextInt(300); // ошибка длится 100-400ms
            }
        } else {
            // Плавное затухание ошибки
            float decay = 0.92f;
            errorYaw *= decay;
            errorPitch *= decay;
        }
    }

    /**
     * Блуждание прицела (случайные медленные отклонения)
     */
    private void updateWander(long currentTime, boolean canAttack) {
        // Обновляем wandering каждые 500-1500ms
        if (currentTime - lastWanderUpdate > 500 + RANDOM.nextInt(1000)) {
            lastWanderUpdate = currentTime;

            // Когда можем атаковать - меньше блуждания
            float wanderScale = canAttack ? 0.3f : 1.0f;
            wanderScale *= (1f + fatigueLevel * 0.5f); // усталость увеличивает блуждание

            // Плавное изменение к новой цели блуждания
            float targetWanderYaw = randomRange(-1.5f, 1.5f) * wanderScale;
            float targetWanderPitch = randomRange(-0.8f, 0.8f) * wanderScale;

            wanderYaw = MathHelper.lerp(0.3f, wanderYaw, targetWanderYaw);
            wanderPitch = MathHelper.lerp(0.3f, wanderPitch, targetWanderPitch);
        }
    }

    /**
     * Расчёт скорости yaw на основе паттернов движения
     */
    private float calculateYawSpeed(float yawDelta, float totalDelta, boolean canAttack) {
        float absYaw = Math.abs(yawDelta);

        // Добавляем случайный множитель для каждого вызова
        float tickRandomness = 1f + randomRange(-0.08f, 0.08f);

        if (absYaw > yawThreshold) {
            float speedMult = MathHelper.lerp(Math.min(absYaw / 90.0f, 1.0f), 0.65f + randomRange(0, 0.1f), 1.15f + randomRange(0, 0.1f));
            return fastYawSpeed * speedMult * getPhaseMultiplier() * tickRandomness;
        } else {
            float precision = 1.0f - (absYaw / yawThreshold);
            float speedMult = MathHelper.lerp(precision, 0.85f + randomRange(0, 0.1f), 0.35f + randomRange(0, 0.1f));
            return baseYawSpeed * speedMult * getPhaseMultiplier() * tickRandomness;
        }
    }

    /**
     * Расчёт скорости pitch на основе паттернов движения
     */
    private float calculatePitchSpeed(float pitchDelta, float totalDelta, boolean canAttack) {
        float absPitch = Math.abs(pitchDelta);

        // Добавляем случайный множитель для каждого вызова
        float tickRandomness = 1f + randomRange(-0.08f, 0.08f);

        if (absPitch > pitchThreshold) {
            float speedMult = MathHelper.lerp(Math.min(absPitch / 45.0f, 1.0f), 0.55f + randomRange(0, 0.1f), 1.05f + randomRange(0, 0.1f));
            return fastPitchSpeed * speedMult * getPhaseMultiplier() * tickRandomness;
        } else {
            float precision = 1.0f - (absPitch / pitchThreshold);
            float speedMult = MathHelper.lerp(precision, 0.8f + randomRange(0, 0.1f), 0.3f + randomRange(0, 0.1f));
            return basePitchSpeed * speedMult * getPhaseMultiplier() * tickRandomness;
        }
    }

    /**
     * Применение человекоподобного easing
     * Из данных видно: быстрый старт, замедление к цели
     */
    private float applyHumanEasing(float delta, float maxSpeed, float totalDelta) {
        if (Math.abs(delta) < 0.01f) return 0;

        float sign = Math.signum(delta);
        float absDelta = Math.abs(delta);

        // Ограничиваем максимальную скорость с рандомизацией
        float speedVariation = maxSpeed * (0.9f + randomRange(0, 0.2f));
        float clampedDelta = Math.min(absDelta, speedVariation);

        // Easing функция с вариацией экспоненты
        float t = clampedDelta / Math.max(absDelta, 1.0f);
        float easePower = 1.8f + randomRange(0, 0.4f) + (fatigueLevel * 0.3f);
        float eased = 1.0f - (float)Math.pow(1.0f - t, easePower);

        // Случайность зависит от усталости (±5-10%)
        float randomRange = 0.05f + (fatigueLevel * 0.05f);
        float randomFactor = 1.0f + randomRange(-randomRange, randomRange);

        return sign * clampedDelta * eased * randomFactor;
    }

    /**
     * Микро-дрожание характерное для человеческой руки
     */
    private float calculateMicroJitter(float amplitude, boolean canAttack, long time, boolean isYaw) {
        if (!canAttack) {
            amplitude *= 1.4f + randomRange(0, 0.3f);
        }

        // Комбинация синусоид с РАНДОМИЗИРОВАННЫМИ частотами
        // Частоты меняются в зависимости от профиля и усталости
        float freqMult = 1f + (fatigueLevel * 0.3f) + randomRange(-0.1f, 0.1f);
        double baseFreq1 = isYaw ? (80 + currentProfile * 5) : (115 + currentProfile * 7);
        double baseFreq2 = isYaw ? (40 + currentProfile * 3) : (62 + currentProfile * 5);
        double baseFreq3 = isYaw ? (150 + currentProfile * 8) : (195 + currentProfile * 10);

        double phase1 = time / (baseFreq1 * freqMult);
        double phase2 = time / (baseFreq2 * freqMult);
        double phase3 = time / (baseFreq3 * freqMult);
        // Дополнительная случайная фаза
        double phase4 = time / (200.0 + RANDOM.nextInt(100));

        float wave1 = (float) Math.sin(phase1) * (0.45f + randomRange(0, 0.1f));
        float wave2 = (float) Math.sin(phase2) * (0.28f + randomRange(0, 0.08f));
        float wave3 = (float) Math.cos(phase3) * (0.18f + randomRange(0, 0.06f));
        float wave4 = (float) Math.sin(phase4) * (0.12f + randomRange(0, 0.05f));

        float jitter = (wave1 + wave2 + wave3 + wave4) * amplitude;

        // Случайный шум (Perlin-like)
        jitter += randomRange(-amplitude * 0.35f, amplitude * 0.35f);

        // Редкие "рывки" (имитация непроизвольных движений)
        if (RANDOM.nextFloat() < 0.008f) {
            jitter += randomRange(-amplitude * 2f, amplitude * 2f);
        }

        return jitter;
    }

    /**
     * Сглаживание через историю (имитация инерции)
     */
    private float smoothWithHistory(float value, LinkedList<Float> history) {
        history.addLast(value);
        while (history.size() > historySize) {
            history.removeFirst();
        }

        // Взвешенное среднее с рандомизированными весами
        float sum = 0;
        float weightSum = 0;
        int i = 0;
        float weightBase = 1.4f + randomRange(0, 0.2f);
        for (float v : history) {
            float weight = (float) Math.pow(weightBase, i);
            // Небольшая случайность в весах
            weight *= (0.95f + randomRange(0, 0.1f));
            sum += v * weight;
            weightSum += weight;
            i++;
        }

        return sum / weightSum;
    }

    /**
     * Расчёт "промаха" - человек часто слегка перелетает цель
     */
    private float calculateOvershoot(float totalDelta) {
        float proximity = 1.0f - Math.min(totalDelta / 10.0f, 1.0f);
        // Усталость увеличивает overshoot
        float maxOvershoot = (0.12f + fatigueLevel * 0.08f) * proximity;

        // Профиль влияет на шанс перелёта
        float overshootChance = switch (currentProfile) {
            case 0 -> 0.35f; // агрессивный - больше перелётов
            case 1 -> 0.15f; // точный - меньше перелётов
            case 2 -> 0.30f; // нервный - средне
            case 3 -> 0.25f; // расслабленный - средне
            default -> 0.25f;
        };
        overshootChance += fatigueLevel * 0.1f;

        if (RANDOM.nextFloat() < overshootChance) {
            // Иногда недолёт вместо перелёта
            if (RANDOM.nextFloat() < 0.3f) {
                return randomRange(-maxOvershoot * 0.5f, 0);
            }
            return randomRange(0, maxOvershoot);
        }
        return 0;
    }

    /**
     * Обновление фазы движения
     */
    private void updateMovePhase(float totalDelta, boolean canAttack) {
        phaseProgress += 0.05f;

        if (totalDelta > 60.0f) {
            movePhase = 0; // Быстрое наведение
            phaseProgress = 0;
        } else if (totalDelta < 5.0f && canAttack) {
            movePhase = 2; // Удержание на цели
        } else {
            movePhase = 1; // Точное наведение
        }
    }

    /**
     * Множитель скорости в зависимости от фазы
     */
    private float getPhaseMultiplier() {
        float baseMult = switch (movePhase) {
            case 0 -> 1.15f + randomRange(-0.12f, 0.15f); // Быстрая фаза
            case 1 -> 0.8f + randomRange(-0.12f, 0.12f);  // Точная фаза
            case 2 -> 0.35f + randomRange(-0.08f, 0.08f); // Удержание
            default -> 1.0f;
        };

        // Усталость влияет на множитель
        baseMult *= (1f - fatigueLevel * 0.15f);

        return baseMult;
    }

    private float randomRange(float min, float max) {
        return MathHelper.lerp(RANDOM.nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        // Случайность для точки прицеливания зависит от профиля и усталости
        float spread = 0.06f + (fatigueLevel * 0.04f);

        // Профиль "точный" имеет меньший spread
        if (currentProfile == 1) spread *= 0.7f;
        // Профиль "нервный" имеет больший spread
        if (currentProfile == 2) spread *= 1.4f;

        return new Vec3d(
                randomRange(-spread, spread),
                randomRange(-spread * 0.6f, spread * 0.6f),
                randomRange(-spread, spread)
        );
    }

    /**
     * Сброс сессии (вызывать при отключении модуля)
     */
    public void resetSession() {
        initializeSession();
        yawHistory.clear();
        pitchHistory.clear();
        tickCounter = 0;
        attackCounter = 0;
        errorYaw = 0;
        errorPitch = 0;
        wanderYaw = 0;
        wanderPitch = 0;
    }
}