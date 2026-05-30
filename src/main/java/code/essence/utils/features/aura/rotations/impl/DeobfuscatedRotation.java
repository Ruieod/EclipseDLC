package code.essence.utils.features.aura.rotations.impl;

import code.essence.Essence;
import code.essence.features.impl.combat.Aura;
import code.essence.utils.features.aura.rotations.constructor.RotateConstructor;
import code.essence.utils.features.aura.striking.StrikeManager;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

import static code.essence.utils.display.interfaces.QuickImports.mc;


public class DeobfuscatedRotation extends RotateConstructor {
    
    
    private float baseYaw = 0.0F;
    private long lastShakeTime = 0L;
    private boolean isPaused = false;
    private float pauseProgress = 0.0F;
    
    
    private boolean dynamicXZEnabled = true;
    private boolean hitboxShakeEnabled = true;
    private double currentXOffset = 0.0;
    private double currentZOffset = 0.0;
    private double targetXOffset = 0.0;
    private double targetZOffset = 0.0;
    private long lastDynamicUpdate = 0L;
    private long nextDynamicUpdate = 0L;
    
    
    private boolean isDodging = false;
    private boolean isReturning = false;
    private long dodgeStartTime = 0L;
    private long dodgeDuration = 0L;
    private long returnDuration = 0L;
    private float dodgeYawOffset = 0.0F;
    private float dodgePitchOffset = 0.0F;
    private int lastSwingTicks = 0;
    
    
    private boolean pauseActive = false;
    private long pauseStartTime = 0L;
    private long pauseDuration = 0L;
    private long nextPauseTime = 0L;
    
    
    private boolean hitboxShakeActive = false;
    private long hitboxShakeStartTime = 0L;
    private long hitboxShakeDuration = 0L;
    private Vec3d hitboxShakeOffset = Vec3d.ZERO;
    private long lastHitboxShakeUpdate = 0L;
    private double shakeOffsetX = 0.0;
    private double shakeOffsetY = 0.0;
    private double shakeOffsetZ = 0.0;
    private int shakeDirection = 1;
    
    
    private float attackShakeAmount = 0.0F;
    
    
    private static final float MIN_SPEED_Y = 5.0f;
    private static final float MAX_SPEED_Y = 80.0f;
    private static final float MIN_SPEED_P = 5.0f;
    private static final float MAX_SPEED_P = 30.0f;
    private static final float SHAKE_SPEED = 1.0f;
    private static final float SHAKE_AMOUNT = 2.0f;
    
    public DeobfuscatedRotation() {
        super("Deobfuscated");
    }
    
    @Override
    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
        if (!(entity instanceof LivingEntity target)) {
            return currentAngle;
        }
        
        StrikeManager attackHandler = Essence.getInstance().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        boolean canAttack = attackHandler.canAttack(aura.getConfig(), 0);
        boolean isAttack = canAttack;
        
        long currentTime = System.currentTimeMillis();
        long shakeTime = (long) (System.currentTimeMillis() / (5.0F / SHAKE_SPEED));
        
        
        if (hitboxShakeEnabled) {
            updateHitboxShake(currentTime);
        }
        
        
        float oscillY = (float) Math.cos(shakeTime / 850.0);
        float offsetY = 0.16F * oscillY;
        float oscillZ = (float) Math.sin(shakeTime / 350.0);
        float offsetZ = 0.35F * oscillZ;
        float oscillX = (float) Math.cos(shakeTime / 340.0);
        float offsetX3 = 0.31F * oscillX;
        float oscillX3 = (float) Math.sin(shakeTime / 880.0);
        float offsetX = 0.3F * oscillX3;
        
        
        double currentY = 0.0;
        double currentXOffset = 0.0;
        double currentZOffset = 0.0;
        
