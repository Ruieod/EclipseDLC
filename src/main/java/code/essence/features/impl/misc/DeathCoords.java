package code.essence.features.impl.misc;

import code.essence.events.packet.PacketEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.client.chat.ChatMessage;
import code.essence.utils.client.managers.event.EventHandler;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;

public class DeathCoords extends Module {
    public DeathCoords() {
        super("DeathCoords", ModuleCategory.MISC);
    }

    @EventHandler
    public void onPacket(PacketEvent e){
        if (e.getPacket() instanceof DeathMessageS2CPacket) {
            String text = String.format("%.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            ChatMessage.send( Text.literal(text).styled(style->style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text))));
        }
    }
}
