package code.essence.features.impl.render;


import code.essence.events.packet.PacketEvent;
import code.essence.events.player.AttackEvent;
import code.essence.events.player.TickEvent;
import code.essence.events.render.WorldRenderEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.*;
import code.essence.utils.animation.util.Animation;
import code.essence.utils.animation.util.Easings;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.display.render.geometry.Render3D;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.math.projection.Projection;
import code.essence.utils.math.time.TimerUtil;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Particles extends Module implements QuickImports {

    private static final int TOTEM_COLOR_GREEN = 0x00FF00;
    private static final int TOTEM_COLOR_YELLOW = 0xFFFF00;
    private static Particles instance;
    public final MultiSelectSetting typep = new MultiSelectSetting("Спавнить при", "Когда спавнить партиклы").value("Бездействии", "Движении", "Крите", "Броске", "Тотеме").selected("Бездействии", "Движении", "Крите", "Тотеме");
    private final SliderSettings countAFK = new SliderSettings("Кол-во при бездействии", "Количество партиклов при бездействии").range(1, 25).setValue(5).step(1F).visible(() -> typep.isSelected("Бездействии"));
    private final SliderSettings countAttack = new SliderSettings("Кол-во при крите", "Количество партиклов при крите").range(1, 50).setValue(2).step(1F).visible(() -> typep.isSelected("Крите"));
    private final SliderSettings countMove = new SliderSettings("Кол-во при движении", "Количество партиклов при движении").range(1, 25).setValue(2).step(1F).visible(() -> typep.isSelected("Движении"));
    private final SliderSettings moveLifetime = new SliderSettings("Время жизни при движении", "Время жизни партиклов при движении в мс").range(500, 5000).setValue(3500).step(250F).visible(() -> typep.isSelected("Движении"));
    private final SliderSettings moveSpeed = new SliderSettings("Скорость при движении", "Скорость партиклов при движении").range(0.1F, 4.0F).setValue(1.0F).step(0.1F).visible(() -> typep.isSelected("Движении"));
    private final SliderSettings moveSize = new SliderSettings("Размер при движении", "Размер партиклов при движении").range(0.0F, 1F).setValue(0.5F).step(0.1F).visible(() -> typep.isSelected("Движении"));
    private final SliderSettings moveOpacity = new SliderSettings("Прозрачность при движении", "Прозрачность партиклов при движении").range(0.1F, 1.0F).setValue(1.0F).step(0.1F).visible(() -> typep.isSelected("Движении"));
    private final SliderSettings size = new SliderSettings("Размер", "Размер партиклов").range(0.0F, 1F).setValue(0.5F).step(0.1F);
    private final SliderSettings range = new SliderSettings("Дистанция", "Дистанция спавна").range(4, 32).setValue(16).step(1F);
    private final SliderSettings duration = new SliderSettings("Время жизни", "Время жизни партиклов").range(500, 5000).setValue(3500).step(250F);
    private final SliderSettings strength = new SliderSettings("Скорость движения", "Скорость движения партиклов").range(0.1F, 2.0F).setValue(1.0F).step(0.1F);
    private final SliderSettings opacity = new SliderSettings("Прозрачность", "Прозрачность партиклов").range(0.1F, 1.0F).setValue(1.0F).step(0.1F);
    private final SliderSettings critSize = new SliderSettings("Размер при крите", "Размер партиклов при крите").range(0.0F, 1F).setValue(0.5F).step(0.1F).visible(() -> typep.isSelected("Крите"));
    private final SliderSettings critDuration = new SliderSettings("Время жизни при крите", "Время жизни партиклов при крите").range(500, 5000).setValue(3500).step(250F).visible(() -> typep.isSelected("Крите"));
    private final SliderSettings critStrength = new SliderSettings("Скорость движения при крите", "Скорость движения партиклов при крите").range(0.1F, 4.0F).setValue(1.0F).step(0.1F).visible(() -> typep.isSelected("Крите"));
    private final SliderSettings critOpacity = new SliderSettings("Прозрачность при крите", "Прозрачность партиклов при крите").range(0.1F, 1.0F).setValue(1.0F).step(0.1F).visible(() -> typep.isSelected("Крите"));
    private final SliderSettings totemCount = new SliderSettings("Кол-во при тотеме", "Количество партиклов при тотеме").range(1, 200).setValue(75).step(1F).visible(() -> typep.isSelected("Тотеме"));
    private final SliderSettings totemSpeed = new SliderSettings("Скорость при тотеме", "Скорость партиклов при тотеме").range(0.1F, 10.0F).setValue(4.0F).step(0.1F).visible(() -> typep.isSelected("Тотеме"));
    private final SliderSettings totemLifetime = new SliderSettings("Время жизни при тотеме", "Время жизни партиклов при тотеме в мс").range(500, 10000).setValue(4000).step(100F).visible(() -> typep.isSelected("Тотеме"));
    private final SliderSettings totemSize = new SliderSettings("Размер при тотеме", "Размер партиклов при тотеме").range(0.0F, 1F).setValue(0.25F).step(0.1F).visible(() -> typep.isSelected("Тотеме"));
    private final SliderSettings totemOpacity = new SliderSettings("Прозрачность при тотеме", "Прозрачность партиклов при тотеме").range(0.1F, 1.0F).setValue(1.0F).step(0.1F).visible(() -> typep.isSelected("Тотеме"));
    private final SliderSettings throwCount = new SliderSettings("Кол-во при броске", "Количество партиклов при броске").range(1, 25).setValue(2).step(1F).visible(() -> typep.isSelected("Броске"));
    private final SliderSettings throwLifetime = new SliderSettings("Время жизни при броске", "Время жизни партиклов при броске в мс").range(500, 5000).setValue(3500).step(250F).visible(() -> typep.isSelected("Броске"));
    private final SliderSettings throwSpeed = new SliderSettings("Скорость при броске", "Скорость партиклов при броске").range(0.1F, 4.0F).setValue(2.0F).step(0.1F).visible(() -> typep.isSelected("Броске"));
    private final SliderSettings throwSize = new SliderSettings("Размер при броске", "Размер партиклов при броске").range(0.0F, 1F).setValue(0.4F).step(0.1F).visible(() -> typep.isSelected("Броске"));
    private final SliderSettings throwOpacity = new SliderSettings("Прозрачность при броске", "Прозрачность партиклов при броске").range(0.1F, 1.0F).setValue(1.0F).step(0.1F).visible(() -> typep.isSelected("Броске"));
    private final SelectSetting particleMode = new SelectSetting("Тип частиц", "Тип частиц").value("Шарики", "Кубы", "Доллары", "Сердечки", "Снежинки", "Звезды", "Звезды2", "Рандом").selected("Шарики");
    private final BooleanSetting glowing = new BooleanSetting("Свечение", "Свечение партиклов").setValue(true).visible(() -> particleMode.isSelected("Кубы"));
    private final BooleanSetting onlyMove = new BooleanSetting("Только в движении", "Спавнить только в движении").setValue(false);
    private final BooleanSetting ground = new BooleanSetting("Спавнить на земле", "Спавнить партиклы на земле").setValue(false);
    private final BooleanSetting physic = new BooleanSetting("Физика", "Физика партиклов").setValue(false);

    private final SelectSetting colorMode = new SelectSetting("Режим цвета", "Режим цвета партиклов")
            .value("Клиентский", "Радужный", "Кастом").selected("Клиентский");
    public final ColorSetting customMainColor = new ColorSetting("Главный цвет", "Главный цвет").value(-1).visible(() -> colorMode.isSelected("Кастом"));
    public final ColorSetting customSecondaryColor = new ColorSetting("Вторичный цвет", "Вторичный цвет").value(-1).visible(() -> colorMode.isSelected("Кастом"));
    private final List<Particle> particles = new ArrayList<>();
    private final List<ParticleAttack> targetParticles = new ArrayList<>();
    private final List<ParticleAttack> flameParticles = new ArrayList<>();
    private final List<ParticleAttack> thrownParticles = new ArrayList<>();
    private final List<ParticleAttack> totemParticles = new ArrayList<>();
    private double lastPlayerX = 0;
    private double lastPlayerY = 0;
    private double lastPlayerZ = 0;
    private boolean positionInitialized = false;

    public Particles() {
        super("Particles", ModuleCategory.RENDER);
        setup(particleMode, typep,
                // Бездействие
                countAFK, range, duration, strength, opacity, size, onlyMove, ground,
                // Движение
                countMove, moveLifetime, moveSpeed, moveSize, moveOpacity,
                // Крите
                countAttack, critSize, critDuration, critStrength, critOpacity,
                // Броске
                throwCount, throwLifetime, throwSpeed, throwSize, throwOpacity,
                // Тотеме
                totemCount, totemSpeed, totemLifetime, totemSize, totemOpacity,
                // Общие
                glowing, physic, colorMode, customMainColor, customSecondaryColor);
        instance = this;
    }

    public static Particles getInstance() {
        return instance;
    }

    private void clear() {
        particles.clear();
        targetParticles.clear();
        flameParticles.clear();
        thrownParticles.clear();
        totemParticles.clear();
    }

    @Override
    public void setState(boolean state) {
        super.setState(state);
        if (!state) {
            clear();
        }
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        if (!typep.isSelected("Крите")) return;
        if (mc.player.fallDistance == 0) return;

        Entity target = e.getEntity();
        if (target == null) return;

        float motion = strength.getValue();

        for (int i = 0; i < countAttack.getInt(); i++) {
            spawnParticleAttack(targetParticles, new Vec3d(target.getX(), target.getY() + randomValue(0, target.getHeight()), target.getZ()), new Vec3d(randomValue(-motion, motion), randomValue(-motion, motion / 4F), randomValue(-motion, motion)));
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (typep.isSelected("Движении") && hasPlayerMoved() && !mc.options.getPerspective().isFirstPerson()) {
            for (int i = 0; i < countMove.getInt(); i++) {
                Vec3d playerVel = mc.player.getVelocity();
                spawnParticleAttack(flameParticles, new Vec3d(mc.player.getX() + randomValue(-0.5, 0.5), mc.player.getY() + randomValue(0, mc.player.getHeight()), mc.player.getZ() + randomValue(-0.5, 0.5)), new Vec3d(playerVel.x + randomValue(-0.25, 0.25), randomValue(-0.15, 0.15), playerVel.z + randomValue(-0.25, 0.25)).multiply(moveSpeed.getValue()));
            }
        }

        if (typep.isSelected("Бездействии")) {
            if (!onlyMove.isValue() || hasPlayerMoved()) {
                int r = range.getInt();
                for (int i = 0; i < countAFK.getInt(); i++) {
                    Vec3d additional = mc.player.getPos().add(randomValue(-r, r), 0, randomValue(-r, r));
                    BlockPos pos = mc.world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, BlockPos.ofFloored(additional));
                    spawnParticle(new Vec3d(pos.getX() + randomValue(0, 1), ground.isValue() ? pos.getY() : mc.player.getY() + randomValue(mc.player.getHeight(), r), pos.getZ() + randomValue(0, 1)), new Vec3d(0, randomValue(0.0, strength.getValue()) * (ground.isValue() ? 1 : -1), 0));
                }
            }
        }

        if (typep.isSelected("Броске")) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EnderPearlEntity || entity instanceof ArrowEntity || entity instanceof TridentEntity) {

                    if (entity instanceof TridentEntity) {
                        TridentEntity trident = (TridentEntity) entity;
                        if (trident.getOwner() != null) {
                            // Skip if trident has dealt damage (we can't check private field)
                            continue;
                        }
                    }

                    boolean isMoving = entity.prevX != entity.getX() || entity.prevY != entity.getY() || entity.prevZ != entity.getZ();
                    if (!isMoving) {
                        continue;
                    }
                    Vec3d pos = entity.getPos();
                    Vec3d entityVel = entity.getVelocity();
                    for (int i = 0; i < throwCount.getInt(); i++) {
                        spawnParticleThrown(new Vec3d(pos.x + randomValue(-0.2, 0.2), pos.y + randomValue(-0.2, 0.2), pos.z + randomValue(-0.2, 0.2)), new Vec3d(entityVel.x * 0.1 + randomValue(-0.1, 0.1), entityVel.y * 0.1 + randomValue(-0.1, 0.1), entityVel.z * 0.1 + randomValue(-0.1, 0.1)));
                    }
                }
            }
        }

        removeExpiredParticlesAttack(targetParticles, (long) critDuration.getValue());
        removeExpiredParticlesAttack(flameParticles, (long) moveLifetime.getValue());
        removeExpiredParticlesAttack(thrownParticles, (long) throwLifetime.getValue());
        removeExpiredParticlesAttack(totemParticles, (long) totemLifetime.getValue());
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof EntityStatusS2CPacket packet) {
            if (packet.getStatus() == 35) {
                Entity entity = packet.getEntity(mc.world);
                if (entity instanceof LivingEntity) {
                    if (isState() && typep.isSelected("Тотеме")) {
                        createTotemEffect(entity.getX(), entity.getY() + entity.getHeight() / 2, entity.getZ());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (!isState()) return;

        MatrixStack matrix = e.getStack();
        float partialTicks = e.getPartialTicks();

        setupRenderState();

        if (typep.isSelected("Бездействии")) {
            renderParticles(matrix, particles, duration.getValue(), duration.getMin(), partialTicks);
        }

        if (typep.isSelected("Крите")) {
            renderParticlesAttack(matrix, targetParticles, 500, 2000, partialTicks);
        }
        if (typep.isSelected("Движении") && !mc.options.getPerspective().isFirstPerson()) {
            renderParticlesAttack(matrix, flameParticles, 500, (long) moveLifetime.getValue(), partialTicks);
        }
        if (typep.isSelected("Броске")) {
            renderParticlesAttack(matrix, thrownParticles, 500, (long) throwLifetime.getValue(), partialTicks);
        }
        if (typep.isSelected("Тотеме")) {
            renderParticlesAttack(matrix, totemParticles, 1000, (long) totemLifetime.getValue(), partialTicks);
        }

        resetRenderState();
    }

    private void renderParticles(MatrixStack matrix, List<Particle> particles, double lifetime, double duration, float partialTicks) {
        removeExpiredParticles(particles, lifetime + duration);
        if (particles.isEmpty()) return;

        matrix.push();
        RenderSystem.enableDepthTest();
        for (Particle particle : particles) {
            particle.update(physic.isValue());
            Animation animation = particle.animation();
            animation.update();
            float alpha = animation.get();

            if (alpha != opacity.getValue() && !particle.time().isReached((long) duration)) {
                animation.run(opacity.getValue(), (duration / 1000), Easings.CUBIC_OUT);
            }
            if (alpha != 0.0F && particle.time().isReached((long) lifetime)) {
                animation.run(0.0F, (duration / 1000), Easings.CUBIC_OUT);
            }

            int color = ColorAssist.multAlpha(ColorAssist.replAlpha(particle.color(), alpha), (float) ((Math.sin((System.currentTimeMillis() - particle.spawnTime()) / 200D) + 1F) / 2F));
            Vec3d vec = particle.position();
            float x = (float) vec.x;
            float y = (float) vec.y;
            float z = (float) vec.z;

            renderParticle(matrix, particle, x, y, z, color, partialTicks);
        }
        matrix.pop();
    }

    private void removeExpiredParticles(List<Particle> particles, double lifespan) {
        particles.removeIf(particle -> !Projection.canSee(particle.box));
        particles.removeIf(particle -> particle.time().isReached((long) lifespan));
    }

    private void removeExpiredParticlesAttack(List<ParticleAttack> particles, long lifespan) {
        if (particles == totemParticles || particles == targetParticles) {
            // Для тотемов и партиклов при крите не проверяем видимость
            particles.removeIf(particle -> particle.time().isReached(lifespan));
        } else if (particles == flameParticles) {
            particles.removeIf(particle -> particle.time().isReached(lifespan));
        } else {
            particles.removeIf(particle -> !Projection.canSee(particle.box));
            particles.removeIf(particle -> particle.time().isReached(lifespan));
        }
    }

    private static final double CUBE_ROTATION_RAD_PER_MS = 0.000000012;

    private void renderParticle(MatrixStack matrix, Particle particle, float x, float y, float z, int color, float partialTicks) {
        if (particle.type() == ParticleType.cube) {
            Vec3d pos = getInterpolatedPos(particle.prevPosition(), particle.position(), partialTicks);
            Vec3d baseRot = getInterpolatedPos(particle.prevRotateVec(), particle.rotateVec(), partialTicks);
            long elapsed = particle.time().getTime();
            double timeY = elapsed * CUBE_ROTATION_RAD_PER_MS;
            double timeX = elapsed * CUBE_ROTATION_RAD_PER_MS * 0.6;
            Vec3d rot = new Vec3d(baseRot.x + timeX, baseRot.y + timeY, baseRot.z + timeX * 0.5);
            float cubeScale = particle.size * 2.0f;
            renderCube(pos, rot, cubeScale, color, glowing.isValue());
            return;
        }

        float pos = particle.size;
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        double posX = x - camPos.x;
        double posY = y - camPos.y;
        double posZ = z - camPos.z;

        MatrixStack matrices = new MatrixStack();
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        matrices.translate(posX, posY, posZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        if (particle.type() != ParticleType.star) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180F));
        }

        if (particle.type() == ParticleType.heart) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90F));
        }
        if (particle.type().rotatable()) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(particle.rotate()));
        matrices.translate(0, -pos, -pos);

        // Свечение всегда включено для не-кубов
        Render3D.drawTexture(matrices.peek(), ParticleType.shariki.texture(), -pos * 4, -pos * 4, pos * 8, pos * 8, new org.joml.Vector4i(ColorAssist.multAlpha(color, 0.1F)), true);
        Render3D.drawTexture(matrices.peek(), particle.type().texture(), -pos, -pos, pos * 2, pos * 2, new org.joml.Vector4i(color), true);
        if (particle.type.equals(ParticleType.shariki)) {
            Render3D.drawTexture(matrices.peek(), particle.type().texture(), -pos / 2, -pos / 2, pos, pos, new org.joml.Vector4i(color), true);
        }
        matrices.pop();
    }

    private void setupRenderState() {
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
    }

    private void resetRenderState() {
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
    }

    private void renderParticlesAttack(MatrixStack matrix, List<ParticleAttack> particles, long fadeInTime, long fadeOutTime, float partialTicks) {
        if (particles.isEmpty()) {
            return;
        }

        matrix.push();
        RenderSystem.enableDepthTest();
        for (ParticleAttack particle : particles) {
            boolean usePhysics = physic.isValue() || particles == totemParticles;
            particle.update(usePhysics);
            particle.animation.update();

            long lifetime;
            float targetOpacity;
            if (particles == targetParticles) {
                lifetime = (long) critDuration.getValue();
                targetOpacity = critOpacity.getValue();
            } else if (particles == flameParticles) {
                lifetime = (long) moveLifetime.getValue();
                targetOpacity = moveOpacity.getValue();
            } else if (particles == thrownParticles) {
                lifetime = (long) throwLifetime.getValue();
                targetOpacity = throwOpacity.getValue();
            } else if (particles == totemParticles) {
                lifetime = (long) totemLifetime.getValue();
                targetOpacity = totemOpacity.getValue();
            } else {
                lifetime = fadeOutTime;
                targetOpacity = opacity.getValue();
            }

            if (particle.animation().get() != targetOpacity) {
                if (!particle.time().isReached(fadeInTime)) {
                    particle.animation().run(targetOpacity, 0.5, Easings.CUBIC_OUT);
                } else if (particle.animation().get() == 0.0f && particle.time().getTime() < fadeInTime) {
                    particle.animation().run(targetOpacity, 0.5, Easings.CUBIC_OUT);
                }
            }
            long fadeOutStart = lifetime - 1000;
            if (particle.animation().get() != 0 && particle.time().isReached(fadeOutStart)) {
                particle.animation().run(0, 1.0, Easings.CUBIC_OUT);
            }

            int color = ColorAssist.replAlpha(particle.color(), particle.animation.get());
            Vec3d vec = particle.position();
            float x = (float) vec.x;
            float y = (float) vec.y;
            float z = (float) vec.z;

            renderParticleAttack(matrix, particle, x, y, z, particle.size, color, partialTicks);
        }
        matrix.pop();
    }

    private void renderParticleAttack(MatrixStack matrix, ParticleAttack particle, float x, float y, float z, float pos, int color, float partialTicks) {
        if (particle.type() == ParticleType.cube) {
            Vec3d position = getInterpolatedPos(particle.prevPosition(), particle.position(), partialTicks);
            Vec3d baseRot = getInterpolatedPos(particle.prevRotateVec(), particle.rotateVec(), partialTicks);
            long elapsed = particle.time().getTime();
            double timeY = elapsed * CUBE_ROTATION_RAD_PER_MS;
            double timeX = elapsed * CUBE_ROTATION_RAD_PER_MS * 0.6;
            Vec3d rot = new Vec3d(baseRot.x + timeX, baseRot.y + timeY, baseRot.z + timeX * 0.5);
            float cubeScale = particle.size * 2.0f;
            renderCube(position, rot, cubeScale, color, glowing.isValue());
            return;
        }

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        double posX = x - camPos.x;
        double posY = y - camPos.y;
        double posZ = z - camPos.z;

        MatrixStack matrices = new MatrixStack();
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        matrices.translate(posX, posY, posZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        if (particle.type() != ParticleType.star) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180F));
        }
        if (particle.type() == ParticleType.heart) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90F));
        }
        if (particle.type().rotatable()) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(particle.rotate()));
        matrices.translate(0, -pos, 0);

        // Свечение всегда включено для не-кубов
        Render3D.drawTexture(matrices.peek(), ParticleType.shariki.texture(), -pos * 4, -pos * 4, pos * 8, pos * 8, new org.joml.Vector4i(ColorAssist.multAlpha(color, 0.1F)), true);
        Render3D.drawTexture(matrices.peek(), particle.type().texture(), -pos, -pos, pos * 2, pos * 2, new org.joml.Vector4i(color), true);
        if (particle.type.equals(ParticleType.shariki)) {
            Render3D.drawTexture(matrices.peek(), particle.type().texture(), -pos / 2, -pos / 2, pos, pos, new org.joml.Vector4i(color), true);
        }
        matrices.pop();
    }

    // kubi

    private Vec3d getInterpolatedPos(Vec3d prev, Vec3d current, float partialTicks) {
        if (prev == null) prev = current;
        return new Vec3d(prev.x + (current.x - prev.x) * partialTicks, prev.y + (current.y - prev.y) * partialTicks, prev.z + (current.z - prev.z) * partialTicks);
    }

    private Vec3d rotatePoint(Vec3d point, Vec3d rotation) {
        double cosY = Math.cos(rotation.y);
        double sinY = Math.sin(rotation.y);
        double x = point.x * cosY - point.z * sinY;
        double z = point.x * sinY + point.z * cosY;

        double cosX = Math.cos(rotation.x);
        double sinX = Math.sin(rotation.x);
        double y = point.y * cosX - z * sinX;
        z = point.y * sinX + z * cosX;

        return new Vec3d(x, y, z);
    }

    private void renderCube(Vec3d pos, Vec3d rot, float scale, int color, boolean withGlow) {
        float half = scale * 0.5f;

        Vec3d[] corners = new Vec3d[8];
        Vec3d[] offsets = {new Vec3d(-half, -half, -half), new Vec3d(half, -half, -half), new Vec3d(half, half, -half), new Vec3d(-half, half, -half), new Vec3d(-half, -half, half), new Vec3d(half, -half, half), new Vec3d(half, half, half), new Vec3d(-half, half, half)};

        for (int i = 0; i < 8; i++) {
            corners[i] = rotatePoint(offsets[i], rot).add(pos);
        }

        // Свечение - рендерим текстуру shariki поверх куба (billboarded)
        if (withGlow) {
            Camera camera = mc.gameRenderer.getCamera();
            Vec3d camPos = camera.getPos();

            double posX = pos.x - camPos.x;
            double posY = pos.y - camPos.y;
            double posZ = pos.z - camPos.z;

            MatrixStack matrices = new MatrixStack();
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
            matrices.translate(posX, posY, posZ);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            float glowSize = scale * 2.0f;
            Render3D.drawTexture(matrices.peek(), ParticleType.shariki.texture(), -glowSize * 2, -glowSize * 2, glowSize * 4, glowSize * 4, new org.joml.Vector4i(ColorAssist.multAlpha(color, 0.2F)), true);
            matrices.pop();
        }

        // Основной куб
        Render3D.drawLine(corners[0], corners[1], color, 1.5f, true);
        Render3D.drawLine(corners[1], corners[2], color, 1.5f, true);
        Render3D.drawLine(corners[2], corners[3], color, 1.5f, true);
        Render3D.drawLine(corners[3], corners[0], color, 1.5f, true);
        Render3D.drawLine(corners[4], corners[5], color, 1.5f, true);
        Render3D.drawLine(corners[5], corners[6], color, 1.5f, true);
        Render3D.drawLine(corners[6], corners[7], color, 1.5f, true);
        Render3D.drawLine(corners[7], corners[4], color, 1.5f, true);
        Render3D.drawLine(corners[0], corners[4], color, 1.5f, true);
        Render3D.drawLine(corners[1], corners[5], color, 1.5f, true);
        Render3D.drawLine(corners[2], corners[6], color, 1.5f, true);
        Render3D.drawLine(corners[3], corners[7], color, 1.5f, true);

        int diagColor = ColorAssist.multAlpha(color, 0.5f);
        Render3D.drawLine(corners[0], corners[6], diagColor, 1.0f, true);
        Render3D.drawLine(corners[1], corners[7], diagColor, 1.0f, true);
        Render3D.drawLine(corners[2], corners[4], diagColor, 1.0f, true);
        Render3D.drawLine(corners[3], corners[5], diagColor, 1.0f, true);
    }

    private void spawnParticle(Vec3d position, Vec3d velocity) {
        float size = 0.05F + (this.size.getValue() * 0.2F);
        int color = switch (this.colorMode.getSelected()) {
            case "Клиентский" -> ColorAssist.fade(particles.size() * 100);
            case "Радужный" -> ColorAssist.rainbow(10, particles.size() * 100, 0.5F, 1F, 1F);
            default -> {
                int mainColor = customMainColor.getColor();
                int secondaryColor = customSecondaryColor.getColor();
                float factor = (float) (particles.size() % 100) / 100f;
                yield ColorAssist.interpolateColor(mainColor, secondaryColor, factor);
            }
        };

        ParticleType type = switch (this.particleMode.getSelected()) {
            case "Шарики" -> ParticleType.shariki;
            case "Кубы" -> ParticleType.cube;
            case "Звезды" -> ParticleType.star;
            case "Звезды2" -> ParticleType.startwo;
            case "Доллары" -> ParticleType.dollar;
            case "Сердечки" -> ParticleType.heart;
            case "Снежинки" -> ParticleType.snowflake;
            default -> ParticleType.getRandom();
        };

        particles.add(new Particle(type, position.add(0, size, 0), velocity, particles.size(), color, size, (int) (Math.round(randomValue(0, 360) / 15.0) * 15)));
    }

    private void spawnParticleAttack(List<ParticleAttack> particles, Vec3d position, Vec3d velocity) {
        float size;
        if (particles == targetParticles) {
            size = 0.05F + (this.critSize.getValue() * 0.2F);
        } else if (particles == flameParticles) {
            size = 0.05F + (this.moveSize.getValue() * 0.2F);
        } else {
            size = 0.05F + (this.size.getValue() * 0.2F);
        }
        int color = switch (this.colorMode.getSelected()) {
            case "Клиентский" -> ColorAssist.fade(particles.size() * 100);
            case "Радужный" -> ColorAssist.rainbow(4, particles.size() * 100, 0.5F, 1F, 1F);
            default -> {
                int mainColor = customMainColor.getColor();
                int secondaryColor = customSecondaryColor.getColor();
                yield ColorAssist.interpolateColor(mainColor, secondaryColor, (float) (particles.size() % 100) / 100f);
            }
        };

        ParticleType type = switch (this.particleMode.getSelected()) {
            case "Шарики" -> ParticleType.shariki;
            case "Кубы" -> ParticleType.cube;
            case "Звезды" -> ParticleType.star;
            case "Звезды2" -> ParticleType.startwo;
            case "Доллары" -> ParticleType.dollar;
            case "Сердечки" -> ParticleType.heart;
            case "Снежинки" -> ParticleType.snowflake;
            default -> ParticleType.getRandom();
        };

        Vec3d motion;
        if (particles == targetParticles) {
            motion = velocity.multiply(this.critStrength.getValue() * 5);
        } else if (particles == flameParticles) {
            motion = velocity.multiply(this.moveSpeed.getValue());
        } else {
            motion = velocity.multiply(this.strength.getValue());
        }

        ParticleAttack particle = new ParticleAttack(type, position.add(0, size, 0), motion, particles.size(), (int) (Math.round(randomValue(0, 360) / 15.0) * 15), color, size);
        particles.add(particle);
    }

    private void spawnParticleThrown(Vec3d position, Vec3d velocity) {
        float size = 0.05F + (throwSize.getValue() * 0.2F);
        int color = switch (this.colorMode.getSelected()) {
            case "Клиентский" -> ColorAssist.fade(thrownParticles.size() * 100);
            case "Радужный" -> ColorAssist.rainbow(4, thrownParticles.size() * 100, 0.5F, 1F, 1F);
            default -> {
                int mainColor = customMainColor.getColor();
                int secondaryColor = customSecondaryColor.getColor();
                yield ColorAssist.interpolateColor(mainColor, secondaryColor, (float) (thrownParticles.size() % 100) / 100f);
            }
        };

        ParticleType type = switch (this.particleMode.getSelected()) {
            case "Шарики" -> ParticleType.shariki;
            case "Кубы" -> ParticleType.cube;
            case "Звезды" -> ParticleType.star;
            case "Звезды2" -> ParticleType.startwo;
            case "Доллары" -> ParticleType.dollar;
            case "Сердечки" -> ParticleType.heart;
            case "Снежинки" -> ParticleType.snowflake;
            default -> ParticleType.getRandom();
        };
        Vec3d motion = velocity.multiply(throwSpeed.getValue());

        thrownParticles.add(new ParticleAttack(type, position.add(0, size, 0), motion, thrownParticles.size(), (int) (Math.round(randomValue(0, 360) / 15.0) * 15), color, size));
    }

    private void spawnTotemParticle(Vec3d position, Vec3d velocity, int color) {
        ParticleType type = switch (this.particleMode.getSelected()) {
            case "Шарики" -> ParticleType.shariki;
            case "Кубы" -> ParticleType.cube;
            case "Звезды" -> ParticleType.star;
            case "Звезды2" -> ParticleType.startwo;
            case "Доллары" -> ParticleType.dollar;
            case "Сердечки" -> ParticleType.heart;
            case "Снежинки" -> ParticleType.snowflake;
            default -> ParticleType.getRandom();
        };

        float size = 0.05F + (totemSize.getValue() * 0.2F);
        if (type == ParticleType.star || type == ParticleType.heart) {
            size = 0.1F;
        }

        totemParticles.add(new ParticleAttack(type, position.add(randomValue(-0.3, 0.3), randomValue(-0.3, 0.3), randomValue(-0.3, 0.3)), velocity.multiply(totemSpeed.getValue()), totemParticles.size(), (int) (Math.round(randomValue(0, 360) / 15.0) * 15), color, size));
    }

    private boolean hasPlayerMoved() {
        if (mc.player == null) return false;

        if (!positionInitialized) {
            lastPlayerX = mc.player.getX();
            lastPlayerY = mc.player.getY();
            lastPlayerZ = mc.player.getZ();
            positionInitialized = true;
            return false;
        }

        boolean moved = Math.abs(mc.player.getX() - lastPlayerX) > 0.001 || Math.abs(mc.player.getY() - lastPlayerY) > 0.001 || Math.abs(mc.player.getZ() - lastPlayerZ) > 0.001;

        if (moved) {
            lastPlayerX = mc.player.getX();
            lastPlayerY = mc.player.getY();
            lastPlayerZ = mc.player.getZ();
        }

        return moved;
    }

    private double randomValue(double min, double max) {
        return min + (max - min) * new Random().nextDouble();
    }

    public void createTotemEffect(double x, double y, double z) {
        if (!isState() || !typep.isSelected("Тотеме")) return;

        for (int i = 0; i < totemCount.getInt(); i++) {
            double angleXZ = Math.random() * Math.PI * 2;
            double angleY = Math.random() * Math.PI;
            double strength = 0.5 + Math.random() * 0.5;

            Vec3d velocity = new Vec3d(Math.sin(angleXZ) * Math.sin(angleY) * strength, Math.cos(angleY) * strength, Math.cos(angleXZ) * Math.sin(angleY) * strength);

            int color = Math.random() < 0.7 ? TOTEM_COLOR_GREEN : TOTEM_COLOR_YELLOW;

            spawnTotemParticle(new Vec3d(x, y, z), velocity, color);
        }
    }

    @Getter
    @Accessors(fluent = true)
    public enum ParticleType {
        shariki("glow", false),
        cube(null, true),
        dollar("bucks1", false),
        heart("heart1", false),
        snowflake("show1", false),
        star("star1", false),
        startwo("sparkle", false);

        private final Identifier texture;
        private final boolean rotatable;

        ParticleType(String name, boolean rotatable) {
            texture = name != null ? Identifier.of("textures/particles/" + name + ".png") : null;
            this.rotatable = rotatable;
        }

        public static ParticleType getRandom() {
            ParticleType[] values = ParticleType.values();
            ParticleType result;
            do {
                result = values[new Random().nextInt(values.length)];
            } while (result == cube);
            return result;
        }
    }

    @Getter
    @Accessors(fluent = true)
    public static class Particle {
        private final long spawnTime = System.currentTimeMillis();
        private final ParticleType type;
        private final Box box;
        private final int rotate;
        private final int index;
        private final int color;
        private final float size;
        private final TimerUtil time = new TimerUtil();
        private final Animation animation = new Animation();
        private Vec3d position;
        private Vec3d prevPosition;
        private Vec3d velocity;
        private Vec3d rotateVec;
        private Vec3d prevRotateVec;
        private Vec3d rotateMotion;

        public Particle(ParticleType type, final Vec3d position, final Vec3d velocity, final int index, int color, float size, int rotate) {
            this.type = type;
            this.rotate = rotate;
            this.box = new Box(position, position).expand(size);
            this.position = position;
            this.prevPosition = position;
            this.velocity = velocity.multiply(0.01F);
            this.index = index;
            this.color = ColorAssist.clampSaturation(color, 0f, 0.8f);
            this.size = size;
            this.time.resetCounter();

            this.rotateVec = Vec3d.ZERO;
            this.prevRotateVec = Vec3d.ZERO;
            this.rotateMotion = new Vec3d(randomDouble(-1.0, 1.0) * 0.04, randomDouble(-1.0, 1.0) * 0.04, randomDouble(-1.0, 1.0) * 0.04);
        }


        private static double randomDouble(double min, double max) {
            return min + (max - min) * new Random().nextDouble();
        }

        public void update(boolean physic) {
            this.prevPosition = this.position;
            this.prevRotateVec = this.rotateVec;

            if (physic) {
                if (isBlockSolid(this.position.x, this.position.y, this.position.z + this.velocity.z)) {
                    this.velocity = new Vec3d(this.velocity.x, this.velocity.y, -this.velocity.z * 0.8);
                }
                if (isBlockSolid(this.position.x, this.position.y + this.velocity.y, this.position.z)) {
                    this.velocity = new Vec3d(this.velocity.x * 0.999, -this.velocity.y * 0.6, this.velocity.z * 0.999);
                }
                if (isBlockSolid(this.position.x + this.velocity.x, this.position.y, this.position.z)) {
                    this.velocity = new Vec3d(-this.velocity.x * 0.8, this.velocity.y, this.velocity.z);
                }
                this.velocity = this.velocity.multiply(0.999999).subtract(0, 0.00005, 0);
            }
            this.position = this.position.add(this.velocity);

            this.rotateVec = this.rotateVec.add(this.rotateMotion);
            this.rotateMotion = this.rotateMotion.multiply(0.98);
        }

        private boolean isBlockSolid(double x, double y, double z) {
            BlockPos pos = BlockPos.ofFloored(x, y, z);
            return mc.world != null && !mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty();
        }
    }

    @Getter
    @Accessors(fluent = true)
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class ParticleAttack {
        ParticleType type;
        int index;
        int rotate;
        int color;
        float size;
        TimerUtil time = new TimerUtil();
        Animation animation = new Animation();
        @NonFinal
        Box box;
        @NonFinal
        Vec3d position;
        @NonFinal
        Vec3d prevPosition;
        @NonFinal
        Vec3d velocity;
        @NonFinal
        Vec3d rotateVec;
        @NonFinal
        Vec3d prevRotateVec;
        @NonFinal
        Vec3d rotateMotion;

        public ParticleAttack(ParticleType type, final Vec3d position, final Vec3d velocity, final int index, int rotate, int color, float size) {
            this.box = new Box(position, position).expand(size);
            this.type = type;
            this.position = position;
            this.prevPosition = position;
            this.velocity = velocity.multiply(0.01F);
            this.index = index;
            this.rotate = rotate;
            this.color = color;
            this.size = size;
            this.time.resetCounter();
            this.animation.set(0.0);

            this.rotateVec = Vec3d.ZERO;
            this.prevRotateVec = Vec3d.ZERO;
            Random rand = new Random();
            this.rotateMotion = new Vec3d((rand.nextDouble() * 2 - 1) * 0.04, (rand.nextDouble() * 2 - 1) * 0.04, (rand.nextDouble() * 2 - 1) * 0.04);
        }

        public void update(boolean physic) {
            this.prevPosition = this.position;
            this.prevRotateVec = this.rotateVec;

            if (physic) {
                if (isBlockSolid(this.position.x, this.position.y, this.position.z + this.velocity.z)) {
                    this.velocity = new Vec3d(this.velocity.x, this.velocity.y, -this.velocity.z * 0.8);
                }
                if (isBlockSolid(this.position.x, this.position.y + this.velocity.y, this.position.z)) {
                    this.velocity = new Vec3d(this.velocity.x * 0.999, -this.velocity.y * 0.6, this.velocity.z * 0.999);
                }
                if (isBlockSolid(this.position.x + this.velocity.x, this.position.y, this.position.z)) {
                    this.velocity = new Vec3d(-this.velocity.x * 0.8, this.velocity.y, this.velocity.z);
                }
                this.velocity = this.velocity.multiply(0.999999).subtract(0, 0.00005, 0);
            }
            this.position = this.position.add(this.velocity);
            this.box = new Box(this.position, this.position).expand(this.size);

            this.rotateVec = this.rotateVec.add(this.rotateMotion);
            this.rotateMotion = this.rotateMotion.multiply(0.98);
        }

        private boolean isBlockSolid(double x, double y, double z) {
            BlockPos pos = BlockPos.ofFloored(x, y, z);
            return mc.world != null && !mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty();
        }
    }
}

