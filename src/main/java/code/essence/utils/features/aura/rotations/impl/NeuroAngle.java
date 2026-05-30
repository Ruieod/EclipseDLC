package code.essence.utils.features.aura.rotations.impl;

import code.essence.utils.client.chat.ChatMessage;
import code.essence.utils.features.aura.rotations.constructor.RotateConstructor;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.features.aura.warp.Turns;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Режим наводки: записывает движения игрока (yaw/pitch) и воспроизводит их при наводке на цель.
 */
public class NeuroAngle extends RotateConstructor {

    private static final float SPEED_FACTOR = 10.0F;

    /**
     * Постоянная ротация: вставь сюда массив из консоли (после записи НейроАнгл выводится в лог).
     * Скопируй из лога блок вида: { 0.8496f, 0.3685f }, { 0.0000f, 0.0000f }, ... — и вставь ниже.
     * Если массив не пустой, эта ротация будет использоваться всегда (без повторной записи).
     */
    private static final float[][] NEURO_PRESET_ROTATION = new float[][] {
        // Вставь сюда дельты из консоли (только строки { yaw, pitch },)
        // Например после записи 1266 дельт — вставь все 1266 строк сюда.
    };

    static {
        if (NEURO_PRESET_ROTATION != null && NEURO_PRESET_ROTATION.length > 0) {
            loadPresetDeltas(NEURO_PRESET_ROTATION);
        }
    }

    private static final List<Turns> samples = new ArrayList<>();
    private static final List<Turns> recordedDeltas = new ArrayList<>();
    private static boolean recording = false;
    private static int playbackIndex = 0;
    private static boolean fileLoadAttempted = false;

    public NeuroAngle() {
        super("NeuroAngle");
    }

    public static boolean isRecording() {
        return recording;
    }

    public static void startRecording() {
        recording = true;
        samples.clear();
        recordedDeltas.clear();
        playbackIndex = 0;
    }

    public static void stopRecording() {
        recording = false;
        if (samples.size() >= 2) {
            recordedDeltas.clear();
            for (int i = 0; i < samples.size() - 1; i++) {
                Turns a = samples.get(i);
                Turns b = samples.get(i + 1);
                recordedDeltas.add(new Turns(
                        MathHelper.wrapDegrees(b.getYaw() - a.getYaw()),
                        b.getPitch() - a.getPitch()
                ));
            }
            printRecordedRotationToChat();
            writeRecordedYawPitchToFile();
        }
    }

    /** Пишет записанные yaw и pitch в текстовый файл (для блокнота). Одна строка = одна запись: "yaw pitch". */
    public static void writeRecordedYawPitchToFile() {
        if (samples.isEmpty()) return;
        File dir = MinecraftClient.getInstance().runDirectory;
        if (dir == null) return;
        File file = new File(dir, "neuro_yaw_pitch.txt");
        try (FileWriter w = new FileWriter(file)) {
            w.write("# Записанные yaw pitch (одна строка = одна запись)\n");
            w.write("# Всего записей: " + samples.size() + "\n");
            for (Turns t : samples) {
                w.write(String.format("%.6f %.6f\n", t.getYaw(), t.getPitch()));
            }
        } catch (IOException e) {
            System.err.println("[NeuroAngle] Не удалось записать в файл: " + e.getMessage());
        }
        ChatMessage.brandmessage("§e[НейроАнгл] §fYaw/pitch записаны в файл: " + file.getAbsolutePath());
    }

