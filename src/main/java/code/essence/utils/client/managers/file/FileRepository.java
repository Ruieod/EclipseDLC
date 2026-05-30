package code.essence.utils.client.managers.file;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import code.essence.utils.client.managers.file.impl.*;
import code.essence.Essence;

import java.util.ArrayList;
import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileRepository {
    List<ClientFile> clientFiles = new ArrayList<>();

    public void setup(Essence main) {
        register(
                new ModuleFile(main.getModuleRepository(), main.getDraggableRepository()),
                new EntityESPFile(main.getBoxESPRepository()),
                new BlockESPFile(main.getBoxESPRepository()),
                new MacroFile(main.getMacroRepository()),
                new WayFile(main.getWayRepository()),
                new PrefixFile(),
                new FriendFile(),
                new StaffFile(),
                new ElementsFile(main.getDraggableRepository())
        );
    }

    public void register(ClientFile... clientFIle) {
        clientFiles.addAll(List.of(clientFIle));
    }
}
