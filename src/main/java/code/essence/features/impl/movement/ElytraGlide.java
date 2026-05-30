package code.essence.features.impl.movement;


import code.essence.events.player.TickEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.math.time.TimerUtil;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.Vec3d;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ElytraGlide extends Module {
    TimerUtil ticks = new TimerUtil();

    public ElytraGlide() {
        super("ElytraGlide", "ElytraGlide", ModuleCategory.MOVEMENT);
    }

    @EventHandler


    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.isGliding()) return;

        Vec3d pos = mc.player.getPos();
        float yaw = mc.player.getYaw();
        double forward = 0.087;
        double motion = calculateBPS();
        float valuePidor = 52;

        if (motion >= valuePidor) {
            forward = 0f;
        }

        double dx = -Math.sin(Math.toRadians(yaw)) * forward;
        double dz = Math.cos(Math.toRadians(yaw)) * forward;

        mc.player.setVelocity(
                dx * Calculate.getRandom(1.1f, 1.21f),
                mc.player.getVelocity().y - 0.02f,
                dz * Calculate.getRandom(1.1f, 1.21f)
        );

        if (ticks.hasTimeElapsed(50)) {
            mc.player.setPosition(
                    pos.getX() + dx,
                    pos.getY(),
                    pos.getZ() + dz
            );
            ticks.resetCounter();
        }

        mc.player.setVelocity(
                dx * Calculate.getRandom(1.1f, 1.21f),
                mc.player.getVelocity().y + 0.016f,
                dz * Calculate.getRandom(1.1f, 1.21f)
        );
    }

    private double calculateBPS() {
        if (mc.player == null) return 0;
        Vec3d velocity = mc.player.getVelocity();
        return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) * 20;
    }
}