package code.essence.features.impl.player;


import code.essence.events.player.InputEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.client.managers.event.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;

public class SafeWalk extends Module {

    public SafeWalk() {
        super("SafeWalk", ModuleCategory.PLAYER);
        setup();
    }

    @EventHandler
    public void onInput(InputEvent e) {
        if (mc.player == null || mc.world == null) return;
        BlockPos pos = mc.player.getBlockPos().down();
        boolean atEdge = mc.world.getBlockState(pos).getBlock() == Blocks.AIR && mc.player.isOnGround();

        if (atEdge) {
            e.setSneak(true);
        }
    }
}


