package code.essence.features.impl.movement;

import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.Box;

import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.BooleanSetting;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.display.render.geometry.Render3D;
import code.essence.events.packet.PacketEvent;
import code.essence.events.render.WorldRenderEvent;
import code.essence.events.player.TickEvent;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Blink extends Module {
    private final BooleanSetting pulse = new BooleanSetting("Пульсация", "Периодически отправлять накопленные пакеты").setValue(false);
    private final SliderSettings pulseDelay = new SliderSettings("Задержка", "Интервал пульсации в мс")
            .setValue(500.0F)
            .range(50.0F, 3000.0F)
            .step(10.0F)
            .visible(pulse::isValue);

    private final List<Packet<?>> packets = new CopyOnWriteArrayList<>();
    private Box box;
    private Vec3d renderPos; 
    private long lastToggleTime;
    private long lastPulseTime;
    private boolean isFrozen = true;

    public Blink() {
        super("Blink", ModuleCategory.PLAYER);
        setup(pulse, pulseDelay);
    }

    @Override
    public void activate() {
        start();
    }

    @Override
    public void deactivate() {
        end();
    }

    public void start() {
        if (mc.player != null) {
            box = mc.player.getBoundingBox();
            renderPos = mc.player.getPos();
            lastPulseTime = System.currentTimeMillis();
        }
    }

    public void end() {
        packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
        packets.clear();
        box = null;
        renderPos = null;
        lastPulseTime = -1;
    }

    @EventHandler
    public void onTick(TickEvent e) {
        update(pulse.isValue(), pulseDelay.getValue());
    }

    public void update(boolean pulse, double pulseDelay) {
        if (PlayerInteractionHelper.nullCheck()) return;
        if (!pulse) return;

        long now = System.currentTimeMillis();
        if (now - lastPulseTime >= (long) pulseDelay) {
            packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
            packets.clear();

            if (mc.player != null) {
                box = mc.player.getBoundingBox();
                renderPos = mc.player.getPos();
            }
            lastPulseTime = now;
        }
    }
    
    @EventHandler
    public void onPacket(PacketEvent e) {
        if (PlayerInteractionHelper.nullCheck()) return;
        switch (e.getPacket()) {
            case PlayerRespawnS2CPacket respawn -> setState(false);
            case GameJoinS2CPacket join -> setState(false);
            case ClientStatusC2SPacket status when status.getMode().equals(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN) ->
                    setState(false);
            default -> {
                if (e.isSend()) {
                    packets.add(e.getPacket());
                    e.cancel();
                }
            }
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (renderPos != null && mc.player != null) {
            Box renderBox = mc.player.getBoundingBox().offset(renderPos.subtract(mc.player.getPos()));
            Render3D.drawBox(renderBox, ColorAssist.getClientColor(), 1);
        }
    }
}
