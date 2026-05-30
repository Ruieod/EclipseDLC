package code.essence.features.impl.render;

import code.essence.features.module.setting.implement.ColorSetting;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.client.Instance;
import code.essence.utils.display.render.geometry.Render3D;
import code.essence.events.render.WorldRenderEvent;

import java.awt.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlockOverlay extends Module {
    public static BlockOverlay getInstance() {
        return Instance.get(BlockOverlay.class);
    }

    static ColorSetting color = new ColorSetting("Цвет", "Цвет кристаллов").value(new Color(255, 255, 255, 55).getRGB())
            .presets(new Color(0, 246, 255,255).getRGB(),
                    new Color(183, 1, 195,255).getRGB()
                    ,new Color(255, 60, 0,255).getRGB()
                    ,new Color(171, 253, 0,255).getRGB());

    public BlockOverlay() {
        super("BlockOverlay", "BlockOverlay", ModuleCategory.RENDER);
        setup(color);
    }




    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.crosshairTarget instanceof BlockHitResult result && result.getType().equals(HitResult.Type.BLOCK)) {
            BlockPos pos = result.getBlockPos();
            Render3D.drawShapeAlternative(pos, mc.world.getBlockState(pos).getOutlineShape(mc.world, pos), color.getColor(), 2, true, true);
        }
    }
}
