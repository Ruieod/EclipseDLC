package code.essence.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import code.essence.utils.client.managers.event.EventManager;
import code.essence.events.block.PushEvent;
import code.essence.events.item.SwingDurationEvent;
import code.essence.events.player.EntityDeathEvent;
import code.essence.events.player.JumpEvent;
import code.essence.utils.features.aura.warp.TurnsConnection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow public abstract boolean hasStatusEffect(RegistryEntry<StatusEffect> effect);
    @Shadow @Nullable public abstract StatusEffectInstance getStatusEffect(RegistryEntry<StatusEffect> effect);
    
    @Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true)
    private void onHasStatusEffect(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        if (effect != null && effect.value() == StatusEffects.NAUSEA) {
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            if (client != null && client.player != null && (Object) this == client.player) {
                code.essence.features.impl.render.NoRender noRender = code.essence.features.impl.render.NoRender.getInstance();
                if (noRender != null && noRender.shouldHideBadEffects()) {
                    cir.setReturnValue(false);
                }
            }
        }
    }

    @Inject(method = "getStuckArrowCount", at = @At("HEAD"), cancellable = true)
    private void onGetStuckArrowCount(CallbackInfoReturnable<Integer> cir) {
        code.essence.features.impl.render.NoRender noRender = code.essence.features.impl.render.NoRender.getInstance();
        if (noRender != null && noRender.shouldHideStuckArrows()) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "getStingerCount", at = @At("HEAD"), cancellable = true)
    private void onGetStingerCount(CallbackInfoReturnable<Integer> cir) {
        code.essence.features.impl.render.NoRender noRender = code.essence.features.impl.render.NoRender.getInstance();
        if (noRender != null && noRender.shouldHideStuckArrows()) {
            cir.setReturnValue(0);
        }
    }
    @Shadow public float bodyYaw;
    @Shadow public abstract boolean isInSwimmingPose();
    @Unique private final MinecraftClient client = MinecraftClient.getInstance();

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    public void isPushable(CallbackInfoReturnable<Boolean> infoReturnable) {
        PushEvent event = new PushEvent(PushEvent.Type.COLLISION);
        EventManager.callEvent(event);
        if (event.isCancelled()) infoReturnable.setReturnValue(false);
    }

    @Redirect(method = "calcGlidingVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getPitch()F"))
    private float hookModifyFallFlyingPitch(LivingEntity instance) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return instance.getPitch();
        }
        var rotationManager = TurnsConnection.INSTANCE;
        var rotation = rotationManager.getRotation();
        var configurable = rotationManager.getCurrentRotationPlan();
        if (rotation == null || configurable == null || !configurable.isMoveCorrection() || configurable.isChangeLook()) {
            return instance.getPitch();
        }
        return rotation.getPitch();
    }

    @Redirect(method = "calcGlidingVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookModifyFallFlyingRotationVector(LivingEntity original) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return original.getRotationVector();
        }
        var rotationManager = TurnsConnection.INSTANCE;
        var rotation = rotationManager.getRotation();
        var configurable = rotationManager.getCurrentRotationPlan();
        if (rotation == null || configurable == null || !configurable.isMoveCorrection() || configurable.isChangeLook()) {
            return original.getRotationVector();
        }
        return rotation.toVector();
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void jump(CallbackInfo info) {
        if ((Object) this instanceof ClientPlayerEntity player) {
            JumpEvent event = new JumpEvent(player);
            EventManager.callEvent(event);
            if (event.isCancelled()) info.cancel();
        }
    }

    @ModifyExpressionValue(method = "jump", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookFixRotation(Vec3d original) {
        if (client.player == null || (Object) this != client.player) {
            return original;
        }
        float yaw = TurnsConnection.INSTANCE.getMoveRotation().getYaw() * 0.017453292F;
        return new Vec3d(-MathHelper.sin(yaw) * 0.2F, 0.0, MathHelper.cos(yaw) * 0.2F);
    }

    @ModifyExpressionValue(method = "calcGlidingVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getPitch()F"))
    private float hookModifyFallFlyingPitch(float original) {
        if (client.player == null || (Object) this != client.player) {
            return original;
        }
        return TurnsConnection.INSTANCE.getMoveRotation().getPitch();
    }

    @ModifyExpressionValue(method = "calcGlidingVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookModifyFallFlyingRotationVector(Vec3d original) {
        if (client.player == null || (Object) this != client.player) {
            return original;
        }
        return TurnsConnection.INSTANCE.getMoveRotation().toVector();
    }

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void swingProgressHook(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this != client.player) {
            return;
        }
        SwingDurationEvent event = new SwingDurationEvent();
        EventManager.callEvent(event);
        if (event.isCancelled()) {
            float animation = event.getAnimation();
            if (StatusEffectUtil.hasHaste(client.player)) animation *= (6 - (1 + StatusEffectUtil.getHasteAmplifier(client.player)));
            else animation *= (hasStatusEffect(StatusEffects.MINING_FATIGUE) ? 6 + (1 + getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) * 2 : 6);
            cir.setReturnValue((int) animation);
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        EntityDeathEvent event = new EntityDeathEvent(entity, source);
        EventManager.callEvent(event);
    }

    @Inject(method = "handleStatus", at = @At("HEAD"))
    private void handleStatus(byte status, CallbackInfo ci) {
        if (status == 3) {
            LivingEntity entity = (LivingEntity) (Object) this;
            EntityDeathEvent event = new EntityDeathEvent(entity, null);
            EventManager.callEvent(event);
        }
    }
}