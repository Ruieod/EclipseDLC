/*
package code.essence.commands.defaults;

import code.essence.features.impl.misc.CreeperFarm;
import code.essence.utils.client.managers.api.command.Command;
import code.essence.utils.client.managers.api.command.argument.IArgConsumer;
import code.essence.utils.client.managers.api.command.exception.CommandException;
import code.essence.utils.client.managers.api.command.helpers.TabCompleteHelper;
import code.essence.utils.display.interfaces.QuickImports;
import net.minecraft.util.Formatting;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class CFCommand extends Command implements QuickImports {

    public CFCommand() {
        super("creeper", "creeperfarm", "creepfarm");
    }

    private CreeperFarm cf() {
        return code.essence.utils.client.Instance.get(CreeperFarm.class);
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (args.hasAtMost(0)) {
            error();
            return;
        }
        String commandType = args.getString().toLowerCase(Locale.US);
        CreeperFarm farm = cf();
        if (farm == null) {
            logDirect(Formatting.RED + "Модуль CreeperFarm недоступен.");
            return;
        }

        switch (commandType) {
            case "pos1" -> {
                if (mc.player == null) {
                    logDirect(Formatting.RED + "Игрок не найден.");
                    return;
                }
                farm.setPos1(new Vector3f((float) mc.player.getX(), (float) mc.player.getY(), (float) mc.player.getZ()));
                logDirect("Первая точка установлена.");
            }
            case "pos2" -> {
                if (mc.player == null) {
                    logDirect(Formatting.RED + "Игрок не найден.");
                    return;
                }
                farm.setPos2(new Vector3f((float) mc.player.getX(), (float) mc.player.getY(), (float) mc.player.getZ()));
                logDirect("Вторая точка установлена.");
            }
            case "clear" -> {
                if (!args.hasAny()) {
                    logDirect(Formatting.RED + "Укажите подкоманду: pos1, pos2, all");
                    return;
                }
                String sub = args.getString().toLowerCase(Locale.US);
                switch (sub) {
                    case "pos1" -> {
                        farm.resetPos1();
                        logDirect("Первая точка сброшена.");
                    }
                    case "pos2" -> {
                        farm.resetPos2();
                        logDirect("Вторая точка сброшена.");
                    }
                    case "all" -> {
                        farm.resetPos1();
                        farm.resetPos2();
                        logDirect("Обе точки сброшены.");
                    }
                    default -> logDirect(Formatting.RED + "Неверная подкоманда: pos1, pos2, all");
                }
            }
            default -> error();
        }
    }

    private void error() {
        logDirect(Formatting.RED + "Использование: pos1, pos2, clear [pos1|pos2|all]");
        getLongDesc().forEach(line -> logDirect(Formatting.GRAY + line));
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            return new TabCompleteHelper().sortAlphabetically().prepend("pos1", "pos2", "clear").stream();
        }
        String first = args.peekString();
        if (args.has(2) && first.equalsIgnoreCase("clear")) {
            return new TabCompleteHelper().sortAlphabetically().prepend("pos1", "pos2", "all").filterPrefix(args.peekString(1)).stream();
        }
        return new TabCompleteHelper().sortAlphabetically().prepend("pos1", "pos2", "clear").filterPrefix(first).stream();
    }

    @Override
    public String getShortDesc() {
        return "Управление точками CreeperFarm";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                ".creeper pos1 - Установить первую точку на текущих координатах",
                ".creeper pos2 - Установить вторую точку на текущих координатах",
                ".creeper clear pos1 - Сбросить первую точку",
                ".creeper clear pos2 - Сбросить вторую точку",
                ".creeper clear all - Сбросить обе точки"
        );
    }
}
*/
