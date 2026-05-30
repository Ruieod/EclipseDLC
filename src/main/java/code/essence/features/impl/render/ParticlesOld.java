package code.essence.features.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import code.essence.features.module.setting.implement.BooleanSetting;
import code.essence.features.module.setting.implement.MultiSelectSetting;
import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.events.render.WorldRenderEvent;
import code.essence.events.player.AttackEvent;
import code.essence.events.player.TickEvent;
import code.essence.events.packet.PacketEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.display.render.geometry.Render3D;
import code.essence.utils.math.frame.FrameRateCounter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.features.module.setting.implement.ColorSetting;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.client.Instance;
import net.minecraft.util.Identifier;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Iterator;


public class ParticlesOld extends Module {

    public static ParticlesOld getInstance() {
        return Instance.get(ParticlesOld.class);
    }

    SelectSetting particleType = new SelectSetting("Тип частиц", "Выбор типа").value("Кристаллы", "Снежинки", "Шарики", "Звезды", "Звезды2");
    
    MultiSelectSetting renderMode = new MultiSelectSetting("Спавнить при", "Когда рендерить партиклы")
            .value("Бездействии", "Движении", "Крите", "Броске")
            .selected("Бездействии");
    
    BooleanSetting spawnFromGround = new BooleanSetting("Спавн от земли", "Спавн частиц от земли").setValue(true);
    BooleanSetting collision = new BooleanSetting("Физика", "Коллизия частиц").setValue(true);
    SliderSettings particleCount = new SliderSettings("Количество кристаллов", "Количество кристаллов в мире").range(10, 200).setValue(50).visible(() -> particleType.isSelected("Кристаллы"));
    SliderSettings range = new SliderSettings("Дальность", "Дальность спавна кристаллов от игрока").range(8, 64).setValue(32).visible(() -> particleType.isSelected("Кристаллы"));
    SliderSettings size = new SliderSettings("Размер кристаллов", "Размер кристаллов").range(0.01F, 0.15F).setValue(0.09F).visible(() -> particleType.isSelected("Кристаллы"));


    SliderSettings maxParticles = new SliderSettings("Количество", "Количество партиклов в мире").range(10, 200).setValue(50).visible(() -> !particleType.isSelected("Кристаллы"));
    SliderSettings spawnRate = new SliderSettings("Спавн/сек", "Количество спавна частиц в секунду").range(10f, 200f).setValue(15f).visible(() -> !particleType.isSelected("Кристаллы"));
    SliderSettings spawnHeight = new SliderSettings("Высота спавна", "Высота спавна частиц").range(0.05f, 30f).setValue(10f).visible(() -> !particleType.isSelected("Кристаллы"));
    SliderSettings particleGravity = new SliderSettings("Гравитация", "Гравитация частиц").range(-10f, 10f).setValue(0f).visible(() -> !particleType.isSelected("Кристаллы"));
    SliderSettings motionPower = new SliderSettings("Скорость движения", "Сила движения частиц").range(0.1f, 2f).setValue(1f).visible(() -> !particleType.isSelected("Кристаллы"));
    SliderSettings inclineX = new SliderSettings("Наклон X", "Наклон полёта по X").range(-17.5f, 17.5f).setValue(0f).visible(() -> !particleType.isSelected("Кристаллы"));
    SliderSettings inclineZ = new SliderSettings("Наклон Z", "Наклон полёта по Z").range(-17.5f, 17.5f).setValue(0f).visible(() -> !particleType.isSelected("Кристаллы"));
    SliderSettings particleSize = new SliderSettings("Размер", "Размер частиц").setValue(1.0f).range(0.01f, 2.0f).visible(() -> !particleType.isSelected("Кристаллы"));
    SliderSettings lifeTime = new SliderSettings("Время жизни", "Время жизни частиц в мс").setValue(800f).range(250f, 3000f).visible(() -> !particleType.isSelected("Кристаллы"));
    SliderSettings spawnRange = new SliderSettings("Радиус спавна", "Радиус спавна").setValue(25f).range(10f, 50f).visible(() -> !particleType.isSelected("Кристаллы"));

    ColorSetting particleColor = new ColorSetting("Цвет", "Цвет кристаллов").value(new Color(255, 255, 255, 55).getRGB())
            .presets(new Color(0, 246, 255,255).getRGB(),
                    new Color(183, 1, 195,255).getRGB()
            ,new Color(255, 60, 0,255).getRGB()
            ,new Color(171, 253, 0,255).getRGB());
    

