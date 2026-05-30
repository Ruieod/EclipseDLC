package code.essence.utils.client.managers.api.command.datatypes;

import code.essence.Essence;
import code.essence.utils.client.managers.api.command.exception.CommandException;
import code.essence.utils.client.managers.api.command.helpers.TabCompleteHelper;
import code.essence.utils.client.managers.file.ClientFile;
import code.essence.utils.client.managers.file.impl.ModuleFile;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ConfigDataType implements IDatatypeFor<ClientFile> {
    INSTANCE;

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        Stream<String> friends = getList().stream().map(ModuleFile::getName);

        String context = ctx
                .getConsumer()
                .getString();

        return new TabCompleteHelper()
                .append(friends)
                .filterPrefix(context)
                .sortAlphabetically()
                .stream();
    }
    @Override
    public ClientFile get(IDatatypeContext datatypeContext) throws CommandException {
        String username = datatypeContext
                .getConsumer()
                .getString();

        return getList().stream()
                .filter(s -> s.getName().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }

    public List<? extends ModuleFile> getList() {
        return Essence.getInstance().getFileRepository().getClientFiles()
                .stream()
                .filter(clientFile -> clientFile instanceof ModuleFile)
                .map(clientFile -> (ModuleFile) clientFile)
                .collect(Collectors.toList());
    }
}
