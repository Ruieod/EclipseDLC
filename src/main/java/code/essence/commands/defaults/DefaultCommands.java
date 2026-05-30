package code.essence.commands.defaults;

import code.essence.Essence;
import code.essence.utils.client.managers.api.command.ICommand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DefaultCommands {
    public static List<ICommand> createAll() {
        Essence main = Essence.getInstance();
        List<ICommand> commands = new ArrayList<>(Arrays.asList(
                new ConfigCommand(main),
                new ThemeCommand(main),
                new MacroCommand(main),
                new BindCommand(main),
                new WayCommand(main),
                new FriendCommand(),
                new IRCCommand(),
                new PrefixCommand(),
                new TargetCommand(),
                new StaffCommand(),
                new BlockESPCommand(),
                new RCTCommand(main),
                new DragCommand(),
                new ABCommand(),
                new VClipCommand(),
                new HelpCommand(main)

        ));
        return Collections.unmodifiableList(commands);
    }
}