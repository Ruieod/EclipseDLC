package code.essence.features.impl.combat;


import code.essence.events.player.AttackEvent;
import code.essence.events.player.TickEvent;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.MinecraftClient;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShiftTap extends Module {

    @NonFinal
    long shiftTapEndTime = 0;
    @NonFinal
    boolean isModuleControllingSneak = false;
    @NonFinal
    int shiftTapDuration = 100;

    MinecraftClient mc = MinecraftClient.getInstance();

    public ShiftTap() {
        super("ShiftTap", "ShiftTap", ModuleCategory.COMBAT);
    }

    private void startShiftTap() {
        shiftTapEndTime = System.currentTimeMillis() + shiftTapDuration;
        if (!isModuleControllingSneak) {
            mc.options.sneakKey.setPressed(true);
            isModuleControllingSneak = true;
        }
    }
    private void stopShiftTap() {
        if (isModuleControllingSneak) {
            mc.options.sneakKey.setPressed(false);
            isModuleControllingSneak = false;
        }
    }

    @EventHandler
    public void onAttack(AttackEvent event) {
        if ( mc.player == null) {
            return;
        }
        startShiftTap();
    }

    @EventHandler
    
    public void onTick(TickEvent event) {
        if ( mc.player == null || mc.player.isSpectator()) {
            stopShiftTap();
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (isModuleControllingSneak && currentTime > shiftTapEndTime) {
            stopShiftTap();
        }
    }

}