package code.essence.features.impl.misc;

import code.essence.events.packet.PacketEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.client.Instance;
import code.essence.utils.client.managers.event.EventHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;

public class SoundsRemove extends Module {
    public static SoundsRemove getInstance() {
        return Instance.get(SoundsRemove.class);
    }

    public SoundsRemove() {
        super("SoundsRemove", "SoundsRemove", ModuleCategory.MISC);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (!isState() || e.isSend()) return;
        if (!(e.getPacket() instanceof PlaySoundS2CPacket packet)) return;

        String soundId = packet.getSound().getIdAsString();
        if (soundId != null && soundId.contains("entity.wither")) {
            e.setCancelled(true);
        }
    }
}
