package code.essence.features.impl.misc.creeperFarm;

import code.essence.events.packet.PacketEvent;
import code.essence.utils.display.interfaces.QuickImports;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;

/**
 * @author nikitavodolaz
 * @since 07.02.2026
 */

public class CreeperStorage implements QuickImports {
    Deque<Entity> entityDeque = new ArrayDeque<>();

    public void onTick() {
        entityDeque.removeIf(entity -> !entity.isAlive() || entity.isRemoved());
    }

    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.getPacket() instanceof EntitySpawnS2CPacket packet) {
            Entity entity = mc.world.getEntityById(packet.getEntityId());

            if (entity != null && entity.isAlive() && !entity.isRemoved()) {
                if (!entityDeque.contains(entity)) {
                    entityDeque.add(entity);
                }
            }
        }
    }

    public @Nullable Entity getTarget() {
        return !entityDeque.isEmpty()
                ? getClosestEntity()
                : null;
    }

    private @Nullable Entity getClosestEntity() {
        return entityDeque.stream()
                .min(Comparator.comparingDouble(entity -> mc.player.distanceTo(entity)))
                .orElse(null);
    }
}
