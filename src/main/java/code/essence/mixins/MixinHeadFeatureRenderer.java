package code.essence.mixins;

import code.essence.Essence;
import code.essence.features.impl.render.ChinaHat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;



@Mixin(HeadFeatureRenderer.class)
public abstract class MixinHeadFeatureRenderer<S extends LivingEntityRenderState, M extends EntityModel<S> & ModelWithHead> extends FeatureRenderer<S, M> {

    public MixinHeadFeatureRenderer(FeatureRendererContext<S, M> context) {
        super(context);
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/LivingEntityRenderState;FF)V", at = @At("HEAD"))
    public void onRenderHead(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, S livingEntityRenderState, float f, float g, CallbackInfo ci) {
        ChinaHat chinaHat = Essence.getInstance().getModuleProvider().get(ChinaHat.class);
        if (chinaHat != null && chinaHat.isState() && livingEntityRenderState instanceof PlayerEntityRenderState) {
            Entity entity = MinecraftClient.getInstance().world.getEntityById(((PlayerEntityRenderState) livingEntityRenderState).id);
            if (entity instanceof PlayerEntity player) {
                chinaHat.render(matrixStack, vertexConsumerProvider, player, this.getContextModel());
            }
        }
    }
}