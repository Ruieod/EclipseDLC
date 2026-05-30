package code.essence.features.impl.render.jumpCircle.handler;


import code.essence.features.impl.render.jumpCircle.data.JumpCircleLayer;
import code.essence.features.impl.render.jumpCircle.receiver.JumpCircleModule;
import code.essence.main.client.Api;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.theme.ThemeManager;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public record JumpCircleHandler(JumpCircleModule module) implements Api {
    public static Identifier IDENTIFIER = Identifier.of("textures/circle.png");

    public void provideRender(MatrixStack matrixStack, float delta) {
        BufferBuilder bufferBuilder = createBuilder();

        setupGlConst();
        setupTexture();

        ShaderProgram program = setupShader();

        for (JumpCircleLayer layer : module.getStorage().getJumpCircleLayers()) {
            setupVertices(bufferBuilder, layer, matrixStack);
        }

        closeBuffer(bufferBuilder);
        returnGlConst();
    }

    private void setupVertices(BufferBuilder builder, JumpCircleLayer layer, MatrixStack matrixStack) {
        float animValue = layer.animation().getOutput().floatValue();
        float sinTime = (float) (System.nanoTime() / 100_000_000_0D);
        int selectedColor;
        if (JumpCircleModule.colorMode.get().equals("Клиентский")) {
            selectedColor = ThemeManager.primaryColor.getColor();
        } else {
            selectedColor = JumpCircleModule.color.getColor();
        }

        Camera camera = mc.getEntityRenderDispatcher().camera;
        if (camera == null) {
            return;
        }
        int firstColor = ColorAssist.multAlpha(selectedColor, animValue);
        int secondColor = ColorAssist.multAlpha(ColorAssist.multSaturation(selectedColor, -0.3f), animValue);

        matrixStack.push();
        matrixStack.translate(layer.vec3d());
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(360 * sinTime));

        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();

        float size = module.getSize().getValue() / (module.getSize().getMax() - module.getSize().getMin()) * animValue;

        builder.vertex(matrix4f, -size, -size, 0).texture(0, 0).color(firstColor);
        builder.vertex(matrix4f, -size, size, 0).texture(0, 1).color(firstColor);
        builder.vertex(matrix4f, size, size, 0).texture(1, 1).color(secondColor);
        builder.vertex(matrix4f, size, -size, 0).texture(1, 0).color(secondColor);

        builder.vertex(matrix4f, -size, -size, 0).texture(0, 0).color(firstColor);
        builder.vertex(matrix4f, -size, size, 0).texture(0, 1).color(firstColor);
        builder.vertex(matrix4f, size, size, 0).texture(1, 1).color(secondColor);
        builder.vertex(matrix4f, size, -size, 0).texture(1, 0).color(secondColor);

        matrixStack.pop();
    }

    private BufferBuilder createBuilder() {
        return Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
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
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
    }

    private void returnGlConst() {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
    }

    private ShaderProgram setupShader() {
        return RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
    }

    private void setupTexture() {
        AbstractTexture texture = mc.getTextureManager().getTexture(IDENTIFIER);
        texture.setFilter(false, false);
        RenderSystem.setShaderTexture(0, texture.getGlId());
    }
}
