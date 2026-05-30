package code.essence.features.impl.misc;

import code.essence.events.packet.PacketEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.TextSetting;
import code.essence.utils.client.managers.event.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import static code.essence.utils.display.interfaces.QuickImports.mc;

public class AutoAuth extends Module {
    private final TextSetting password = new TextSetting("Пароль", "Пароль для автоматической регистрации/входа")
            .setText("bee1892");

    public AutoAuth() {
        super("AutoAuth", "AutoAuth", ModuleCategory.MISC);
        setup(password);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (!state || mc.player == null || mc.world == null) return;
        
        if (e.getPacket() instanceof GameMessageS2CPacket chatPacket) {
            String chatMessage = chatPacket.content().getString();
            String pass = password.getText();
            
            if (chatMessage.contains("Войдите") || chatMessage.contains("/login")) {
                if (mc.player.networkHandler != null) {
                    mc.player.networkHandler.sendChatCommand("login " + pass);
                }
            }
            
            if (chatMessage.contains("Зарегистрируйтесь") || chatMessage.contains("/reg")) {
                if (pass != null && pass.length() >= 4 && mc.player.networkHandler != null) {
                    mc.player.networkHandler.sendChatCommand("reg " + pass + " " + pass);
                }
            }
        }
    }
}