    SliderSettings moveCount = new SliderSettings("Кол-во при движении", "Количество партиклов при движении").range(1, 25).setValue(2).step(1F).visible(() -> renderMode.isSelected("Движении") && !particleType.isSelected("Кристаллы"));
    SliderSettings moveSpeed = new SliderSettings("Скорость при движении", "Скорость партиклов при движении").range(0.1f, 4f).setValue(1f).step(0.1F).visible(() -> renderMode.isSelected("Движении") && !particleType.isSelected("Кристаллы"));
    SliderSettings moveSize = new SliderSettings("Размер при движении", "Размер партиклов при движении").range(0.1f, 2f).setValue(1f).step(0.1F).visible(() -> renderMode.isSelected("Движении") && !particleType.isSelected("Кристаллы"));
    SliderSettings moveLifetime = new SliderSettings("Время жизни при движении", "Время жизни партиклов при движении в мс").range(2.5f, 50f).setValue(8f).step(2.5F).visible(() -> renderMode.isSelected("Движении") && !particleType.isSelected("Кристаллы"));
    

    SliderSettings critCount = new SliderSettings("Кол-во при крите", "Количество партиклов при крите").range(1, 25).setValue(2).step(1F).visible(() -> renderMode.isSelected("Крите") && !particleType.isSelected("Кристаллы"));
    SliderSettings critSpeed = new SliderSettings("Скорость при крите", "Скорость партиклов при крите").range(0.1f, 4f).setValue(1f).step(0.1F).visible(() -> renderMode.isSelected("Крите") && !particleType.isSelected("Кристаллы"));
    SliderSettings critSize = new SliderSettings("Размер при крите", "Размер партиклов при крите").range(0.1f, 2f).setValue(1f).step(0.1F).visible(() -> renderMode.isSelected("Крите") && !particleType.isSelected("Кристаллы"));
    SliderSettings critLifetime = new SliderSettings("Время жизни при крите", "Время жизни партиклов при крите в мс").range(250f, 5000f).setValue(800f).step(250F).visible(() -> renderMode.isSelected("Крите") && !particleType.isSelected("Кристаллы"));
    

    SliderSettings totemCount = new SliderSettings("Кол-во при сносе тотема", "Количество партиклов при сносе тотема").range(1, 200).setValue(75).step(1F).visible(() -> renderMode.isSelected("Сносе тотема") && !particleType.isSelected("Кристаллы"));
    SliderSettings totemSpeed = new SliderSettings("Скорость при сносе тотема", "Скорость партиклов при сносе тотема").range(0.1f, 10f).setValue(4f).step(0.1F).visible(() -> renderMode.isSelected("Сносе тотема") && !particleType.isSelected("Кристаллы"));
    SliderSettings totemSize = new SliderSettings("Размер при сносе тотема", "Размер партиклов при сносе тотема").range(0.1f, 2f).setValue(0.25f).step(0.1F).visible(() -> renderMode.isSelected("Сносе тотема") && !particleType.isSelected("Кристаллы"));
    SliderSettings totemLifetime = new SliderSettings("Время жизни при сносе тотема", "Время жизни партиклов при сносе тотема в мс").range(500f, 50000f).setValue(40000f).step(500F).visible(() -> renderMode.isSelected("Сносе тотема") && !particleType.isSelected("Кристаллы"));

    final List<WorldCrystal> crystalList = new ArrayList<>();
    final List<Particle2D> particles = new ArrayList<>();
    final List<Particle2D> critParticles = new ArrayList<>();
    final List<Particle2D> moveParticles = new ArrayList<>();
    final List<Particle2D> throwParticles = new ArrayList<>();
    final List<Particle2D> totemParticles = new ArrayList<>();
    final Random random = new Random();
    private int previousParticleCount;
    private long lastSpawnTime = 0;
    private Vec3d lastPlayerPos = null;
    private long lastMoveSpawnTime = 0;
    private long lastThrowSpawnTime = 0;

    public ParticlesOld() {
        super("Particles", "Particles", ModuleCategory.RENDER);
        setup(particleType, renderMode, spawnFromGround, collision, particleCount, range, size, maxParticles, spawnRate, spawnHeight, particleGravity, motionPower, inclineX, inclineZ, particleSize, lifeTime, spawnRange, particleColor,
                moveCount, moveSpeed, moveSize, moveLifetime,
                critCount, critSpeed, critSize, critLifetime,
                totemCount, totemSpeed, totemSize, totemLifetime);        previousParticleCount = particleCount.getInt();
        lastSpawnTime = System.currentTimeMillis();
    }

    @Override
    public void activate() {
        super.activate();
        if (particleType.isSelected("Кристаллы")) {
            generateCrystals();
        }
        previousParticleCount = particleCount.getInt();
    }

    @Override
    public void deactivate() {
        super.deactivate();
        crystalList.clear();
        particles.clear();
        critParticles.clear();
        moveParticles.clear();
        throwParticles.clear();
        totemParticles.clear();
        lastPlayerPos = null;
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null) return;

        FrameRateCounter.INSTANCE.recordFrame();

