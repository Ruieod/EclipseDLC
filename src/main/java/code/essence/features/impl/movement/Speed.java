package code.essence.features.impl.movement;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import code.essence.features.module.setting.implement.BooleanSetting;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.features.impl.combat.Aura;
import code.essence.events.player.PlayerTravelEvent;
import code.essence.events.player.TickEvent;
import code.essence.utils.interactions.simulate.Simulations;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.AxisAngle4d;

import static code.essence.utils.interactions.simulate.Simulations.forward;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Speed extends Module {

    SelectSetting mode = new SelectSetting("Режим", "Выберите режим скорости")
            .value("Обычный", "Грим", "ХолиВорлд", "ФанТайм")
            .selected("Грим");

    SliderSettings speed = new SliderSettings("Скорость", "Настройка скорости передвижения")
            .range(1.0f, 5.0f)
            .setValue(1.5f)
            .visible(() -> mode.getSelected().equals("Обычный"));

    BooleanSetting up = new BooleanSetting("Усиление","Увеличивает дистанцию ускорения до цели в Aura").setValue(true).visible(() -> mode.getSelected().equals("Грим"));

    SliderSettings strength = new SliderSettings("Сила", "Фактор умножения до цели")
            .range(1.0f, 6.0f)
            .setValue(1.5f)
            .visible(() -> mode.getSelected().equals("Грим") && up.isValue());

    
    final SliderSettings bypassDistance = new SliderSettings("Дистанция", "Дистанция обнаружения коллизий")
            .range(0.1f, 2.0f)
            .setValue(0.3f)
            .step(0.01f)
            .visible(() -> mode.getSelected().equals("ХолиВорлд"));

    final SliderSettings bypassSpeed = new SliderSettings("Скорость", "Скорость байпаса")
            .range(0.1f, 20.0f)
            .setValue(3.1f)
            .step(0.1f)
            .visible(() -> mode.getSelected().equals("ХолиВорлд"));



    
    Vec3d currentVelocity = Vec3d.ZERO;
    int grimTargetCollisions = 0;
    long lastCollisionCheck = 0;

    public Speed() {
        super("Speed", "Speed", ModuleCategory.MOVEMENT);
        setup(mode, up, strength, speed, bypassDistance, bypassSpeed);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        String modeVal = mode.getSelected();
        if (modeVal.equals("Обычный")) {
            Simulations.setVelocity(speed.getValue() / 3);
        }
    }

    @EventHandler

    public void onMotion(PlayerTravelEvent e) {
        String modeVal = mode.getSelected();
        if (modeVal.equals("ФанТайм")) {
            if (!mc.player.isSwimming() && !mc.player.isGliding() && !mc.player.isSneaking()) {
                if (mc.player.getBoundingBox().maxY - mc.player.getBoundingBox().minY < 1.5f) {
                    float motion = mc.player.hasStatusEffect(StatusEffects.SPEED) ? 0.32f : 0.28f;
                    Simulations.setVelocity(motion);
                }
            }
        }
        if (modeVal.equals("Грим") && e.isPre() && Simulations.hasPlayerMovement()) {
            int collisions = 0;
            float box = 0.4F;
            if (Aura.getInstance().isState() && Aura.getInstance().getTarget() != null && Aura.getInstance().getTarget().isSprinting() && mc.player.isSprinting() && up.isValue()) {
                box = strength.getValue();
            }
            for (Entity ent : mc.world.getEntities())
                if (ent != mc.player && (!(ent instanceof ArmorStandEntity)) && (ent instanceof LivingEntity || ent instanceof BoatEntity) && mc.player.getBoundingBox().expand(box).intersects(ent.getBoundingBox()))
                    collisions++;
            double[] motion = forward(0.08 * collisions);
            mc.player.addVelocity(motion[0], 0, motion[1]);
        }
        if (modeVal.equals("ХолиВорлд") && e.isPre()) {
            if (System.currentTimeMillis() - lastCollisionCheck > 100) {
            updateGrimTargetCollisions();
            lastCollisionCheck = System.currentTimeMillis();
            }
            
            if (grimTargetCollisions > 0) {
            applyGrimTargetBypass();
            }
        }
    }

    private void updateGrimTargetCollisions() {
        grimTargetCollisions = 0;
        if (mc.player == null || mc.world == null) return;

        float expandValue = bypassDistance.getValue();
        Box expandedBox = mc.player.getBoundingBox().expand(expandValue);

        for (Entity ent : mc.world.getEntities()) {
            if (ent == mc.player) continue;
            if (ent instanceof ArmorStandEntity) continue; 
            if (!(ent instanceof LivingEntity) && !(ent instanceof BoatEntity)) continue;

            if (expandedBox.intersects(ent.getBoundingBox())) {
                grimTargetCollisions++;
            }
        }
    }

    private void applyGrimTargetBypass() {
        if (grimTargetCollisions <= 0) return;

        LivingEntity target = Aura.getInstance().getTarget();
        if (target != null && target.isAlive()) {
            double distance = mc.player.getPos().distanceTo(target.getPos());
            double maxRange =3f;

            if (distance <= maxRange) {
                applySpeedBoost(target);
                return;
            }
        }

        double maxRange = 3f;
        double maxRangeSq = maxRange * maxRange;

        Entity nearest = null;
        double bestSq = Double.MAX_VALUE;

        for (Entity ent : mc.world.getEntities()) {
            if (ent == mc.player) continue;
            if (ent instanceof ArmorStandEntity) continue; 
            if (!(ent instanceof LivingEntity) && !(ent instanceof BoatEntity)) continue;

            double dx = ent.getX() - mc.player.getX();
            double dz = ent.getZ() - mc.player.getZ();
            double sq = dx * dx + dz * dz;

            if (sq <= maxRangeSq && sq < bestSq) {
                bestSq = sq;
                nearest = ent;
            }
        }

        if (nearest != null) {
            applySpeedBoost(nearest);
        }
    }

    private void applySpeedBoost(Entity targetEntity) {
        double finalSpeed = bypassSpeed.getValue() * 0.01 * grimTargetCollisions;
        if (finalSpeed <= 0.0) return;

        double[] direction = getDirectionToPoint(mc.player.getPos(), targetEntity.getPos(), finalSpeed);
        mc.player.addVelocity(direction[0], 0.0, direction[1]);

        Vec3d currentVel = mc.player.getVelocity();
        currentVelocity = new Vec3d(currentVel.x, currentVel.y, currentVel.z);
    }

    private double[] getDirectionToPoint(Vec3d from, Vec3d to, double speed) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double length = Math.sqrt(dx * dx + dz * dz);

        if (length == 0) return new double[]{0.0, 0.0};

        return new double[]{dx / length * speed, dz / length * speed};
    }

    @Override
    public void activate() {
        super.activate();
        grimTargetCollisions = 0;
        lastCollisionCheck = System.currentTimeMillis();
    }

    @Override
    public void deactivate() {
        super.deactivate();
        grimTargetCollisions = 0;
    }

    private boolean hasSprintingTarget() {
        return false;
    }
}