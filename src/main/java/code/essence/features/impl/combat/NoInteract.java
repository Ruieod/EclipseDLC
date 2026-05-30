package code.essence.features.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.client.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoInteract extends Module {
    public static NoInteract getInstance() {
        return Instance.get(NoInteract.class);
    }

    public NoInteract() {
        super("NoInteract", "NoInteract", ModuleCategory.COMBAT);
    }
}
