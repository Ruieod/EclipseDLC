package code.essence.features.impl.combat.ElytraTargetUtil;

import code.essence.features.impl.combat.Aura;
import code.essence.features.impl.combat.ElytraAura;
import code.essence.utils.client.Instance;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.features.aura.rotations.constructor.RotateConstructor;
import code.essence.utils.features.aura.warp.Turns;
import code.essence.utils.features.aura.warp.TurnsConfig;
import code.essence.utils.features.aura.warp.TurnsConnection;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.math.task.TaskPriority;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.concurrent.ThreadLocalRandom;


@UtilityClass
public final class ElytraTargetUtil implements QuickImports {
    private float lastAntiAimYaw = 0f;
    private long antiAimStartMs = 0L;
    private boolean antiAimActive = false;
    private Turns defensiveAngle = new Turns(0, 0);
    private boolean lastHitRandom = false;

    private ElytraAura elytraTargetaModule;


    private Vec2f[] getDefensiveYawOffsets() {
        ElytraAura el = ElytraAura.getInstance();
        String mode = (el != null) ? el.getDefensiveType().getSelected() : "Горизонтальный";

        return switch (mode) {
            case "Рандомный" -> new Vec2f[]{
                    new Vec2f(180, 0),
                    new Vec2f(135, 0),
                    new Vec2f(90, 0),
                    new Vec2f(45, 0),
                    new Vec2f(-45, 0),
                    new Vec2f(-90, 0),
                    new Vec2f(-135, 0),
                    new Vec2f(0, 0)
            };
            case "Горизонтальный", "Крутящийся" -> new Vec2f[]{new Vec2f(360, 0)};
            case "Односторонний" -> new Vec2f[]{new Vec2f(180, 0)};
            default -> new Vec2f[]{new Vec2f(0, 0)};
        };
    }


    private Vec3d getLookVector(float yaw, float pitch) {
        float radYaw = (float) Math.toRadians(yaw);
        float radPitch = (float) Math.toRadians(pitch);
        float cosPitch = MathHelper.cos(-radPitch);
        return new Vec3d(
                -MathHelper.sin(radYaw) * cosPitch,
                -MathHelper.sin(-radPitch),
                MathHelper.cos(radYaw) * cosPitch
        );
    }


    private Turns computeDefensiveAngle(LivingEntity target) {
        if (mc.player == null || mc.world == null || target == null) {
            return new Turns(0, 0);
        }

        ElytraAura el = ElytraAura.getInstance();
        if (el == null) {
            return new Turns(0, 0);
        }

        Vec2f[] yawOffsets = getDefensiveYawOffsets();
        Vec2f[] pitchOffsets = new Vec2f[]{
                new Vec2f(0, el.getDefensivePitchOffset().getValue())
        };

        java.util.List<Vec2f> availableYawOffsets = new java.util.ArrayList<>();

        float targetYaw = (float) Math.toDegrees(Math.atan2(
                target.getEyePos().z - mc.player.getEyePos().z,
                target.getEyePos().x - mc.player.getEyePos().x
        )) - 90.0f;

        for (Vec2f offset : yawOffsets) {
            float potentialYaw = MathHelper.wrapDegrees(targetYaw + offset.x);
            if (Math.abs(MathHelper.wrapDegrees(potentialYaw - lastAntiAimYaw)) > 45.0f) {
                availableYawOffsets.add(offset);
            }
        }

        if (availableYawOffsets.isEmpty()) {
            availableYawOffsets.addAll(java.util.Arrays.asList(yawOffsets));
        }

        java.util.Random random = new java.util.Random();

        Vec2f selectedYawOffset = availableYawOffsets.get(random.nextInt(availableYawOffsets.size()));
        float yaw = MathHelper.wrapDegrees(targetYaw + selectedYawOffset.x);

        Vec2f selectedPitchOffset = pitchOffsets[random.nextInt(pitchOffsets.length)];
        float pitch = selectedPitchOffset.y;

        Vec3d lookVec = getLookVector(yaw, pitch);
        BlockHitResult collisionResult = mc.world.raycast(
                new RaycastContext(
                        mc.player.getEyePos(),
                        mc.player.getEyePos().add(lookVec.multiply(20.0)),
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        mc.player
                )
        );

        if (collisionResult.getType() == HitResult.Type.BLOCK) {
            for (Vec2f offset : availableYawOffsets) {
                if (offset.equals(selectedYawOffset)) continue;

                float alternativeYaw = MathHelper.wrapDegrees(targetYaw + offset.x);
                Vec3d altLookVec = getLookVector(alternativeYaw, pitch);

                BlockHitResult altResult = mc.world.raycast(
                        new RaycastContext(
                                mc.player.getEyePos(),
                                mc.player.getEyePos().add(altLookVec.multiply(20.0)),
                                RaycastContext.ShapeType.COLLIDER,
                                RaycastContext.FluidHandling.NONE,
                                mc.player
                        )
                );

                if (altResult.getType() != HitResult.Type.BLOCK) {
                    yaw = alternativeYaw;
                    break;
                }
            }
        }

        lastAntiAimYaw = yaw;
        return new Turns(yaw, pitch);
    }


