package code.essence.mixins;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import code.essence.features.impl.render.NoRender;
import code.essence.utils.display.interfaces.QuickImports;

@Mixin(BlockRenderManager.class)
public class BlockRenderMixin implements QuickImports {

    
    @Inject(method = "renderBlock(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;)V", 
            at = @At("HEAD"), cancellable = true, require = 0)
    private void essence$hideVegetation(BlockState state,
                                        BlockPos pos,
                                        BlockRenderView world,
                                        MatrixStack matrices,
                                        VertexConsumer vertexConsumer,
                                        boolean cull,
                                        Random random,
                                        CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.shouldHideVegetation() && isVegetationBlock(state)) {
            ci.cancel();
        }
    }

    
    private boolean isVegetationBlock(BlockState blockState) {
        if (blockState == null) return false;

        try {
            
            java.lang.reflect.Method getMaterial = blockState.getClass().getMethod("getMaterial");
            Object material = getMaterial.invoke(blockState);
            if (material != null) {
                String materialName = material.toString().toUpperCase();
                if (materialName.contains("PLANTS") || materialName.contains("TALL_PLANTS")
                        || materialName.contains("OCEAN_PLANT") || materialName.contains("NETHER_PLANTS")
                        || materialName.contains("SEA_GRASS") || materialName.contains("REPLACEABLE_PLANT")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            
        }

        
        try {
            String blockName = Registries.BLOCK.getId(blockState.getBlock()).toString().toLowerCase();
            if (blockName.contains("grass") || blockName.contains("flower") || blockName.contains("plant")
                    || blockName.contains("vine") || blockName.contains("sea_grass") || blockName.contains("kelp")
                    || blockName.contains("fern") || blockName.contains("bush") || blockName.contains("sapling")
                    || blockName.contains("mushroom") || blockName.contains("coral") || blockName.contains("lily")
                    || blockName.contains("sugar_cane") || blockName.contains("bamboo") || blockName.contains("cactus")
                    || blockName.contains("nether_wart") || blockName.contains("chorus") || blockName.contains("weeping_vines")
                    || blockName.contains("twisting_vines") || blockName.contains("cave_vines")) {
                return true;
            }
        } catch (Exception ignored) {
            
        }

        return false;
    }
}
