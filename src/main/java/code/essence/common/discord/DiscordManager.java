package code.essence.common.discord;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import code.essence.common.discord.utils.*;
import code.essence.utils.client.discord.Buffer;
import code.essence.Essence;
import java.io.IOException;

@Setter
@Getter
public class DiscordManager {
    private final DiscordDaemonThread discordDaemonThread = new DiscordDaemonThread();
    private boolean running = true;
    private DiscordInfo info = new DiscordInfo("Unknown", "", "");
    private Identifier avatarId;

    public void init() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            return;
        }

        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder()
                .ready((user) -> {
                    Essence.getInstance().getDiscordManager().setInfo(
                            new DiscordInfo(user.username,
                                    "https://cdn.discordapp.com/avatars/" + user.userId + "/" + user.avatar + ".png",
                                    user.userId));
                    String uid = Essence.getInstance().getNativeUserIdentifier();
                    String role = Essence.getInstance().getNativeUserRole();
                    String username = Essence.getInstance().getNativeUsername();
                    
                    DiscordRichPresence essencePresence = new DiscordRichPresence.Builder()
                            .setStartTimestamp(System.currentTimeMillis() / 1000)
                            .setDetails("Никнейм: " + username)
                            .setState("Роль: " + role + " (Юид: " + uid+")")
                            .setLargeImage("https://i.ibb.co/Df5c6FR/photo-2024-12-28-01-25-20.jpg", "https://t.me/qstarlab")
                            .setSmallImage(Essence.getInstance().getDiscordManager().getInfo().avatarUrl, "Я советую приобрести Rockstar :)")
                            .setButtons(RPCButton.create("Телеграм", "https://t.me/rockclient"),
                                    RPCButton.create("Дискорд", "https://discord.gg/DpwtHChbrm"))
                            .build();
                    DiscordRPC.INSTANCE.Discord_UpdatePresence(essencePresence);
                }).build();
        DiscordRPC.INSTANCE.Discord_Initialize("1425176750262063154", handlers, true, "");
        discordDaemonThread.start();
    }

    public void stopRPC() {
        DiscordRPC.INSTANCE.Discord_Shutdown();
        this.running = false;
    }

    public void load() throws IOException {
        if (avatarId == null && !info.avatarUrl.isEmpty()) {
            avatarId = Buffer.registerDynamicTexture("avatar-", Buffer.getHeadFromURL(info.avatarUrl));
        }
    }

    public Identifier getAvatarId() {
        return avatarId;
    }

    private class DiscordDaemonThread extends Thread {
        @Override
        public void run() {
            this.setName("Discord-RPC");
            try {
                MinecraftClient mc = MinecraftClient.getInstance();
                while (mc == null || mc.getTextureManager() == null) {
                    Thread.sleep(500);
                    mc = MinecraftClient.getInstance();
                }
                
                while (Essence.getInstance().getDiscordManager().isRunning()) {
                    DiscordRPC.INSTANCE.Discord_RunCallbacks();
                    load();
                    Thread.sleep(1500);
                }
            } catch (Exception exception) {
                stopRPC();
            }
            super.run();
        }
    }

    public record DiscordInfo(String userName, String avatarUrl, String userId) {}
}