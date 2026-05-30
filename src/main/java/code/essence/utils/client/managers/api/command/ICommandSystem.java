package code.essence.utils.client.managers.api.command;

import code.essence.utils.client.managers.api.command.argparser.IArgParserManager;

public interface ICommandSystem {
    IArgParserManager getParserManager();
}