    private void startDefensiveAntiAimAfterHit() {
        LivingEntity target = Aura.getInstance().getTarget();
        if (target == null || mc.player == null) return;
        if (!isElytraFlying()) return;

        ElytraAura el = ElytraAura.getInstance();
        if (el == null || !el.getDefensiveEnabled().isValue()) return;

        lastHitRandom = ThreadLocalRandom.current().nextBoolean();
        defensiveAngle = computeDefensiveAngle(target);
        antiAimActive = true;
        antiAimStartMs = System.currentTimeMillis();
    }


    public boolean isElytraFlying() {
        if (mc.player == null) return false;
        return mc.player.isGliding() &&
                mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
    }


    public void attackInElytra() {
        LivingEntity target = Aura.getInstance().getTarget();
        if (target == null || mc.player == null) return;

        float distance = mc.player.distanceTo(target);
        float attackRange = Aura.attackRange.getValue() * 1.5f;
        if (distance > attackRange) return;

        if (mc.player.getAttackCooldownProgress(0.5f) < 1.0f) {
            return;
        }

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        startDefensiveAntiAimAfterHit();
    }


    public void triggerElytraDefensive() {
        startDefensiveAntiAimAfterHit();
    }


    private ElytraAura getElytraTargeta() {
        if (elytraTargetaModule == null) {
            elytraTargetaModule = Instance.get(ElytraAura.class);
        }
        return elytraTargetaModule;
    }


    public static boolean shouldUseElytraMode() {
        return isElytraFlying() && isElytraAuraEnabled();
    }


    private boolean isElytraAuraEnabled() {
        ElytraAura elytra = getElytraTargeta();
        return elytra != null && elytra.isState();
    }



