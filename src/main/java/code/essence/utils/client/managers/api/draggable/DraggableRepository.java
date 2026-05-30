package code.essence.utils.client.managers.api.draggable;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import code.essence.display.hud.*;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DraggableRepository {
    List<AbstractDraggable> draggable = new ArrayList<>();

    
    
    public void setup() {
        register(
                new TargetHud(),
                new TargetHud2(),
                new Potions(),
                new Keybinds(),

                new Armor(),
                new Watermark(),
                new Inventory(),
                new CoolDowns(),
                new StaffList(),
                new BindsHw(),
                new Notifications(),



                new Info()
        );
    }

    
    
    public void register(AbstractDraggable... module) {
        draggable.addAll(List.of(module));
    }

    public List<AbstractDraggable> draggable() {
        return draggable;
    }

    public <T extends AbstractDraggable> T get(String name) {
        return draggable.stream()
                .filter(module -> module.getName().equalsIgnoreCase(name))
                .map(module -> (T) module)
                .findFirst()
                .orElse(null);
    }

    public <T extends AbstractDraggable> T get(Class<T> clazz) {
        return draggable.stream()
                .filter(module -> clazz.isAssignableFrom(module.getClass()))
                .map(clazz::cast)
                .findFirst()
                .orElse(null);
    }
}