        if (dynamicXZEnabled) {
            if (currentTime - lastDynamicUpdate >= nextDynamicUpdate && pauseProgress >= 1.0F) {
                targetXOffset = this.currentXOffset;
                targetZOffset = this.currentZOffset;
                this.currentXOffset = ThreadLocalRandom.current().nextDouble(-0.2, 0.2);
                this.currentZOffset = ThreadLocalRandom.current().nextDouble(-0.2, 0.2);
                pauseProgress = 0.0F;
                lastDynamicUpdate = currentTime;
                nextDynamicUpdate = ThreadLocalRandom.current().nextLong(1000L, 3000L);
            }
            
            if (pauseProgress < 1.0F) {
                pauseProgress += 0.06F;
                if (pauseProgress > 1.0F) {
                    pauseProgress = 1.0F;
                }
            }
            
            currentXOffset = targetXOffset + (this.currentXOffset - targetXOffset) * pauseProgress;
            currentZOffset = targetZOffset + (this.currentZOffset - targetZOffset) * pauseProgress;
        }
        
        
        float baseYaw = targetAngle.getYaw();
        float basePitch = targetAngle.getPitch();
        
        
        if (hitboxShakeEnabled && hitboxShakeActive) {
            Vec3d playerEyePos = mc.player.getEyePos();
            Vec3d targetPos = target.getPos().add(hitboxShakeOffset);
            Vec3d directionVec = targetPos
                    .add(offsetX + currentXOffset, currentY + offsetX3, offsetZ + currentZOffset)
                    .subtract(playerEyePos)
                    .normalize();
            
            baseYaw = (float) Math.toDegrees(Math.atan2(-directionVec.x, directionVec.z));
            basePitch = (float) MathHelper.clamp(
                    -Math.toDegrees(Math.atan2(directionVec.y, Math.hypot(directionVec.x, directionVec.z))),
                    -90.0F, 90.0F);
        }
        
        this.baseYaw = baseYaw;
        
        
        int currentSwingTicks = mc.player.handSwingTicks;
        boolean justAttacked = currentSwingTicks < lastSwingTicks;
        
        if (justAttacked && !isDodging && !isReturning) {
            startDodge(currentTime);
        }
        
        lastSwingTicks = currentSwingTicks;
        
        
        if (isDodging) {
            long elapsedDodgeTime = currentTime - dodgeStartTime;
            if (elapsedDodgeTime >= dodgeDuration) {
                isDodging = false;
                isReturning = true;
                dodgeStartTime = currentTime;
            } else {
                float dodgeProgress = (float) elapsedDodgeTime / (float) dodgeDuration;
                float easedProgress = easeOutCubic(dodgeProgress);
                baseYaw += dodgeYawOffset * easedProgress;
                basePitch += dodgePitchOffset * easedProgress;
            }
        }
        
        
        if (isReturning) {
            long elapsedReturnTime = currentTime - dodgeStartTime;
            if (elapsedReturnTime >= returnDuration) {
                isReturning = false;
                baseYaw = this.baseYaw;
            } else {
                float returnProgress = (float) elapsedReturnTime / (float) returnDuration;
                float easedProgress = easeOutCubic(returnProgress);
                float currentYawOffset = dodgeYawOffset * (1.0F - easedProgress);
                float currentPitchOffset = dodgePitchOffset * (1.0F - easedProgress);
                baseYaw = this.baseYaw + currentYawOffset;
                basePitch += currentPitchOffset * (1.0F - easedProgress);
            }
        }
        
        
        if (isPaused) {
            if (currentTime - pauseStartTime < pauseDuration) {
                
                return currentAngle;
            }
            isPaused = false;
            nextPauseTime = currentTime + (long) randomRange(30.0F, 60.0F);
        } else {
            if (nextPauseTime == 0L) {
                nextPauseTime = currentTime + (long) randomRange(30.0F, 60.0F);
            }
            
            if (currentTime >= nextPauseTime) {
                isPaused = true;
                pauseStartTime = currentTime;
                pauseDuration = (long) randomRange(3.0F, 7.0F);
                nextPauseTime = 0L;
            }
        }
        
        
        float waveB = (float) Math.cos(System.currentTimeMillis() / 328.0F);
        if (isAttack) {
            attackShakeAmount = (float) randomRange(1.0F, 6.0F);
        }
        
        
        float randomAttackShift = 0.0F;
        if (attackShakeAmount > 0.0F && mc.player.getPos().distanceTo(target.getPos()) < 3.0) {
            float waveA = (float) Math.cos(System.currentTimeMillis() / 150.0F);
            randomAttackShift = (float) randomRange(0.0F, 0.0F);
            attackShakeAmount--;
        }
        
        
        float waveA = (float) Math.sin(shakeTime / 220.0F);
        float yawJitter = waveB * SHAKE_AMOUNT * waveA;
        float pitchJitter = waveA * SHAKE_AMOUNT * 0.5F;
        
        
        float modifiedYaw = baseYaw + yawJitter + randomAttackShift;
        float modifiedPitch = MathHelper.clamp(basePitch + pitchJitter + randomAttackShift, -90.0F, 90.0F);
        
