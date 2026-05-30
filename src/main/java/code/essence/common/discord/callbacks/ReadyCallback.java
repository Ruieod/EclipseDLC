package code.essence.common.discord.callbacks;

import com.sun.jna.Callback;
import code.essence.common.discord.utils.DiscordUser;

public interface ReadyCallback extends Callback {
    void apply(DiscordUser var1);
}