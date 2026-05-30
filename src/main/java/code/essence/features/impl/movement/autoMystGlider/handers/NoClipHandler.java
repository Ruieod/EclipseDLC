package code.essence.features.impl.movement.autoMystGlider.handers;

import code.essence.events.packet.PacketEvent;
import code.essence.events.player.MoveEvent;
import code.essence.features.impl.movement.Fly;
import code.essence.features.impl.movement.autoMystGlider.states.ConnectionState;
import code.essence.features.impl.movement.autoMystGlider.states.GlideState;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.Packet;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author nikitavodolaz
 * @since 04.02.2026
 */

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoClipHandler implements QuickImports {
    final List<Packet<?>> packets = new CopyOnWriteArrayList<>();
    final ConnectionState connectionState;
    final GlideState glideState;
    long lastPulseTime, fullyDoneTime;

    public NoClipHandler(GlideState glideState, ConnectionState connectionState) {
        this.glideState = glideState;
        this.connectionState = connectionState;
    }

    public void onPacket(PacketEvent packetEvent) {
        if (isDone()) {
            if (PlayerInteractionHelper.nullCheck()) return;

            if (packetEvent.isSend()) {
                packets.add(packetEvent.getPacket());
                packetEvent.cancel();
            }
        }
    }

    public void onTick() {
        if (mc.player == null) return;

        if (isDone()) {
            if (lastPulseTime == -1) {
                start();
            }

            Fly fly = Fly.getInstance();
            fly.onTick(null);

            update();
        } else {
            end();
        }
    }

    public void onMove(MoveEvent moveEvent) {
        if (mc.player == null) return;

        if (isDone()) {
            Fly fly = Fly.getInstance();
            fly.onMove(moveEvent);
        }
    }

    private boolean isDone() {
        if (glideState.fullyDone()) {
            if (fullyDoneTime == -1) {
                fullyDoneTime = System.currentTimeMillis();
            }
        } else {
            fullyDoneTime = -1;
        }

        return System.currentTimeMillis() - fullyDoneTime > 3000 && !connectionState.isReconnected();
    }

    public void start() {
        if (mc.player != null) {
            lastPulseTime = System.currentTimeMillis();
        }
    }

    public void end() {
        if (!packets.isEmpty()) {
            packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
            packets.clear();
        }

        lastPulseTime = -1;
    }

    public void update() {
        if (PlayerInteractionHelper.nullCheck()) return;

        long now = System.currentTimeMillis();
        if (now - lastPulseTime >= (long) 400) {
            packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
            packets.clear();
            lastPulseTime = now;
        }
    }
}
