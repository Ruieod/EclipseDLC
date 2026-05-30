package code.essence.features.impl.combat;

import code.essence.events.player.AttackEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.utils.client.Instance;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.features.aura.warp.TurnsConnection;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.math.calc.Calculate;
import io.netty.util.internal.MathUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.MathHelper;


@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Criticals extends Module {
    public static Criticals getInstance() {
        return Instance.get(Criticals.class);
    }

    SelectSetting mode = new SelectSetting("Mode", "Select bypass mode").value("ReallyWorld");

    public Criticals() {
        super("Criticals", ModuleCategory.COMBAT);
        setup(mode);
    }


    @EventHandler
    public void onAttack(AttackEvent e) {

        if (!PlayerInteractionHelper.isPlayerInBlock(Blocks.COBWEB)) {
            return;
        }

        if (mc.player.isTouchingWater()) return;

        if (mode.isSelected("ReallyWorld")) {
            if (!mc.player.isOnGround() && mc.player.fallDistance == 0) {
                PlayerInteractionHelper.grimSuperBypass$$$(-(mc.player.fallDistance = Calculate.getRandom(1e-5F, 1e-4F)), TurnsConnection.INSTANCE.getRotation().random(1e-3F));
            }
        }
    }
}