        if (particleType.isSelected("Кристаллы") && renderMode.isSelected("Бездействии")) {
            int currentCount = particleCount.getInt();
            if (currentCount != previousParticleCount) {
                adjustCrystalCount(currentCount);
                previousParticleCount = currentCount;
            }
            updateCrystals();
            renderCrystals(e.getStack());
        } else {
            
            if (renderMode.isSelected("Бездействии")) {
                Iterator<Particle2D> iterator = particles.iterator();
                while (iterator.hasNext()) {
                    Particle2D particle = iterator.next();
                    particle.update();
                    if (particle.isDead()) {
                        iterator.remove();
                    }
                }
            }

            
            if (renderMode.isSelected("Бездействии")) {
                renderParticles(e.getStack(), particles);
            }
            if (renderMode.isSelected("Крите")) {
            updateAndRenderParticles(e.getStack(), critParticles);
            }
            if (renderMode.isSelected("Движении")) {
            updateAndRenderParticles(e.getStack(), moveParticles);
            }
            if (renderMode.isSelected("Броске")) {
            updateAndRenderParticles(e.getStack(), throwParticles);
            }
            if (renderMode.isSelected("Сносе тотема")) {
            updateAndRenderParticles(e.getStack(), totemParticles);
            }
        }
    }
    
    @EventHandler
    public void onAttack(AttackEvent e) {
        if (!renderMode.isSelected("Крите") || mc.player == null) return;
        if (mc.player.fallDistance == 0) return;
        
        if (e.getEntity() == null) return;
        Entity target = e.getEntity();
        
        float motion = motionPower.getValue();
            
        int count = critCount.getInt()*5;
            for (int i = 0; i < count; i++) {
                Vec3d spawnPos = new Vec3d(
                target.getX(),
                target.getY() + randomValue(0, target.getHeight()),
                target.getZ()
                );
                
                Vec3d velocity = new Vec3d(
                randomValue(-motion, motion),
                randomValue(-motion, motion / 4F),
                randomValue(-motion, motion)
            );
                
                int lifetime = (int) critLifetime.getValue();
                int color = particleColor.getColor();
                float size = critSize.getValue();
            
            critParticles.add(new Particle2D(spawnPos, velocity.multiply(critSpeed.getValue() * 12), lifetime, color, size, 1.0f));
        }
    }
    
    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;
        
        
        if (renderMode.isSelected("Движении") && hasPlayerMoved()) {
            if (!mc.options.getPerspective().isFirstPerson()) { 
                        int count = moveCount.getInt();
                        for (int i = 0; i < count; i++) {
                            Vec3d spawnPos = new Vec3d(
                        mc.player.getX() + randomValue(-0.5, 0.5),
                        mc.player.getY() + randomValue(0, mc.player.getHeight()),
                        mc.player.getZ() + randomValue(-0.5, 0.5)
                            );
                            
                            Vec3d playerVel = mc.player.getVelocity();
                            Vec3d velocity = new Vec3d(
                        playerVel.x + randomValue(-0.25, 0.25),
                        randomValue(-0.15, 0.15),
                        playerVel.z + randomValue(-0.25, 0.25)
                            ).multiply(moveSpeed.getValue());
                            
                    int lifetime = (int) (moveLifetime.getValue() * 100);
                            int color = particleColor.getColor();
                            float size = moveSize.getValue();
                            moveParticles.add(new Particle2D(spawnPos, velocity, lifetime, color, size));
                        }
            }
        }

        
        if (renderMode.isSelected("Бездействии")) {
            int r = (int) spawnRange.getValue();
            int count = maxParticles.getInt();
            for (int i = 0; i < count; i++) {
                Vec3d additional = mc.player.getPos().add(
                    randomValue(-r, r), 0, randomValue(-r, r)
                );
                BlockPos pos = mc.world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, BlockPos.ofFloored(additional));
                Vec3d spawnPos = new Vec3d(
                    pos.getX() + randomValue(0, 1),
                    spawnFromGround.isValue() ? pos.getY() : mc.player.getY() + randomValue(mc.player.getHeight(), r),
                    pos.getZ() + randomValue(0, 1)
                );
                Vec3d velocity = new Vec3d(
                    0,
                    randomValue(0.0, motionPower.getValue()) * (spawnFromGround.isValue() ? 1 : -1),
                    0
                );
                spawnParticleIdle(spawnPos, velocity);
        }
        }

        
        if (renderMode.isSelected("Броске")) {
            for (net.minecraft.entity.Entity entity : mc.world.getEntities()) {
                    if (entity instanceof EnderPearlEntity || 
                        entity instanceof ArrowEntity || 
                    entity instanceof TridentEntity) {

                    boolean isMoving = entity.prevX != entity.getX() || 
                                       entity.prevY != entity.getY() || 
                                       entity.prevZ != entity.getZ();
                    if (!isMoving) {
                            continue;
                        }
                    Vec3d pos = entity.getPos();
                    for (int i = 0; i < 2; i++) {
                        Vec3d spawnPos = new Vec3d(
                            pos.x + randomValue(-0.2, 0.2),
                            pos.y + randomValue(-0.2, 0.2),
                            pos.z + randomValue(-0.2, 0.2)
                        );
                        Vec3d entityVel = entity.getVelocity();
                        Vec3d velocity = new Vec3d(
                            entityVel.x * 0.1 + randomValue(-0.1, 0.1),
                            entityVel.y * 0.1 + randomValue(-0.1, 0.1),
                            entityVel.z * 0.1 + randomValue(-0.1, 0.1)
                        );
                        spawnParticleThrown(spawnPos, velocity);
                    }
                }
            }
        }

        
        removeExpiredParticles(particles, (long)(lifeTime.getValue() + 500));
        removeExpiredParticles(critParticles, 5000);
        removeExpiredParticles(moveParticles, 3500);
        removeExpiredParticles(throwParticles, 3500);
        removeExpiredParticles(totemParticles, (long)totemLifetime.getValue());
    }
    
    @EventHandler
    public void onPacket(PacketEvent e) {
        if (!renderMode.isSelected("Сносе тотема")) return;
        
        if (e.getPacket() instanceof EntityStatusS2CPacket statusPacket) {
            if (statusPacket.getStatus() == 35) {
                net.minecraft.entity.Entity entity = statusPacket.getEntity(mc.world);
                if (entity != null && entity instanceof net.minecraft.entity.LivingEntity) {
                    createTotemEffect(entity.getX(), entity.getY() + entity.getHeight() / 2, entity.getZ());
                }
            }
        }
    }

    private void createTotemEffect(double x, double y, double z) {
        if (!isState() || !renderMode.isSelected("Сносе тотема")) return;

                    int count = totemCount.getInt();
                    for (int i = 0; i < count; i++) {
            double angleXZ = Math.random() * Math.PI * 2;
            double angleY = Math.random() * Math.PI;
            double strength = 0.5 + Math.random() * 0.5;
                        
                        Vec3d velocity = new Vec3d(
                            Math.sin(angleXZ) * Math.sin(angleY) * strength,
                            Math.cos(angleY) * strength,
                            Math.cos(angleXZ) * Math.sin(angleY) * strength
                        ).multiply(totemSpeed.getValue());
                        
                        Vec3d spawnPos = new Vec3d(
                x + randomValue(-0.3, 0.3),
                y + randomValue(-0.3, 0.3),
                z + randomValue(-0.3, 0.3)
                        );

            int color = Math.random() < 0.7 ? 
                            new Color(0, 255, 0, 255).getRGB() : 
                            new Color(255, 255, 0, 255).getRGB();
                        
                        int lifetime = (int) totemLifetime.getValue();
                        float size = totemSize.getValue();
                        totemParticles.add(new Particle2D(spawnPos, velocity, lifetime, color, size));
        }
    }

    private void adjustCrystalCount(int targetCount) {
        int currentSize = crystalList.size();
        if (targetCount > currentSize) {
            addCrystals(targetCount - currentSize);
        } else if (targetCount < currentSize) {
            markCrystalsForRemoval(currentSize - targetCount);
        }
    }

    private void addCrystals(int count) {
        if (mc.player == null) return;
        Vec3d playerPos = mc.player.getPos();
        float rangeValue = range.getValue();
        for (int i = 0; i < count; i++) {
            Vec3d position;
            int attempts = 0;
            do {
                double x = playerPos.x + (random.nextDouble() - 0.5) * 2 * rangeValue;
                double y = playerPos.y + (random.nextDouble() - 0.5) * rangeValue;
                double z = playerPos.z + (random.nextDouble() - 0.5) * 2 * rangeValue;
                position = new Vec3d(x, y, z);
                attempts++;
            } while (!isInPlayerView(position) && attempts < 20);
            Vec3d velocity = new Vec3d(
                    (random.nextDouble() - 0.5) * 0.02,
                    (random.nextDouble() - 0.5) * 0.02,
                    (random.nextDouble() - 0.5) * 0.02
            );
            Vec3d rotation = new Vec3d(
                    random.nextDouble() * 360,
                    random.nextDouble() * 360,
                    random.nextDouble() * 360
            );
            crystalList.add(new WorldCrystal(position, velocity, rotation));
        }
    }

    private void markCrystalsForRemoval(int count) {
        int marked = 0;
        for (WorldCrystal crystal : crystalList) {
            if (marked >= count) break;
            if (!crystal.markedForDeath && !crystal.isFadingOut) {
                crystal.markedForDeath = true;
                crystal.isFadingOut = true;
                marked++;
            }
        }
    }

    private void generateCrystals() {
        crystalList.clear();
        if (mc.player == null) return;
        Vec3d playerPos = mc.player.getPos();
        int count = particleCount.getInt();
        float rangeValue = range.getValue();
        for (int i = 0; i < count; i++) {
            Vec3d position;
            int attempts = 0;
            do {
                double x = playerPos.x + (random.nextDouble() - 0.5) * 2 * rangeValue;
                double y = playerPos.y + (random.nextDouble() - 0.5) * rangeValue;
                double z = playerPos.z + (random.nextDouble() - 0.5) * 2 * rangeValue;
                position = new Vec3d(x, y, z);
                attempts++;
            } while (!isInPlayerView(position) && attempts < 20);
            Vec3d velocity = new Vec3d(
                    (random.nextDouble() - 0.5) * 0.02,
                    (random.nextDouble() - 0.5) * 0.02,
                    (random.nextDouble() - 0.5) * 0.02
            );
            Vec3d rotation = new Vec3d(
                    random.nextDouble() * 360,
                    random.nextDouble() * 360,
                    random.nextDouble() * 360
            );
            crystalList.add(new WorldCrystal(position, velocity, rotation));
        }
    }

    private boolean isBlockOccluding(Vec3d crystalPos) {
        if (mc.world == null || mc.player == null) return false;
        
        
        if (mc.player.isTouchingWater() || mc.player.isSubmergedInWater()) {
            return false;
        }
        
        
        CameraSettings cameraSettings = CameraSettings.getInstance();
        if (cameraSettings != null && cameraSettings.isState() && cameraSettings.clipSetting.isValue()) {
            return false;
        }

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        Vec3d direction = crystalPos.subtract(cameraPos).normalize();
        double distance = crystalPos.distanceTo(cameraPos);
        double step = 0.5;
        for (double d = 0; d < distance; d += step) {
            Vec3d checkPos = cameraPos.add(direction.multiply(d));
            BlockPos blockPos = BlockPos.ofFloored(checkPos);
            if (!mc.world.getBlockState(blockPos).isAir()) {
                return true;
            }
        }
        return false;
    }

    private void updateCrystals() {
        if (mc.player == null) return;
        Vec3d playerPos = mc.player.getPos();
        float rangeValue = range.getValue();
        float fadeSpeedValue = 0.05f;

        Iterator<WorldCrystal> iterator = crystalList.iterator();
        while (iterator.hasNext()) {
            WorldCrystal crystal = iterator.next();
            crystal.prevPosition = crystal.position;
            crystal.position = crystal.position.add(crystal.velocity);

            boolean isOccluded = isBlockOccluding(crystal.position);
            boolean inView = isInPlayerView(crystal.position);

            if (crystal.markedForDeath) {
                crystal.fadeAlpha -= fadeSpeedValue;
                if (crystal.fadeAlpha <= 0) {
                    iterator.remove();
                    continue;
                }
            } else {
                if (isOccluded || !inView) {
                    if (!crystal.isFadingOut) {
                        crystal.isFadingOut = true;
                    }
                } else {
                    if (crystal.isFadingOut) {
                        crystal.isFadingOut = false;
                    }
                }

                if (crystal.isFadingOut) {
                    crystal.fadeAlpha -= fadeSpeedValue;
                    if (crystal.fadeAlpha <= 0) {
                        crystal.fadeAlpha = 0;
                        Vec3d newPosition;
                        int attempts = 0;
                        do {
                            double x = playerPos.x + (random.nextDouble() - 0.5) * 2 * rangeValue;
                            double y = playerPos.y + (random.nextDouble() - 0.5) * rangeValue;
                            double z = playerPos.z + (random.nextDouble() - 0.5) * 2 * rangeValue;
                            newPosition = new Vec3d(x, y, z);
                            attempts++;
                        } while (!isInPlayerView(newPosition) && attempts < 20);
                        crystal.position = newPosition;
                        crystal.prevPosition = crystal.position;
                        crystal.isFadingOut = false;
                    }
                } else {
                    crystal.fadeAlpha += fadeSpeedValue;
                    if (crystal.fadeAlpha > 1.0f) {
                        crystal.fadeAlpha = 1.0f;
                    }
                }

                if (crystal.position.distanceTo(playerPos) > rangeValue * 1.5) {
                    Vec3d newPosition;
                    int attempts = 0;
                    do {
                        double x = playerPos.x + (random.nextDouble() - 0.5) * 2 * rangeValue;
                        double y = playerPos.y + (random.nextDouble() - 0.5) * rangeValue;
                        double z = playerPos.z + (random.nextDouble() - 0.5) * 2 * rangeValue;
                        newPosition = new Vec3d(x, y, z);
                        attempts++;
                    } while (!isInPlayerView(newPosition) && attempts < 20);
                    crystal.position = newPosition;
                    crystal.prevPosition = crystal.position;
                    crystal.fadeAlpha = 0;
                    crystal.isFadingOut = false;
                }
            }
        }
    }

    private float getCameraYaw() {
        Camera camera = mc.gameRenderer.getCamera();
        return camera.getYaw();
    }

    private Vec3d getCameraLookVec() {
        Camera camera = mc.gameRenderer.getCamera();
        return Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
    }

    private boolean isInPlayerView(Vec3d pos) {
        if (mc.gameRenderer.getCamera() == null) return true;
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d camPos = cam.getPos();
        Vec3d look = getCameraLookVec();
        Vec3d toParticle = pos.subtract(camPos).normalize();
        return look.dotProduct(toParticle) > 0.1;
    }

    private void renderCrystals(MatrixStack ms) {
        if (mc.player == null || crystalList.isEmpty()) return;
        Camera camera = mc.gameRenderer.getCamera();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        for (WorldCrystal crystal : crystalList) {
            if (crystal.fadeAlpha <= 0) continue;

            float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
            Vec3d renderPos = crystal.prevPosition.lerp(crystal.position, tickDelta);

            if (!isInPlayerView(renderPos) && !crystal.isFadingOut) continue;

            ms.push();
            ms.translate(renderPos.x, renderPos.y, renderPos.z);
            float pulsation = 1.0f + (float) (Math.sin(System.currentTimeMillis() / 500.0) * 0.1f);
            ms.scale(pulsation, pulsation, pulsation);
            float selfRotation = (System.currentTimeMillis() % 36000) / 100.0f * crystal.rotationSpeed;
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) crystal.rotation.x));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) crystal.rotation.y + selfRotation));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) crystal.rotation.z));
            crystal.render(ms, particleColor.getColor(), camera, size.getValue(), 8);
            ms.pop();
        }
    }

    private Vec3d getRandomMotion() {
        return new Vec3d(
                (random.nextDouble() - 0.5) * 0.08,
                random.nextDouble() * 0.05,
                (random.nextDouble() - 0.5) * 0.08
        );
    }

    private void spawnParticleIdle(Vec3d position, Vec3d velocity) {
        float size = 0.05F + (particleSize.getValue() * 0.2F);
        int color = particleColor.getColor();
        int lifetime = (int) lifeTime.getValue();
        particles.add(new Particle2D(position.add(0, size, 0), velocity, lifetime, color));
    }

    private void spawnParticleThrown(Vec3d position, Vec3d velocity) {
        float size = 0.05F + (0.4F * 0.2F);
        int color = particleColor.getColor();
        int lifetime = (int) lifeTime.getValue();
        Vec3d motion = velocity.multiply(2.0);
        throwParticles.add(new Particle2D(position.add(0, size, 0), motion, lifetime, color));
    }

    private boolean hasPlayerMoved() {
        return mc.player.lastRenderX != mc.player.getX()
                || mc.player.lastRenderY != mc.player.getY()
                || mc.player.lastRenderZ != mc.player.getZ();
    }

    private double randomValue(double min, double max) {
        return random.nextDouble() * (max - min) + min;
    }

    private void removeExpiredParticles(List<Particle2D> particleList, long lifespan) {
        if (particleList == totemParticles) {
            particleList.removeIf(particle -> {
                long age = System.currentTimeMillis() - particle.birthTime;
                return age >= lifespan;
            });
        } else {
            particleList.removeIf(particle -> {
                if (!isInPlayerView(particle.pos)) return true;
                long age = System.currentTimeMillis() - particle.birthTime;
                return age >= lifespan;
            });
        }
    }

    private void updateAndRenderParticles(MatrixStack stack, List<Particle2D> particleList) {
        if (particleList.isEmpty()) return;
        
        Iterator<Particle2D> iterator = particleList.iterator();
        while (iterator.hasNext()) {
            Particle2D particle = iterator.next();
            particle.update();
            if (particle.isDead()) {
                iterator.remove();
            }
        }
        renderParticles(stack, particleList);
    }
    
    private void renderParticles(MatrixStack stack, List<Particle2D> particleList) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        RenderSystem.depthMask(true);

        String texturePath = switch (particleType.getSelected()) {
            case "Снежинки" -> "textures/particles/show1.png";
            case "Шарики" -> "textures/particles/glow.png";
            case "Кристаллы" -> "textures/particles/crystal.png";
            case "Звезды" -> "textures/particles/star1.png";
            case "Звезды2" -> "textures/particles/sparkle.png";
            default -> "textures/particles/star1.png";
        };
        Identifier textureId = Identifier.of(texturePath);

        for (Particle2D particle : particleList) {
            float alpha = particle.fade();
            if (alpha <= 0) continue;

            if (isBlockOccluding(particle.pos)) continue;

            float scaleFactor = particle.scaleFactor();
            Color baseColor = new Color(particle.colorInt);
            int r = baseColor.getRed();
            int g = baseColor.getGreen();
            int b = baseColor.getBlue();
            float finalAlpha = alpha;
            int a = (int) (finalAlpha * 255);

            int argb = new Color(r, g, b, a).getRGB();

            double posX = particle.pos.x - camPos.x;
            double posY = particle.pos.y - camPos.y;
            double posZ = particle.pos.z - camPos.z;

            MatrixStack matrices = new MatrixStack();
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
            matrices.translate(posX, posY, posZ);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            float baseScale = particle.particleSize;
            float finalScale = baseScale * scaleFactor;

            Render3D.drawTexture(
                    matrices.peek(),
                    textureId,
                    -finalScale / 2,
                    -finalScale / 2,
                    finalScale,
                    finalScale,
                    new org.joml.Vector4i(argb),
                    true
            );
            matrices.pop();
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableDepthTest();
    }

    private static class WorldCrystal {
        Vec3d position;
        Vec3d prevPosition;
        final Vec3d velocity;
        final Vec3d rotation;
        final float rotationSpeed;
        float fadeAlpha;
        boolean isFadingOut;
        boolean markedForDeath;

        public WorldCrystal(Vec3d position, Vec3d velocity, Vec3d rotation) {
            this.position = position;
            this.prevPosition = position;
            this.velocity = velocity;
            this.rotation = rotation;
            this.rotationSpeed = 0.5f + (float)(Math.random() * 1.5f);
            this.fadeAlpha = 0.0f;
            this.isFadingOut = false;
            this.markedForDeath = false;
        }

        public void render(MatrixStack ms, int baseColor, Camera camera, float size, float bloomSizeMultiplier) {
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            drawCrystal(ms, baseColor, 0.2f, true, size);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            drawCrystal(ms, baseColor, 0.3f, true, size);
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            ms.push();
            ms.scale(1.2f, 1.2f, 1.2f);
            drawCrystal(ms, baseColor, 0.3f, true, size);
            ms.pop();
            drawBloomSphere(ms, baseColor, camera, size, bloomSizeMultiplier);
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
        }

        private void drawBloomSphere(MatrixStack ms, int baseColor, Camera camera, float size, float bloomSizeMultiplier) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, Identifier.of("textures/bloom.png"));
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.depthMask(false);
            int bloomColor = ColorAssist.setAlpha(baseColor, (int)(15 * fadeAlpha));
            float bloomSize = size * bloomSizeMultiplier;
            float pitch = camera.getPitch();
            float yaw = camera.getYaw();
            int segments = 8;
            for (int i = 0; i < segments; i++) {
                ms.push();
                float angle = (360.0f / segments) * i;
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                Matrix4f matrix = ms.peek().getPositionMatrix();
                BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                bufferBuilder.vertex(matrix, -bloomSize / 2, -bloomSize / 2, 0).texture(0, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, -bloomSize / 2, 0).texture(1, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, bloomSize / 2, 0).texture(1, 0).color(bloomColor);
                bufferBuilder.vertex(matrix, -bloomSize / 2, bloomSize / 2, 0).texture(0, 0).color(bloomColor);
                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
                ms.pop();
            }
            for (int i = 0; i < segments; i++) {
                ms.push();
                float angle = (360.0f / segments) * i;
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                Matrix4f matrix = ms.peek().getPositionMatrix();
                BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                bufferBuilder.vertex(matrix, -bloomSize / 2, -bloomSize / 2, 0).texture(0, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, -bloomSize / 2, 0).texture(1, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, bloomSize / 2, 0).texture(1, 0).color(bloomColor);
                bufferBuilder.vertex(matrix, -bloomSize / 2, bloomSize / 2, 0).texture(0, 0).color(bloomColor);
                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
                ms.pop();
            }
            RenderSystem.depthMask(true);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        }

        private void drawCrystal(MatrixStack ms, int baseColor, float alpha, boolean filled, float size) {
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(
                    filled ? VertexFormat.DrawMode.TRIANGLES : VertexFormat.DrawMode.DEBUG_LINES,
                    VertexFormats.POSITION_COLOR
            );
            float s = size;
            float h_prism = size * 1f;
            float h_pyramid = size * 1.5f;
            int numSides = 8;
            List<Vec3d> topVertices = new ArrayList<>();
            List<Vec3d> bottomVertices = new ArrayList<>();
            for (int i = 0; i < numSides; i++) {
                float angle = (float) (2 * Math.PI * i / numSides);
                float x = (float) (s * Math.cos(angle));
                float z = (float) (s * Math.sin(angle));
                topVertices.add(new Vec3d(x, h_prism / 2, z));
                bottomVertices.add(new Vec3d(x, -h_prism / 2, z));
            }
            Vec3d vTop = new Vec3d(0, h_prism / 2 + h_pyramid, 0);
            Vec3d vBottom = new Vec3d(0, -h_prism / 2 - h_pyramid, 0);
            int finalColor = ColorAssist.setAlpha(baseColor, (int)(55 * fadeAlpha));
            for (int i = 0; i < numSides; i++) {
                Vec3d v1 = bottomVertices.get(i);
                Vec3d v2 = bottomVertices.get((i + 1) % numSides);
                Vec3d v3 = topVertices.get((i + 1) % numSides);
                Vec3d v4 = topVertices.get(i);
                drawQuad(ms, bufferBuilder, v1, v2, v3, v4, finalColor, filled);
            }
            for (int i = 0; i < numSides; i++) {
                Vec3d v1 = topVertices.get(i);
                Vec3d v2 = topVertices.get((i + 1) % numSides);
                drawTriangle(ms, bufferBuilder, vTop, v1, v2, finalColor, filled);
            }
            for (int i = 0; i < numSides; i++) {
                Vec3d v1 = bottomVertices.get(i);
                Vec3d v2 = bottomVertices.get((i + 1) % numSides);
                drawTriangle(ms, bufferBuilder, vBottom, v2, v1, finalColor, filled);
            }
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        }

        private void drawTriangle(MatrixStack ms, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, int color, boolean filled) {
            if (filled) {
                bb.vertex(ms.peek().getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z).color(color);
            }
        }

        private void drawQuad(MatrixStack ms, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, Vec3d v4, int color, boolean filled) {
            if (filled) {
                drawTriangle(ms, bb, v1, v2, v3, color, true);
                drawTriangle(ms, bb, v1, v3, v4, color, true);
            }
        }
    }

    class Particle2D {
        Vec3d pos;
        Vec3d vel;
        int colorInt;
        float particleSize;
        boolean hitSurface = false;
        Random rnd = new Random();
        float viewFadeAlpha = 1.0f;
        final long birthTime;
        private final int totalLife;
        private final boolean startsWithFullAlpha; 

        Particle2D(Vec3d pos, Vec3d vel, int life, int colorInt) {
            this(pos, vel, life, colorInt, ParticlesOld.this.particleSize.getValue());
        }
        
        Particle2D(Vec3d pos, Vec3d vel, int life, int colorInt, float size) {
            this(pos, vel, life, colorInt, size, 0.0f);
        }
        
        Particle2D(Vec3d pos, Vec3d vel, int life, int colorInt, float size, float initialAlpha) {
            this.pos = pos;
            
            this.vel = vel.multiply(0.01F);
            this.colorInt = colorInt;
            this.particleSize = size;
            this.birthTime = System.currentTimeMillis();
            this.totalLife = 2 * life;
            this.viewFadeAlpha = initialAlpha;
            this.startsWithFullAlpha = initialAlpha >= 1.0f; 
        }

        boolean isDead() {
            return System.currentTimeMillis() - birthTime >= totalLife;
        }

        float fade() {
            
            if (startsWithFullAlpha) {
                long ageMs = System.currentTimeMillis() - birthTime;
                float progress = Math.min(1.0f, (float) ageMs / totalLife);
                
                float fadeOut = progress > 0.5f ? (2.0f - progress * 2.0f) : 1.0f;
                return fadeOut * viewFadeAlpha;
            }
            
            
            long ageMs = System.currentTimeMillis() - birthTime;
            float progress = Math.min(1.0f, (float) ageMs / totalLife);
            float fadeInOut;
            if (progress <= 0.5f) {
                fadeInOut = progress * 2.0f;
            } else {
                fadeInOut = 2.0f - progress * 2.0f;
            }
            return fadeInOut * viewFadeAlpha;
        }

        float scaleFactor() {
            return 1.0f;
        }

        void update() {
            if (isDead()) return;

            float fps = FrameRateCounter.INSTANCE.getFps();
            float deltaTime = fps > 0 ? 1.0f / fps : 1.0f / 60.0f;
            float motionMultiplier = motionPower.getValue();
            float yaw = getCameraYaw();

            
            if (collision.isValue()) {
                
                if (isBlockSolid(pos.x, pos.y, pos.z + vel.z)) {
                    vel = new Vec3d(vel.x, vel.y, vel.z * -0.8);
                }
                
                if (isBlockSolid(pos.x, pos.y + vel.y, pos.z)) {
                    vel = new Vec3d(vel.x * 0.999, vel.y * -0.6, vel.z * 0.999);
                }
                
                if (isBlockSolid(pos.x + vel.x, pos.y, pos.z)) {
                    vel = new Vec3d(vel.x * -0.8, vel.y, vel.z);
                }
                
                vel = vel.multiply(0.999999).subtract(0, 0.00005, 0);
            } else {
                
            double xY = Math.sin(Math.toRadians(yaw));
            double zY = -Math.cos(Math.toRadians(yaw));
            double xX = -Math.sin(Math.toRadians(yaw + 90));
            double zX = Math.cos(Math.toRadians(yaw + 90));

            Vec3d addMotion = new Vec3d(
                    xY * inclineZ.getValue() / 50 + xX * inclineX.getValue() / 50,
                    0,
                    zY * inclineZ.getValue() / 50 + zX * inclineX.getValue() / 50
            );

            vel = vel.add(
                    addMotion.x * deltaTime * motionMultiplier,
                    (particleGravity.getValue() / 80) * deltaTime * motionMultiplier,
                    addMotion.z * deltaTime * motionMultiplier
            );
            vel = vel.add(0, -0.0002, 0);
            }

            
            pos = pos.add(vel);

            boolean inView = isInPlayerView(pos);
            if (inView) {
                
                viewFadeAlpha += deltaTime * 100.0f; 
                if (viewFadeAlpha > 1.0f) viewFadeAlpha = 1.0f;
            } else {
                viewFadeAlpha -= deltaTime * 1.0f;
                if (viewFadeAlpha < 0) viewFadeAlpha = 0;
            }
        }

        private boolean isBlockSolid(double x, double y, double z) {
            if (mc.world == null) return false;
            BlockPos blockPos = BlockPos.ofFloored(x, y, z);
            return !mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty();
        }
    }
}