package code.essence.commands;

import code.essence.utils.client.managers.api.command.ICommandSystem;
import code.essence.utils.client.managers.api.command.argparser.IArgParserManager;
import code.essence.commands.argparser.ArgParserManager;

public enum CommandSystem implements ICommandSystem {
    INSTANCE;

    @Override
    public IArgParserManager getParserManager() {
        return ArgParserManager.INSTANCE;
    }
}
