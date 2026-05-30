package code.essence.features.impl.misc;

import code.essence.events.packet.PacketEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.client.managers.event.EventHandler;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;

public class XCarry extends Module {
    public XCarry() {
        super("XCarry", ModuleCategory.MISC);
    }
    @EventHandler
    public void onS(PacketEvent e){
        if (e.getPacket() instanceof CloseHandledScreenC2SPacket zalupa2) {
            if (zalupa2.getSyncId() == 0) {
                e.cancel();
            }
        }
    }
}
