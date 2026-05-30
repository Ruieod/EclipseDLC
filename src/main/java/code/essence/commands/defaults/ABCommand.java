package code.essence.commands.defaults;

import code.essence.Essence;
import code.essence.utils.client.managers.api.command.Command;
import code.essence.utils.client.managers.api.command.argument.IArgConsumer;
import code.essence.utils.client.managers.api.command.exception.CommandException;
import code.essence.utils.client.managers.api.command.helpers.TabCompleteHelper;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;


public class ABCommand extends Command {

    public ABCommand() {
        super("ab", "autobuy");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String arg = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "help";

        switch (arg) {
            case "ban" -> {
                if (!args.hasAny()) {
                    logDirect("Использование: .ab ban <ник>", Formatting.RED);
                    return;
                }
                String name = args.getString();

            }
            case "unban" -> {
                if (!args.hasAny()) {
                    logDirect("Использование: .ab unban <ник>", Formatting.RED);
                    return;
                }
            }
            case "list" -> {

            }
            case "clear" -> {

            }
            default -> {
                logDirect("Команды автобая:", Formatting.WHITE);
                logDirect("  .ab ban <ник> - забанить игрока", Formatting.GRAY);
                logDirect("  .ab unban <ник> - разбанить игрока", Formatting.GRAY);
                logDirect("  .ab list - список забаненных", Formatting.GRAY);
                logDirect("  .ab clear - очистить бан-лист", Formatting.GRAY);
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAny()) {
            String arg = args.getString();
            if (args.hasExactlyOne()) {
                if (arg.equalsIgnoreCase("unban")) {

                }
            } else {
                return new TabCompleteHelper()
                        .sortAlphabetically()
                        .prepend("ban", "unban", "list", "clear")
                        .filterPrefix(arg)
                        .stream();
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление бан-листом автобая";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда для управления бан-листом автобая.",
                "Предметы от забаненных игроков не будут покупаться.",
                "",
                "Использование:",
                "> ab ban <ник> - Добавить игрока в бан-лист",
                "> ab unban <ник> - Удалить игрока из бан-листа",
                "> ab list - Показать список забаненных",
                "> ab clear - Очистить бан-лист"
        );
    }
}
