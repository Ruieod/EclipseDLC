package code.essence.features.impl.player;

import code.essence.features.impl.combat.Aura;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.client.Instance;
import net.minecraft.client.Keyboard;
import net.minecraft.world.GameRules;

public class SaturationViewer extends Module {

    public static SaturationViewer getInstance() {
        return Instance.get(SaturationViewer.class);
    }

    public SaturationViewer() {
        super("SaturationViewer", ModuleCategory.PLAYER);
    }
}
