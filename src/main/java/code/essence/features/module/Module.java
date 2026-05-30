package code.essence.features.module;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import code.essence.common.animation.Animation;
import code.essence.common.animation.Direction;
import code.essence.common.animation.implement.Decelerate;
import code.essence.utils.client.sound.SoundManager;
import code.essence.Essence;
import code.essence.features.module.setting.SettingRepository;
import code.essence.utils.client.managers.event.EventManager;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.display.hud.Notifications;
import code.essence.features.impl.render.Hud;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Module extends SettingRepository implements QuickImports {
    String name;
    String visibleName;
    ModuleCategory category;
    Animation animation = new Decelerate().setMs(175).setValue(1);

    public Module(String name, ModuleCategory category) {
        this.name = name;
        this.category = category;
        this.visibleName = name;
    }

    public Module(String name, String visibleName, ModuleCategory category) {
        this.name = name;
        this.visibleName = visibleName;
        this.category = category;
    }

    @NonFinal
    int key = GLFW.GLFW_KEY_UNKNOWN, type = 1;

    @NonFinal
    public boolean state;

    public void switchState() {
        setState(!state);
    }

    public void setState(boolean state) {
        animation.setDirection(state ? Direction.FORWARDS : Direction.BACKWARDS);
        if (state != this.state) {
            this.state = state;
            handleStateChange();
        }
    }

    private void handleStateChange() {
        MinecraftClient mc = MinecraftClient.getInstance();
        Hud hud = Hud.getInstance();
        if (hud == null) {
            
            if (state) {
                activate();
            } else {
                deactivate();
            }
            toggleSilent(state);
            return;
        }

        if (mc.player != null && mc.world != null) {
            if (state) {
                if (hud.notificationSettings.isSelected("Переключение модулей")) {
                    Notifications.getInstance().addList("«" +visibleName+"»" + Formatting.RESET + " включен!", 1000, false);
                    SoundManager.playSound(SoundManager.ENABLE_MODULE);
                }
                activate();
            } else {
                if (hud.notificationSettings.isSelected("Переключение модулей")) {
                    Notifications.getInstance().addList("«" +visibleName +"»" + Formatting.RESET + " выключен!", 1000, true);
                    SoundManager.playSound(SoundManager.DISABLE_MODULE);
                }
                deactivate();
            }
        }
        toggleSilent(state);
    }

    private void toggleSilent(boolean activate) {
        EventManager eventManager = Essence.getInstance().getEventManager();
        if (activate) {
            eventManager.register(this);
        } else {
            eventManager.unregister(this);
        }
    }

    public void activate() {
    }

    public void deactivate() {
    }
}
