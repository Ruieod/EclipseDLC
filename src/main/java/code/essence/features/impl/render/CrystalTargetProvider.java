package code.essence.features.impl.render;


import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.display.render.geometry.Render3D;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.features.aura.warp.Turns;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4i;

/**
 * @author nikitavodolaz
 * @since 30.01.2026
 */

public record CrystalTargetProvider(TargetESP targetEspModule) implements TargetRenderProvider {
    private static final Vector3f[] VERTICES = new Vector3f[]{new Vector3f(0.0f, 1.5f, 0.0f), new Vector3f(0.0f, -1.5f, 0.0f), new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f(-1.0f, 0.0f, 0.0f), new Vector3f(0.0f, 0.0f, 1.0f), new Vector3f(0.0f, 0.0f, -1.0f)};
    private static final int[][] FACES = new int[][]{{0, 2, 4}, {0, 4, 3}, {0, 3, 5}, {0, 5, 2}, {1, 4, 2}, {1, 3, 4}, {1, 5, 3}, {1, 2, 5}};

    @Override
    public void render(MatrixStack matrixStack, LivingEntity living, Vec3d vec3d, float delta) {
        setupGlConst();
        ShaderProgram program = setupShader();
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();

        BufferBuilder builder = createBuilder();

        double time = System.nanoTime() / 100_000_000_0D;
        double lengthX = living.getBoundingBox().getLengthX();
        double lengthZ = living.getBoundingBox().getLengthZ();

        float animValue = targetEspModule.getCrystal2FadeAnim().getOutput().floatValue();

        for (int i = 0; i < 32; i++) {
//            double cos = Math.cos(time + i) * (lengthX + 1f - 0.8f * animValue);
//            double sin = Math.sin(time + i) * (lengthZ + 1f - 0.8f * animValue);

            double cos = Math.cos(time + i) * (lengthX + 0.2f);
            double sin = Math.sin(time + i) * (lengthZ + 0.2f);

            Vec3d crystalPos = vec3d.add(cos, (i / 8f) % living.getHeight(), sin);
            Vec3d locationDelta = new Vec3d(vec3d.x - crystalPos.x, 0f, vec3d.z - crystalPos.z);
            Turns rotation = MathAngle.fromVec3d(locationDelta);

            Vec3d targetPos = crystalPos.subtract(rotation.toVector().multiply((1.0f - animValue)));

            matrixStack.push();
            matrixStack.translate(targetPos);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rotation.getYaw()));
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            matrixStack.translate(targetPos.negate());
            drawCrystal(matrixStack, builder, targetPos, 0.1f);

            matrixStack.pop();

            MatrixStack matrices = new MatrixStack();

            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
            matrices.translate(targetPos.x - camera.getPos().getX(), targetPos.y - camera.getPos().getY(), targetPos.z - camera.getPos().getZ());
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            float glowSize = 0.2f;
            Render3D.drawTexture(matrices.peek(), Particles.ParticleType.shariki.texture(), -glowSize * 2, -glowSize * 2, glowSize * 4, glowSize * 4, new Vector4i(ColorAssist.multAlpha(ColorAssist.getClientColor(), 0.2f * animValue)), true);
            matrices.pop();
        }

        closeBuffer(builder);
        returnGlConst();
    }

    private void drawCrystal(MatrixStack matrixStack, BufferBuilder builder, Vec3d vec3d, float size) {
        matrixStack.push();
        matrixStack.translate(vec3d);
        matrixStack.scale(size, size, size);
        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();

        for (int[] face : FACES) {
            Vector3f v1 = VERTICES[face[0]];
            Vector3f v2 = VERTICES[face[1]];
            Vector3f v3 = VERTICES[face[2]];

            int color = targetEspModule.getCrystalFill().isValue() ? ColorAssist.clampSaturation(ColorAssist.getClientColor(), 1f, 1.1f) : -1;
            color = ColorAssist.multAlpha(color, targetEspModule.getCrystal2FadeAnim().getOutput().floatValue());

            builder.vertex(matrix4f, v1.x, v1.y, v1.z).color(color);
            builder.vertex(matrix4f, v2.x, v2.y, v2.z).color(color);
            builder.vertex(matrix4f, v3.x, v3.y, v3.z).color(color);
        }

        matrixStack.pop();
    }

    private BufferBuilder createBuilder() {
        return Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
    }

    private void closeBuffer(BufferBuilder bufferBuilder) {
        BuiltBuffer builtBuffer = bufferBuilder.endNullable();

        if (builtBuffer != null && builtBuffer.getDrawParameters().vertexCount() > 0) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
        }
    }

    private void setupGlConst() {
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }

    private void returnGlConst() {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
    }

    private ShaderProgram setupShader() {
        return RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
    }
}
