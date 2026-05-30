package code.essence.utils.client.chat;

import code.essence.mixins.acc.InsertChatMixin;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import code.essence.utils.client.text.TextHelper;

import static code.essence.utils.display.interfaces.QuickImports.mc;

public class ChatMessage {
    public static MutableText brandmessage() {
        MutableText essence = (MutableText) TextHelper.applyPredefinedGradient("Essence", "blue", true);
        MutableText arrow = Text.literal(" > ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY));
        return essence.append(arrow);
    }

    public static void send(Text message) {
        if (mc.inGameHud == null || mc.inGameHud.getChatHud() == null) return;

        Text prefix = TextHelper.applyPredefinedGradient("Essence", "blue", true);
        MutableText arrow = Text.literal(" >").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY));

        MutableText fullMessage = prefix.copy()
                .append(arrow)
                .append(" ")
                .append(message);

        int tick = mc.inGameHud.getTicks();
        ChatHudLine line = new ChatHudLine(tick, fullMessage, null, MessageIndicator.system());

        ChatHud chatHud = mc.inGameHud.getChatHud();
        ((InsertChatMixin) chatHud).invokeAddMessage(line);
        ((InsertChatMixin) chatHud).invokeAddVisibleMessage(line);
    }

    

    public static void brandmessage(String message) {
        if (MinecraftClient.getInstance().player != null) {
            MutableText essence = (MutableText) TextHelper.applyPredefinedGradient("Essence", "blue", true);
            MutableText arrow = Text.literal(" > ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY));
            MutableText formattedMessage = essence.append(arrow).append(Text.literal(message));
            MinecraftClient.getInstance().player.sendMessage(formattedMessage, false);
        }
    }

    public static void brandmessage(Text message) {
        if (MinecraftClient.getInstance().player != null) {
            MutableText essence = (MutableText) TextHelper.applyPredefinedGradient("Essence", "blue", true);
            MutableText arrow = Text.literal(" > ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY));
            MutableText formattedMessage = essence.append(arrow).append(message);
            MinecraftClient.getInstance().player.sendMessage(formattedMessage, false);
        }
    }
}

    