package code.essence.commands.defaults;

import code.essence.utils.client.chat.ChatMessage;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.Formatting;

import code.essence.utils.client.managers.api.command.Command;
import code.essence.utils.client.managers.api.command.argument.IArgConsumer;
import code.essence.utils.client.managers.api.command.exception.CommandException;
import code.essence.common.repository.rct.RCTRepository;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.client.packet.network.Network;
import code.essence.Essence;
import code.essence.display.hud.Notifications;
import code.essence.features.impl.misc.HolyWorldAutoJoin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class RCTCommand extends Command implements QuickImports {
    private final RCTRepository repository;

    protected RCTCommand(Essence main) {
        super("rct");
        repository = main.getRCTRepository();
    }

    
    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (!Network.isHolyWorld() && !Network.isReallyWorld() && !Network.isSpookyTime() && !Network.isFunTime()) {
            Notifications.getInstance().addList("[RCT] Не работает на этом " + Formatting.RED + "сервере", 3000);
            return;
        }

        if (Network.isPvp()) {
            Notifications.getInstance().addList("[RCT] Вы находитесь в режиме " + Formatting.RED + "пвп", 3000);
            return;
        }

        if (Network.isHolyWorld()) {
            HolyWorldAutoJoin autoJoin = Essence.getInstance().getHolyWorldAutoJoin();
            if (autoJoin == null) {
                Notifications.getInstance().addList("[RCT] HolyWorldAutoJoin не найден", 3000);
                return;
            }
            
            if (args.hasAny()) {
                args.requireMin(1);
                int anarchy = args.getArgs().getFirst().getAs(Integer.class);
                
                autoJoin.reset();
                autoJoin.setTargetAnarchy(anarchy);
                autoJoin.setActive(true);
            } else {
                int currentAnarchy = Network.getAnarchy();
                if (currentAnarchy > 0) {
                    if (!autoJoin.isActive()) {
                        autoJoin.setActive(true);
                    } else {
                        autoJoin.reset();
                    }
                } else {
                    Notifications.getInstance().addList("[RCT] Не удалось определить номер " + Formatting.RED + "анархии", 3000);
                }
            }
            return;
        }

        if (args.hasAny()) {
            args.requireMin(1);
            int anarchy = args.getArgs().getFirst().getAs(Integer.class);
            
            
            if (Network.isFunTime() || Network.isSpookyTime()) {
                if (mc.player != null && mc.player.networkHandler != null) {
                    mc.player.networkHandler.sendChatMessage("/an" + anarchy);
                    Notifications.getInstance().addList("[RCT] Переход на анархию #" + Formatting.BLUE + anarchy + Formatting.RESET, 3000);
                }
            } else {
                
                repository.reconnect(anarchy);
            }
        } else {
            int currentAnarchy = Network.getAnarchy();
            if (currentAnarchy > 0) {
                repository.reconnect(currentAnarchy);
            } else {
                Notifications.getInstance().addList("[RCT] Не удалось определить номер " + Formatting.RED + "анархии", 3000);
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        return Stream.empty();
    }


    @Override
    public String getShortDesc() {
        return "Автоматически перезаходит на анархию";
    }


    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Перезаходит на анархию",
                "",
                "Использование:",
                "> rct <anarchy> - Заходит на <anarchy>",
                "> rct - Перезаходит на анархию где вы только что были"
        );
    }
}