    public void rotateToTargetElytraUltraFast() {
        LivingEntity target = Aura.getInstance().getTarget();
        if (target == null || mc.player == null) return;

        ElytraAura el = ElytraAura.getInstance();

        if (antiAimActive && el != null && el.getDefensiveEnabled().isValue()) {
            long dt = System.currentTimeMillis() - antiAimStartMs;
            if (dt > el.getDefensiveTime().getValue()) {
                antiAimActive = false;
            }
        }

        Vec3d aimPos = predictElytraTarget((LivingEntity) target);

        Vec3d eye = mc.player.getEyePos();
        Vec3d dir = aimPos.subtract(eye);
        if (dir.lengthSquared() < 1.0E-6) return;

        float baseYaw = (float) MathHelper.wrapDegrees(
                Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0
        );
        float basePitch = (float) -Math.toDegrees(
                Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z))
        );

        float yaw = baseYaw;
        float pitch = basePitch;

        if (antiAimActive && el != null && el.getDefensiveEnabled().isValue()) {
            if (el.getJitterOnDefensiveSetting().isValue()) {
                yaw += (float) Calculate.getRandom(-20.0, 20.0);
            }

            if (el.getDefensiveType().isSelected("Roll")) {
                yaw += el.getStrenghtRollSetting().getValue() * (lastHitRandom ? -1.0f : 1.0f);
            }

            float finalYaw = el.getDefensiveType().isSelected("Roll") ? yaw : defensiveAngle.getYaw();
            float finalPitch = defensiveAngle.getPitch();

            yaw = finalYaw;
            pitch = finalPitch;
        }

        if (el != null && el.getRangeByter().isValue() && !antiAimActive) {
            LivingEntity entity = target;
            double dist = mc.player.distanceTo(entity);
            if (dist < 0.5) dist = 0.5;

            double wrap = Math.atan2(mc.player.getZ() - entity.getZ(),
                    mc.player.getX() - entity.getX());
            wrap += 4.0 / dist;

            double x = entity.getX() + 5.0 * Math.cos(wrap);
            double z = entity.getZ() + 5.0 * Math.sin(wrap);

            double diffX = x - mc.player.getX();
            double diffZ = z - mc.player.getZ();

            double d1 = Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0;
            yaw = (float) MathHelper.wrapDegrees(d1);
        }
        ElytraAura elytra = ElytraAura.getInstance();
        boolean elytra1 = elytra != null && elytra.lock.isValue();
        if (elytra1) {
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        } else {

        }
        pitch = MathHelper.clamp(pitch, -89.0f, 89.0f);

        if (TurnsConnection.INSTANCE != null) {
            Turns turns = new Turns(yaw, pitch);
            Turns.VecRotation rotation = new Turns.VecRotation(turns, turns.toVector());
            TurnsConfig config = new TurnsConfig(new ElytraOptimizedMode(), true, false);
            ElytraAura elytraModule = ElytraAura.getInstance();
            if (elytraModule != null) {
                TurnsConnection.INSTANCE.rotateTo(rotation, target, 1, config,
                        TaskPriority.HIGH_IMPORTANCE_1, elytraModule);
            }
        }
    }


    private Vec3d predictElytraTarget(LivingEntity t) {
        Vec3d pos = t.getPos();
        Vec3d vel = t.getVelocity();

        ElytraAura el = ElytraAura.getInstance();
        boolean extraOverEnabled = el != null && el.isState() && ElytraAura.shouldElytraTarget;

        if (!extraOverEnabled) {
            double bodyY = pos.y + t.getHeight() * (t.isGliding() ? 0.55 : 0.5);
            return new Vec3d(pos.x, bodyY, pos.z);
        }

        double dist = mc.player.distanceTo(t);
        double targetSpeed = vel.horizontalLength();

        double leadTicks = 0.5;
        if (targetSpeed > 0.1) {
            leadTicks = 0.3 + (dist * 0.03) + (targetSpeed * 1.0);
        }
        if (!t.isGliding()) {
            leadTicks *= 0.4;
        }
        leadTicks = MathHelper.clamp(leadTicks, 0.3, 3.0);

        Vec3d predicted = pos.add(vel.multiply(leadTicks));

        if (el.elytraForward.getValue() > 0.01f) {
            float over = el.elytraForward.getValue();
            Vec3d dir = (vel.lengthSquared() > 1.0E-6)
                    ? vel.normalize()
                    : t.getRotationVector();

            double maxOver = mc.player.distanceTo(t) * 0.5;
            over = (float) Math.min(over, maxOver);

            predicted = predicted.add(dir.multiply(over));
        }

        double bodyY = predicted.y + t.getHeight() * (t.isGliding() ? 0.55 : 0.5);
        return new Vec3d(predicted.x, bodyY, predicted.z);
    }


    public Vec3d getElytraResolvedPoint(LivingEntity entity, boolean fromPlayerEye) {
        if (mc.player == null || entity == null) return Vec3d.ZERO;

        Vec3d eyePos = mc.player.getEyePos();

        Vec3d basePoint = entity.getPos().add(0, entity.getHeight() * 0.5, 0);

        if (!entity.isGliding() || entity.isOnGround()) {
            return fromPlayerEye ? basePoint.subtract(eyePos) : basePoint;
        }

        Vec3d unLerped = new Vec3d(entity.prevX, entity.prevY, entity.prevZ);

        Vec3d resolved = entity.getPos();

        Vec3d tickVel = resolved.subtract(unLerped);

        float cooldown = mc.player.getAttackCooldownProgress(1.5f) - 0.01f;
        cooldown = MathHelper.clamp(cooldown, 0.0f, 1.5f);
        Vec3d point = basePoint.add(tickVel.multiply(cooldown * 4.0));

        double latencyTicks = 0.4;
        if (entity instanceof PlayerEntity p && mc.getNetworkHandler() != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
            if (entry != null) {
                latencyTicks = entry.getLatency() / 50.0;
            }
        }

        boolean shouldPredictMove =
                entity.isGliding()
                        && tickVel.lengthSquared() > 1.0E-6;

        double mul = shouldPredictMove ? latencyTicks : 0.4;
        point = point.add(tickVel.multiply(mul));

        if (ElytraAura.getInstance() != null
                && ElytraAura.getInstance().isState()
                && ElytraAura.shouldElytraTarget) {
            float over = ElytraAura.getInstance().elytraForward.getValue();
            Vec3d dir = (tickVel.lengthSquared() > 1.0E-6) ? tickVel.normalize() : entity.getRotationVector();
            double maxOver = mc.player.distanceTo(entity) * 0.5;
            over = (float) Math.min(over, maxOver);
            point = point.add(dir.multiply(over));
        }
        return fromPlayerEye ? point.subtract(eyePos) : point;
    }


    public RotateConstructor getSmoothMode() {
        if (isElytraFlying() && shouldUseElytraMode()) {
            return new ElytraOptimizedMode();
        }
        return null;
    }
}
