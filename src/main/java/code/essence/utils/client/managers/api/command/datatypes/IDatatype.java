package code.essence.utils.client.managers.api.command.datatypes;

import code.essence.utils.client.managers.api.command.exception.CommandException;
import code.essence.utils.display.interfaces.QuickImports;

import java.util.stream.Stream;

public interface IDatatype extends QuickImports {
    Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException;
}
