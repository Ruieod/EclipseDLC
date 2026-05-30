package code.essence.features.module;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.Formatting;
import code.essence.features.module.exception.ModuleException;
import code.essence.utils.client.managers.event.EventManager;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.events.keyboard.KeyEvent;
import code.essence.common.logger.implement.ConsoleLogger;
import code.essence.utils.display.interfaces.QuickLogger;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleSwitcher implements QuickLogger, QuickImports {
    List<Module> modules;

    public ModuleSwitcher(List<Module> modules, EventManager eventManager) {
        this.modules = modules;
        eventManager.register(this);
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        for (Module module : modules) {
            if (event.key() == module.getKey() && mc.currentScreen == null) {
                try {
                    
                    if (module.getName().equals("ClickGui") && event.action() == 0) {
                        code.essence.display.screens.clickgui.MenuScreen.INSTANCE.openGui();
                        return;
                    }
                    handleModuleState(module, event.action());
                } catch (Exception e) {
                    handleException(module.getName(), e);
                }
            }
        }
    }

    public void handleModuleState(Module module, int action) {
        if (module.getName().equals("ClickGui")) {
            return;
        }

        if (module.getType() == 1 && action == 1) {
            module.switchState();
        }
    }

    public void handleException(String moduleName, Exception e) {
        final ConsoleLogger consoleLogger = new ConsoleLogger();

        if (e instanceof ModuleException) {
            logDirect("[" + moduleName + "] " + Formatting.RED + e.getMessage());
        } else {
            consoleLogger.log("Error in module " + moduleName + ": " + e.getMessage());
        }
    }
}