        Turns modifiedTarget = new Turns(modifiedYaw, modifiedPitch);
        
        
        Turns delta = MathAngle.calculateDelta(currentAngle, modifiedTarget);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        
        
        float yawChangeSpeed = (float) randomRange(MIN_SPEED_Y, MAX_SPEED_Y);
        float pitchChangeSpeed = (float) randomRange(MIN_SPEED_P, MAX_SPEED_P);
        
        
        float yawStep = MathHelper.clamp(yawDelta, -yawChangeSpeed, yawChangeSpeed);
        float pitchStep = MathHelper.clamp(pitchDelta, -pitchChangeSpeed, pitchChangeSpeed);
        
        float newYaw = currentAngle.getYaw() + yawStep;
        float newPitch = MathHelper.clamp(currentAngle.getPitch() + pitchStep, -90.0F, 90.0F);
        
        return new Turns(newYaw, newPitch).adjustSensitivity();
    }
    
    
    private void updateHitboxShake(long currentTime) {
        if (!hitboxShakeActive) {
            if (lastHitboxShakeUpdate == 0L) {
                lastHitboxShakeUpdate = currentTime + ThreadLocalRandom.current().nextLong(1000L, 3001L);
            }
            
            if (currentTime >= lastHitboxShakeUpdate) {
                hitboxShakeActive = true;
                hitboxShakeStartTime = currentTime;
                hitboxShakeDuration = ThreadLocalRandom.current().nextLong(50L, 151L);
                lastHitboxShakeUpdate = 0L;
                shakeOffsetX = ThreadLocalRandom.current().nextDouble(0.3, 1.0F);
                shakeOffsetY = ThreadLocalRandom.current().nextDouble(0.1, 0.3);
                shakeOffsetZ = ThreadLocalRandom.current().nextDouble(0.3, 1.0F);
                shakeDirection = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
            }
        } else {
            long elapsedShakeTime = currentTime - hitboxShakeStartTime;
            if (elapsedShakeTime >= hitboxShakeDuration) {
                hitboxShakeActive = false;
                hitboxShakeOffset = Vec3d.ZERO;
            } else {
                float progress = (float) elapsedShakeTime / (float) hitboxShakeDuration;
                float intensity = (float) Math.sin(progress * Math.PI);
                float extraWave1 = (float) Math.sin(progress * Math.PI * 3.0F) * 0.5F;
                float extraWave2 = (float) Math.cos(progress * Math.PI * 5.0F) * 0.3F;
                float combinedIntensity = intensity * 0.7F + extraWave1 * 0.2F + extraWave2 * 0.1F;
                
                double offsetX = shakeOffsetX * combinedIntensity * shakeDirection;
                double offsetY = shakeOffsetY * combinedIntensity * shakeDirection * 0.5F;
                double offsetZ = shakeOffsetZ * combinedIntensity * (shakeDirection * -1);
                
                hitboxShakeOffset = new Vec3d(offsetX, offsetY, offsetZ);
            }
        }
    }
    
    
    private void startDodge(long currentTime) {
        isDodging = true;
        isReturning = false;
        dodgeStartTime = currentTime;
        dodgeDuration = ThreadLocalRandom.current().nextLong(150L, 250L);
        returnDuration = ThreadLocalRandom.current().nextLong(150L, 200L);
        int direction = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
        boolean pitchUp = ThreadLocalRandom.current().nextBoolean();
        dodgeYawOffset = (float) randomRange(20.0F, 35.0F) * direction;
        dodgePitchOffset = (float) randomRange(10.0F, 25.0F) * (pitchUp ? 1 : -1);
    }
    
    
    private float easeOutCubic(float x) {
        return 1.0F - (float) Math.pow(1.0F - x, 3.0);
    }
    
    
    private double randomRange(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }
    
    @Override
    public Vec3d randomValue() {
        
        return new Vec3d(
                randomRange(-0.1, 0.1),
                randomRange(-0.1, 0.1),
                randomRange(-0.1, 0.1)
        );
    }
}
