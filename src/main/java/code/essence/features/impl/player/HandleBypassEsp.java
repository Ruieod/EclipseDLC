package code.essence.features.impl.player;

import code.essence.events.packet.PacketEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.client.managers.event.EventHandler;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.util.math.ChunkPos;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class HandleBypassEsp extends Module {

    private final Set<ChunkPos> haha = new HashSet<>();

    public HandleBypassEsp() {
        super("HandleBypassEsp", ModuleCategory.COMBAT);
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (mc.world == null || mc.player == null || event.isSend()) return;
        if (!(event.getPacket() instanceof ChunkDataS2CPacket packet)) return;

        ChunkPos chunkPos = new ChunkPos(packet.getChunkX(), packet.getChunkZ());
        if (!haha.add(chunkPos)) return;



        mc.execute(() -> {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof PlayerEntity player && player != mc.player) {
                    if (new ChunkPos(player.getBlockPos()).equals(chunkPos)) {
                        String name = player.getName().getString();

                    }
                }
            }
        });
    }

    @Override
    public void deactivate() {
        haha.clear();
        super.deactivate();
    }
}