    /** Выводит записанную ротацию в чат и консоль (вызывается при выкл/вкл записи и при вкл/выкл Aura). */
    public static void printRecordedRotationToChat() {
        if (recordedDeltas.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        sb.append("// НейроАнгл запись: ").append(recordedDeltas.size()).append(" дельт (yaw, pitch)\n");
        sb.append("float[][] neuroDeltas = new float[][] {\n");
        for (int i = 0; i < recordedDeltas.size(); i++) {
            Turns t = recordedDeltas.get(i);
            sb.append("  { ").append(String.format("%.4ff", t.getYaw())).append(", ")
                    .append(String.format("%.4ff", t.getPitch())).append(" }");
            if (i < recordedDeltas.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("};");
        String full = sb.toString();
        System.out.println("[NeuroAngle] Recorded rotation (copy as code):");
        System.out.println(full);
        ChatMessage.brandmessage("§e[НейроАнгл] §fЗаписано " + recordedDeltas.size() + " дельт. Код в консоли (лог клиента).");
    }

    /**
     * Загружает пресет дельт из массива (например, скопированного из лога после записи).
     * Ротация будет строиться на этих движениях без повторной записи.
     */
    public static void loadPresetDeltas(float[][] preset) {
        recordedDeltas.clear();
        if (preset != null) {
            for (float[] pair : preset) {
                if (pair != null && pair.length >= 2) {
                    recordedDeltas.add(new Turns(pair[0], pair[1]));
                }
            }
        }
        playbackIndex = 0;
    }

    /**
     * Загружает ротацию из файла neuro_yaw_pitch.txt (формат: каждая строка "yaw pitch", запятая или точка в числах).
     * По записям строятся дельты между соседними точками — по ним и воспроизводится ротация.
     */
    public static void loadPresetFromYawPitchFile() {
        if (fileLoadAttempted) return;
        fileLoadAttempted = true;
        try {
            File dir = MinecraftClient.getInstance().runDirectory;
            if (dir == null) return;
            File file = new File(dir, "neuro_yaw_pitch.txt");
            if (!file.exists()) return;
            List<Turns> fileSamples = new ArrayList<>();
            try (BufferedReader r = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String s = line.replace(',', '.');
                    String[] parts = s.split("\\s+");
                    if (parts.length >= 2) {
                        float yaw = Float.parseFloat(parts[0]);
                        float pitch = Float.parseFloat(parts[1]);
                        fileSamples.add(new Turns(yaw, pitch));
                    }
                }
            }
            if (fileSamples.size() < 2) return;
            recordedDeltas.clear();
            for (int i = 0; i < fileSamples.size() - 1; i++) {
                Turns a = fileSamples.get(i);
                Turns b = fileSamples.get(i + 1);
                recordedDeltas.add(new Turns(
                        MathHelper.wrapDegrees(b.getYaw() - a.getYaw()),
                        b.getPitch() - a.getPitch()
                ));
            }
            playbackIndex = 0;
            ChatMessage.brandmessage("§e[НейроАнгл] §fРотация загружена из neuro_yaw_pitch.txt: " + recordedDeltas.size() + " дельт.");
        } catch (Throwable t) {
            System.err.println("[NeuroAngle] Ошибка загрузки neuro_yaw_pitch.txt: " + t.getMessage());
        }
    }

    public static void addSample(float yaw, float pitch) {
        if (recording) {
            samples.add(new Turns(yaw, pitch));
        }
    }

    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        if (recordedDeltas.isEmpty()) {
            loadPresetFromYawPitchFile();
        }
        if (recordedDeltas.isEmpty()) {
            return fallbackLerp(currentAngle, targetAngle);
        }

        Turns delta = recordedDeltas.get(playbackIndex % recordedDeltas.size());
        playbackIndex++;

        float needYaw = MathHelper.wrapDegrees(targetAngle.getYaw() - currentAngle.getYaw());
        float needPitch = targetAngle.getPitch() - currentAngle.getPitch();

        float curYaw = currentAngle.getYaw();
        float curPitch = currentAngle.getPitch();
        float recYaw = delta.getYaw() * SPEED_FACTOR;
        float recPitch = delta.getPitch() * SPEED_FACTOR;

        float newYaw = MathHelper.clamp(
                curYaw + recYaw,
                Math.min(curYaw, curYaw + needYaw),
                Math.max(curYaw, curYaw + needYaw)
        );
        float newPitch = MathHelper.clamp(
                curPitch + recPitch,
                Math.min(curPitch, curPitch + needPitch),
                Math.max(curPitch, curPitch + needPitch)
        );
        newPitch = MathHelper.clamp(newPitch, -90.0F, 90.0F);

        return new Turns(newYaw, newPitch).adjustSensitivity();
    }

    private Turns fallbackLerp(Turns currentAngle, Turns targetAngle) {
        Turns angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(yawDelta, pitchDelta);
        if (rotationDifference < 1e-6f) {
            return currentAngle;
        }
        float straightLineYaw = Math.abs(yawDelta / rotationDifference) * 360.0F;
        float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * 360.0F;
        float newYaw = currentAngle.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw);
        float newPitch = currentAngle.getPitch() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch);
        return new Turns(
                MathHelper.lerp(0.1f, currentAngle.getYaw(), newYaw),
                MathHelper.lerp(0.1f, currentAngle.getPitch(), newPitch)
        );
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0.1, 0.1, 0.1);
    }
}
