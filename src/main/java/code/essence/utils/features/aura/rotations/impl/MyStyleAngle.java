package code.essence.utils.features.aura.rotations.impl;

import code.essence.utils.features.aura.rotations.constructor.RotateConstructor;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Ротация на основе человеческих факторов, извлечённых из записи движений (НейроАнгл).
 * Без массивов и загрузки — только формулы с подобранными константами.
 */
public class MyStyleAngle extends RotateConstructor {

    // —— из записи: у цели много плато (почти нулевые шаги) ——
    private static final float NEAR_THRESHOLD = 4f;
    private static final float NEAR_STEP_SCALE = 0.12f;
    private static final float FAR_THRESHOLD = 42f;

    // —— макс. шаг: на больших углах быстрее наводка ——
    private static final float MAX_YAW_STEP = 67f;
    private static final float MAX_PITCH_STEP = 32f;
    private static final float PITCH_RATIO = 0.72f;

    // —— замедление к цели (easing), на больших углах почти не режем ——
    private static final float EASE_POWER = 1.5f;
    private static final float EASE_DIST = 50f;

    // —— из записи: джиттер ~0.05–0.15° (несколько частот) ——
    private static final float JITTER_AMP = 0.065f;
    private static final float JITTER_NOISE = 0.11f;
    private static final float JITTER_PITCH_MUL = 0.7f;

    // —— из записи: лёгкая вариация шага каждый тик ——
    private static final float STEP_VAR_MIN = 0.9f;
    private static final float STEP_VAR_MAX = 1.08f;

    public MyStyleAngle() {
        super("МойСтиль");
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        Turns delta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float total = (float) Math.hypot(yawDelta, pitchDelta);

        ThreadLocalRandom r = ThreadLocalRandom.current();

        // Масштаб шага: у цели мелко, на больших углах — ускоренно
        float stepScale;
        if (total < NEAR_THRESHOLD) {
            stepScale = NEAR_STEP_SCALE + r.nextFloat() * 0.06f;
        } else if (total < FAR_THRESHOLD) {
            stepScale = MathHelper.lerp(
                    (total - NEAR_THRESHOLD) / (FAR_THRESHOLD - NEAR_THRESHOLD),
                    0.35f, 0.95f
            );
        } else {
            stepScale = 1.05f + r.nextFloat() * 0.2f;
        }

        float var = STEP_VAR_MIN + r.nextFloat() * (STEP_VAR_MAX - STEP_VAR_MIN);
        float maxYaw = MAX_YAW_STEP * stepScale * var;
        float maxPitch = MAX_PITCH_STEP * stepScale * var;

        float moveYaw = MathHelper.clamp(yawDelta, -maxYaw, maxYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -maxPitch, maxPitch);

        // Easing: на больших углах почти не режем шаг
        float t = Math.min(1f, total / EASE_DIST);
        float ease = 1f - (float) Math.pow(1f - t, EASE_POWER);
        float easeMul = total > 45f ? 1f : (0.5f + 0.5f * ease);
        moveYaw *= easeMul;
        movePitch *= easeMul;

        // Джиттер из записи (несколько частот + шум)
        long time = System.currentTimeMillis();
        float j = (float) (
                Math.sin(time / 108.0) * JITTER_AMP +
                        Math.cos(time / 182.0) * (JITTER_AMP * 0.6f) +
                        (r.nextFloat() - 0.5f) * JITTER_NOISE
        );
        moveYaw += j;
        movePitch += j * JITTER_PITCH_MUL;

        float newYaw = currentAngle.getYaw() + moveYaw;
        float newPitch = MathHelper.clamp(currentAngle.getPitch() + movePitch, -90f, 90f);

        return new Turns(newYaw, newPitch).adjustSensitivity();
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.08, 0.08, 0.08);
    }
}
