package code.essence.commands.defaults;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import code.essence.utils.client.managers.api.command.Command;
import code.essence.utils.client.managers.api.command.argument.IArgConsumer;
import code.essence.utils.client.managers.api.command.datatypes.*;
import code.essence.utils.client.managers.api.command.exception.CommandException;
import code.essence.utils.client.managers.api.command.helpers.Paginator;
import code.essence.utils.client.managers.api.command.helpers.TabCompleteHelper;
import code.essence.common.repository.way.WayRepository;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.Essence;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static code.essence.utils.client.managers.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class WayCommand extends Command implements QuickImports {
    final WayRepository wayRepository;

    protected WayCommand(Essence main) {
        super("way", "gps");
        wayRepository = main.getWayRepository();
    }

    
    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            handleListWays(label, args);
            return;
        }
        String first = args.peekString().toLowerCase(Locale.US);
        switch (first) {
            case "add" -> { args.getString(); handleAddWay(args); }
            case "remove" -> { args.getString(); handleRemoveWay(args); }
            case "clear" -> { args.getString(); handleClearWays(args); }
            case "list" -> { args.getString(); handleListWays(label, args); }
            default -> handleAddWayShort(args);
        }
    }

    /** Добавление без "add": .way name [x y z] или .gps name [x y z] */
    private void handleAddWayShort(IArgConsumer args) throws CommandException {
        String name = args.getString();
        String address = mc.getNetworkHandler() == null ? "vanilla" : mc.getNetworkHandler().getServerInfo() == null ? "vanilla" : mc.getNetworkHandler().getServerInfo().address;
        if (args.has(3)) {
            int x = args.getAs(Integer.class);
            int y = args.getAs(Integer.class);
            int z = args.getAs(Integer.class);
            wayRepository.upsertWay(name, new BlockPos(x, y, z), address);
            logDirect("Метка сохранена " + name + ", Координаты: (" + x + ", " + y + ", " + z + "), Сервер: " + address, Formatting.WHITE);
        } else if (!args.hasAny()) {
            if (mc.player == null) {
                logDirect("Игрок не найден", Formatting.RED);
                return;
            }
            BlockPos pos = mc.player.getBlockPos();
            wayRepository.upsertWay(name, pos, address);
            logDirect("Метка добавлена: " + name + " (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "), Сервер: " + address, Formatting.WHITE);
        } else {
            args.requireMax(0);
        }
    }

    
    private void handleAddWay(IArgConsumer args) throws CommandException {
        
        if (!args.hasAny()) {
            if (mc.player == null) {
                logDirect("Игрок не найден", Formatting.RED);
                return;
            }
            BlockPos pos = mc.player.getBlockPos();
            String name = pos.getX() + " " + pos.getY() + " " + pos.getZ();
            String address = mc.getNetworkHandler() == null ? "vanilla" : mc.getNetworkHandler().getServerInfo() == null ? "vanilla" : mc.getNetworkHandler().getServerInfo().address;
            wayRepository.upsertWay(name, pos, address);
            logDirect("Метка добавлена: (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "), Сервер: " + address, Formatting.WHITE);
            return;
        }

        
        args.requireMin(4);
        String name = args.getString();
        int x = args.getArgs().get(0).getAs(Integer.class);
        int y = args.getArgs().get(1).getAs(Integer.class);
        int z = args.getArgs().get(2).getAs(Integer.class);

        String address = mc.getNetworkHandler() == null ? "vanilla" : mc.getNetworkHandler().getServerInfo() == null ? "vanilla" : mc.getNetworkHandler().getServerInfo().address;
        wayRepository.upsertWay(name, new BlockPos(x, y, z), address);
        logDirect("Метка сохранена " + name + ", Координаты:" + " (" + x + ", " + y + ", " + z + "), Сервер: " + address, Formatting.WHITE);
    }

    
    private void handleRemoveWay(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        String name = args.getString();
        if (wayRepository.hasWay(name)) {
            wayRepository.deleteWay(name);
            logDirect(Formatting.GREEN + "Метка " + Formatting.RED + name + Formatting.GREEN + " была успешна удалена!");
        } else logDirect("Метка с названием '" + name + "' не найдена!");
    }

    
    private void handleListWays(String label, IArgConsumer args) throws CommandException {
        args.requireMax(1);
        Paginator.paginate(args, new Paginator<>(wayRepository.wayList),
                () -> logDirect("Список меток:"),
                way -> Text.literal(Formatting.GRAY + "Название: " + Formatting.RED + way.name())
                        .append(Text.literal(Formatting.GRAY + " Координаты: " + Formatting.WHITE + " (" + way.pos().getX() + ", " + way.pos().getY() + ", " + way.pos().getZ() + ")")
                                .append(Text.literal(Formatting.GRAY + " Сервер: " + Formatting.WHITE + way.server()))), FORCE_COMMAND_PREFIX + label);
    }

    
    private void handleClearWays(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        wayRepository.clearList();
        logDirect(Formatting.GREEN + "Все метки были удалены.");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            return new TabCompleteHelper().sortAlphabetically().prepend("add", "remove", "list", "clear").stream();
        }
        String first = args.peekString().toLowerCase(Locale.US);
        if (first.equals("remove")) {
            args.getString();
            if (args.hasExactlyOne()) return args.tabCompleteDatatype(WayDataType.INSTANCE);
            return Stream.empty();
        }
        if (first.equals("add")) {
            args.getString();
            String hint = args.has(4) ? "" : args.has(3) ? "z" : args.has(2) ? "y" : args.has(1) ? "x" : "Название";
            return new TabCompleteHelper().sortAlphabetically().prepend(hint).stream();
        }
        if (first.equals("list") || first.equals("clear")) {
            return Stream.empty();
        }
        int left = args.getArgs().size();
        String hint = left >= 4 ? "" : left == 3 ? "z" : left == 2 ? "y" : left == 1 ? "x" : "название";
        return new TabCompleteHelper().sortAlphabetically().prepend(hint).stream();
    }


    @Override
    public String getShortDesc() {
        return "Позволяет ставить метки в мире";
    }

    
    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "С помощью этой команды можно добавлять/удалять метки в мире (.way и .gps)",
                "",
                "Использование (короткая форма — без add):",
                "> way <name> - Добавляет метку на текущих координатах",
                "> way <name> <x> <y> <z> - Добавляет метку по координатам",
                "> way add - То же, метка на текущих координатах (имя = координаты)",
                "> way add <name> <x> <y> <z> - Добавляет/перезаписывает метку",
                "> way remove <name> - Удаляет метку",
                "> way list - Список меток",
                "> way clear - Очищает список меток."
        );
    }
}