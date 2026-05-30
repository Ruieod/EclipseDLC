package code.essence.mixins;

import code.essence.wavecapes.*;
import code.essence.wavecapes.math.Vector3;
import code.essence.wavecapes.sim.StickSimulation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeFeatureRenderer.class)
public abstract class CapeFeatureRendererMixin extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    @Unique
    private static final int PART_COUNT = 16;

    public CapeFeatureRendererMixin(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    public void onRender(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        
        Entity entity = mc.world.getEntityById(state.id);
        if (!(entity instanceof AbstractClientPlayerEntity player)) return;

       ItemStack chestplate = player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST);
        if (chestplate != null && chestplate.isOf(net.minecraft.item.Items.ELYTRA)) {
            return; 
        }

        
        boolean isLocalPlayer = player == mc.player;
        boolean isFriend = !isLocalPlayer && code.essence.common.repository.friend.FriendUtils.isFriend(player);
        
        if (!isLocalPlayer && !isFriend) {
            return; 
        }
        
        CapeHolder holder = (CapeHolder) player;
        float delta = mc.getRenderTickCounter().getTickDelta(true);
        
        
        
        renderSmoothCape(matrixStack, vertexConsumerProvider, player, state, delta, light);
        ci.cancel();
    }

    @Unique
    private void renderSmoothCape(MatrixStack poseStack, VertexConsumerProvider multiBufferSource, 
                                   AbstractClientPlayerEntity player, PlayerEntityRenderState state, 
                                   float delta, int light) {
        
        net.minecraft.util.Identifier capeTextureId = net.minecraft.util.Identifier.of(WaveyCapes.DEFAULT_CAPE_TEXTURE);
        
        
        VertexConsumer bufferBuilder = multiBufferSource.getBuffer(RenderLayer.getEntityCutout(capeTextureId));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f oldPositionMatrix = null;
        for (int part = 0; part < PART_COUNT; part++) {
            modifyPoseStack(poseStack, player, delta, part);

            if (oldPositionMatrix == null) {
                oldPositionMatrix = new Matrix4f(poseStack.peek().getPositionMatrix());
            }

            if (part == 0) {
                addTopVertex(bufferBuilder, poseStack.peek().getPositionMatrix(), oldPositionMatrix,
                        0.3F, 0, 0F, -0.3F, 0, -0.06F, part, light);
            }

            if (part == PART_COUNT - 1) {
                addBottomVertex(bufferBuilder, poseStack.peek().getPositionMatrix(), poseStack.peek().getPositionMatrix(),
                        0.3F, (part + 1) * (0.96F / PART_COUNT), 0F,
                        -0.3F, (part + 1) * (0.96F / PART_COUNT), -0.06F, part, light);
            }

            addLeftVertex(bufferBuilder, poseStack.peek().getPositionMatrix(), oldPositionMatrix,
                    -0.3F, (part + 1) * (0.96F / PART_COUNT), 0F,
                    -0.3F, part * (0.96F / PART_COUNT), -0.06F, part, light);

            addRightVertex(bufferBuilder, poseStack.peek().getPositionMatrix(), oldPositionMatrix,
                    0.3F, (part + 1) * (0.96F / PART_COUNT), 0F,
                    0.3F, part * (0.96F / PART_COUNT), -0.06F, part, light);

            addBackVertex(bufferBuilder, poseStack.peek().getPositionMatrix(), oldPositionMatrix,
                    0.3F, (part + 1) * (0.96F / PART_COUNT), -0.06F,
                    -0.3F, part * (0.96F / PART_COUNT), -0.06F, part, light);

            addFrontVertex(bufferBuilder, oldPositionMatrix, poseStack.peek().getPositionMatrix(),
                    0.3F, (part + 1) * (0.96F / PART_COUNT), 0F,
                    -0.3F, part * (0.96F / PART_COUNT), 0F, part, light);

            oldPositionMatrix = new Matrix4f(poseStack.peek().getPositionMatrix());
            poseStack.pop();
        }
    }

    @Unique
    private void modifyPoseStack(MatrixStack poseStack, AbstractClientPlayerEntity player, float h, int part) {
        if (WaveyCapes.capeMovement == CapeMovement.BASIC_SIMULATION) {
            modifyPoseStackSimulation(poseStack, player, h, part);
            return;
        }
        modifyPoseStackVanilla(poseStack, player, h, part);
    }

    @Unique
    private void modifyPoseStackSimulation(MatrixStack poseStack, AbstractClientPlayerEntity player, float delta, int part) {
        final StickSimulation simulation = ((CapeHolder) player).essence$getSimulation();
        
        
        if (simulation == null || simulation.getPoints().isEmpty() || simulation.getPoints().size() <= part) {
            
            if (simulation != null && simulation.getPoints().isEmpty()) {
                simulation.init(PART_COUNT);
            }
            
            if (simulation == null || simulation.getPoints().isEmpty() || simulation.getPoints().size() <= part) {
                modifyPoseStackVanilla(poseStack, player, delta, part);
                return;
            }
        }
        
        poseStack.push();

        double z1 = player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).isEmpty() ? 0.125 : 0.15;
        poseStack.translate(0.0, 0.0, z1);

        StickSimulation.Point capePoint = simulation.getPoints().get(0);
        float x = simulation.getPoints().get(part).getLerpX(delta) - capePoint.getLerpX(delta);
        if (x > 0.0f) {
            x = 0.0f;
        }
        final float y = capePoint.getLerpY(delta) - part - simulation.getPoints().get(part).getLerpY(delta);
        final float z = capePoint.getLerpZ(delta) - simulation.getPoints().get(part).getLerpZ(delta);
        final float sidewaysRotationOffset = 0.0f;
        final float partRotation = getRotation(delta, part, simulation);
        float height = 0.0f;
        if (player.isSneaking()) {
            height += 25.0f;
            poseStack.translate(0.0, 0.15000000596046448, 0.0);
        }
        final float naturalWindSwing = getNatrualWindSwing(part, player.isSubmergedInWater());
        poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6.0f + height + naturalWindSwing));
        poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sidewaysRotationOffset / 2.0f));
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - sidewaysRotationOffset / 2.0f));
        poseStack.translate(-z / PART_COUNT, y / PART_COUNT, x / PART_COUNT);
        poseStack.translate(0.0, 0.03, -0.03);
        poseStack.translate(0.0, part * 1.0f / PART_COUNT, 0);
        poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-partRotation));
        poseStack.translate(0.0, -part * 1.0f / PART_COUNT, 0);
        poseStack.translate(0.0, -0.03, 0.03);
    }

    @Unique
    private float getRotation(float delta, int part, StickSimulation simulation) {
        if (simulation == null || simulation.points == null || simulation.points.isEmpty()) {
            return 0.0f;
        }
        if (part == PART_COUNT - 1) {
            return getRotation(delta, part - 1, simulation);
        }
        if (part < 0 || part + 1 >= simulation.points.size()) {
            return 0.0f;
        }
        return (float) getAngle(simulation.points.get(part).getLerpedPos(delta), simulation.points.get(part + 1).getLerpedPos(delta));
    }

    @Unique
    private double getAngle(Vector3 a, Vector3 b) {
        Vector3 angle = b.clone().subtract(a);
        return Math.toDegrees(Math.atan2(angle.x, angle.y)) + 180.0;
    }

    @Unique
    private void modifyPoseStackVanilla(MatrixStack poseStack, AbstractClientPlayerEntity player, float h, int part) {
        poseStack.push();
        poseStack.translate(0.0, 0.0, 0.125);
        
        double d = MathHelper.lerp(h, player.prevCapeX, player.capeX) - MathHelper.lerp(h, player.prevX, player.getX());
        double e = MathHelper.lerp(h, player.prevCapeY, player.capeY) - MathHelper.lerp(h, player.prevY, player.getY());
        double m = MathHelper.lerp(h, player.prevCapeZ, player.capeZ) - MathHelper.lerp(h, player.prevZ, player.getZ());
        
        float n = player.prevBodyYaw + (player.bodyYaw - player.prevBodyYaw);
        double o = MathHelper.sin(n * 0.017453292f);
        double p = -MathHelper.cos(n * 0.017453292f);
        
        float height = (float) e * 10.0f;
        height = MathHelper.clamp(height, -6.0f, 32.0f);
        
        float swing = (float) (d * o + m * p) * easeOutSine(1.0f / PART_COUNT * part) * 100.0f;
        swing = MathHelper.clamp(swing, 0.0f, 150.0f * easeOutSine(1.0f / PART_COUNT * part));
        
        float sidewaysRotationOffset = (float) (d * p - m * o) * 100.0f;
        sidewaysRotationOffset = MathHelper.clamp(sidewaysRotationOffset, -20.0f, 20.0f);
        
        
        double horizontalSpeed = player.getVelocity().horizontalLength();
        float walkAnimation = (float) Math.min(horizontalSpeed * 4.0, 1.0);
        height += MathHelper.sin((float) (System.currentTimeMillis() / 100.0) * 6.0f) * 32.0f * walkAnimation;
        
        if (player.isSneaking()) {
            height += 25.0f;
            poseStack.translate(0.0, 0.15000000596046448, 0.0);
        }
        
        final float naturalWindSwing = getNatrualWindSwing(part, player.isSubmergedInWater());
        poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6.0f + swing / 2.0f + height + naturalWindSwing));
        poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sidewaysRotationOffset / 2.0f));
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - sidewaysRotationOffset / 2.0f));
    }

    @Unique
    private float getNatrualWindSwing(int part, boolean underwater) {
        long highlightedPart = System.currentTimeMillis() / (underwater ? 9 : 3) % 360L;
        float relativePart = (part + 1) / (float) PART_COUNT;
        if (WaveyCapes.windMode == WindMode.WAVES) {
            return (float) (Math.sin(Math.toRadians(relativePart * 360.0f - highlightedPart)) * 3.0);
        }
        return 0.0f;
    }

    @Unique
    private static void addBackVertex(VertexConsumer bufferBuilder, Matrix4f matrix, Matrix4f oldMatrix, float x1, float y1, float z1, float x2, float y2, float z2, int part, int light) {
        float i;
        Matrix4f k;
        if (x1 < x2) {
            i = x1;
            x1 = x2;
            x2 = i;
        }

        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;

            k = matrix;
            matrix = oldMatrix;
            oldMatrix = k;
        }

        float minU = .015625F;
        float maxU = .171875F;
        float minV = .03125F;
        float maxV = .53125F;

        float deltaV = maxV - minV;
        float vPerPart = deltaV / PART_COUNT;
        maxV = minV + (vPerPart * (part + 1));
        minV = minV + (vPerPart * part);

        bufferBuilder.vertex(oldMatrix, x1, y2, z1).color(1f, 1f, 1f, 1f).texture(maxU, minV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
        bufferBuilder.vertex(oldMatrix, x2, y2, z1).color(1f, 1f, 1f, 1f).texture(minU, minV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
        bufferBuilder.vertex(matrix, x2, y1, z2).color(1f, 1f, 1f, 1f).texture(minU, maxV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
        bufferBuilder.vertex(matrix, x1, y1, z2).color(1f, 1f, 1f, 1f).texture(maxU, maxV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
    }

    @Unique
    private static void addFrontVertex(VertexConsumer bufferBuilder, Matrix4f matrix, Matrix4f oldMatrix, float x1, float y1, float z1, float x2, float y2, float z2, int part, int light) {
        float i;
        Matrix4f k;
        if (x1 < x2) {
            i = x1;
            x1 = x2;
            x2 = i;
        }

        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;

            k = matrix;
            matrix = oldMatrix;
            oldMatrix = k;
        }

        float minU = .1875F;
        float maxU = .34375F;
        float minV = .03125F;
        float maxV = .53125F;

        float deltaV = maxV - minV;
        float vPerPart = deltaV / PART_COUNT;
        maxV = minV + (vPerPart * (part + 1));
        minV = minV + (vPerPart * part);

        bufferBuilder.vertex(oldMatrix, x1, y1, z1).color(1f, 1f, 1f, 1f).texture(maxU, maxV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
        bufferBuilder.vertex(oldMatrix, x2, y1, z1).color(1f, 1f, 1f, 1f).texture(minU, maxV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
        bufferBuilder.vertex(matrix, x2, y2, z2).color(1f, 1f, 1f, 1f).texture(minU, minV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
        bufferBuilder.vertex(matrix, x1, y2, z2).color(1f, 1f, 1f, 1f).texture(maxU, minV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
    }

    @Unique
    private static void addLeftVertex(VertexConsumer bufferBuilder, Matrix4f matrix, Matrix4f oldMatrix, float x1, float y1, float z1, float x2, float y2, float z2, int part, int light) {
        float i;
        if (x1 < x2) {
            i = x1;
            x1 = x2;
            x2 = i;
        }

        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;
        }

        float minU = 0;
        float maxU = .015625F;
        float minV = .03125F;
        float maxV = .53125F;

        float deltaV = maxV - minV;
        float vPerPart = deltaV / PART_COUNT;
        maxV = minV + (vPerPart * (part + 1));
        minV = minV + (vPerPart * part);

        bufferBuilder.vertex(matrix, x2, y1, z1).color(1f, 1f, 1f, 1f).texture(maxU, maxV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
        bufferBuilder.vertex(matrix, x2, y1, z2).color(1f, 1f, 1f, 1f).texture(minU, maxV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
        bufferBuilder.vertex(oldMatrix, x2, y2, z2).color(1f, 1f, 1f, 1f).texture(minU, minV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
        bufferBuilder.vertex(oldMatrix, x2, y2, z1).color(1f, 1f, 1f, 1f).texture(maxU, minV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
    }

    @Unique
    private static void addRightVertex(VertexConsumer bufferBuilder, Matrix4f matrix, Matrix4f oldMatrix, float x1, float y1, float z1, float x2, float y2, float z2, int part, int light) {
        float i;
        if (x1 < x2) {
            i = x1;
            x1 = x2;
            x2 = i;
        }

        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;
        }

        float minU = .171875F;
        float maxU = .1875F;
        float minV = .03125F;
        float maxV = .53125F;

        float deltaV = maxV - minV;
        float vPerPart = deltaV / PART_COUNT;
        maxV = minV + (vPerPart * (part + 1));
        minV = minV + (vPerPart * part);

        bufferBuilder.vertex(matrix, x2, y1, z2).color(1f, 1f, 1f, 1f).texture(minU, maxV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
        bufferBuilder.vertex(matrix, x2, y1, z1).color(1f, 1f, 1f, 1f).texture(maxU, maxV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
        bufferBuilder.vertex(oldMatrix, x2, y2, z1).color(1f, 1f, 1f, 1f).texture(maxU, minV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
        bufferBuilder.vertex(oldMatrix, x2, y2, z2).color(1f, 1f, 1f, 1f).texture(minU, minV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
    }

    @Unique
    private static void addBottomVertex(VertexConsumer bufferBuilder, Matrix4f matrix, Matrix4f oldMatrix, float x1, float y1, float z1, float x2, float y2, float z2, int part, int light) {
        float i;
        if (x1 < x2) {
            i = x1;
            x1 = x2;
            x2 = i;
        }

        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;
        }

        float minU = .171875F;
        float maxU = .328125F;
        float minV = 0;
        float maxV = .03125F;

        float deltaV = maxV - minV;
        float vPerPart = deltaV / PART_COUNT;
        maxV = minV + (vPerPart * (part + 1));
        minV = minV + (vPerPart * part);

        bufferBuilder.vertex(oldMatrix, x1, y2, z2).color(1f, 1f, 1f, 1f).texture(maxU, minV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
        bufferBuilder.vertex(oldMatrix, x2, y2, z2).color(1f, 1f, 1f, 1f).texture(minU, minV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
        bufferBuilder.vertex(matrix, x2, y1, z1).color(1f, 1f, 1f, 1f).texture(minU, maxV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
        bufferBuilder.vertex(matrix, x1, y1, z1).color(1f, 1f, 1f, 1f).texture(maxU, maxV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(1, 0, 0);
    }

    @Unique
    private static void addTopVertex(VertexConsumer bufferBuilder, Matrix4f matrix, Matrix4f oldMatrix, float x1, float y1, float z1, float x2, float y2, float z2, int part, int light) {
        float i;
        if (x1 < x2) {
            i = x1;
            x1 = x2;
            x2 = i;
        }

        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;
        }

        float minU = .015625F;
        float maxU = .171875F;
        float minV = 0;
        float maxV = .03125F;

        float deltaV = maxV - minV;
        float vPerPart = deltaV / PART_COUNT;
        maxV = minV + (vPerPart * (part + 1));
        minV = minV + (vPerPart * part);

        bufferBuilder.vertex(oldMatrix, x1, y2, z1).color(1f, 1f, 1f, 1f).texture(maxU, maxV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 1, 0);
        bufferBuilder.vertex(oldMatrix, x2, y2, z1).color(1f, 1f, 1f, 1f).texture(minU, maxV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 1, 0);
        bufferBuilder.vertex(matrix, x2, y1, z2).color(1f, 1f, 1f, 1f).texture(minU, minV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 1, 0);
        bufferBuilder.vertex(matrix, x1, y1, z2).color(1f, 1f, 1f, 1f).texture(maxU, minV).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 1, 0);
    }

    @Unique
    private static float easeOutSine(float x) {
        return (float) Math.sin((x * Math.PI) / 2f);
    }
}
