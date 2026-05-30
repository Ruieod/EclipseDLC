package code.essence.utils.client;

import lombok.experimental.UtilityClass;
import code.essence.utils.client.managers.api.draggable.AbstractDraggable;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleProvider;
import code.essence.features.module.ModuleRepository;
import code.essence.Essence;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@UtilityClass
public class Instance {
    private final ConcurrentMap<Class<? extends Module>, Module> instanceModules = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends AbstractDraggable>, AbstractDraggable> instanceDraggables = new ConcurrentHashMap<>();

    public <T extends Module> T get(Class<T> clazz) {
        return clazz.cast(instanceModules.computeIfAbsent(clazz, instance -> {
            Essence essence = Essence.getInstance();
            if (essence == null) {
                return null;
            }
            
            
            ModuleProvider moduleProvider = essence.getModuleProvider();
            if (moduleProvider != null) {
                return moduleProvider.get(instance);
            }
            
            
            ModuleRepository moduleRepository = essence.getModuleRepository();
            if (moduleRepository != null) {
                return moduleRepository.modules().stream()
                    .filter(m -> instance.isAssignableFrom(m.getClass()))
                    .map(instance::cast)
                    .findFirst()
                    .orElse(null);
            }
            
            return null;
        }));
    }

    public <T extends Module> T get(String module) {
        Essence essence = Essence.getInstance();
        if (essence == null) {
            return null;
        }
        
        
        ModuleProvider moduleProvider = essence.getModuleProvider();
        if (moduleProvider != null) {
            return moduleProvider.get(module);
        }
        
        
        ModuleRepository moduleRepository = essence.getModuleRepository();
        if (moduleRepository != null) {
            return (T) moduleRepository.modules().stream()
                .filter(m -> m.getName().equalsIgnoreCase(module))
                .findFirst()
                .orElse(null);
        }
        
        return null;
    }

    public <T extends AbstractDraggable> T getDraggable(Class<T> clazz) {
        return clazz.cast(instanceDraggables.computeIfAbsent(clazz, instance -> Essence.getInstance().getDraggableRepository().get(instance)));
    }

    public <T extends AbstractDraggable> T getDraggable(String draggable) {
        return Essence.getInstance().getDraggableRepository().get(draggable);
    }
}
