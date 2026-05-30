package code.essence.features.impl.render.jumpCircle.receiver;



import code.essence.common.repository.friend.FriendUtils;
import code.essence.events.player.JumpEvent;
import code.essence.events.render.WorldRenderEvent;
import code.essence.features.impl.render.jumpCircle.data.JumpCircleStorage;
import code.essence.features.impl.render.jumpCircle.handler.JumpCircleHandler;
import code.essence.features.impl.render.jumpCircle.updater.JumpCircleUpdater;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.ColorSetting;
import code.essence.features.module.setting.implement.MultiSelectSetting;
import code.essence.features.module.setting.implement.RadioSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.display.color.ColorAssist;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.player.PlayerEntity;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JumpCircleModule extends Module {
    SliderSettings lifeTime = new SliderSettings("Время жизни", "")
            .range(500, 1500)
            .setInteger(true);

    SliderSettings size = new SliderSettings("Размер", "")
            .range(1, 3).step(0.5F);




    public static RadioSetting colorMode = new RadioSetting("Режим цвета", "Выбор источника цвета",
            new String[]{"Клиентский", "Кастом"}, "Клиентский");
    public static ColorSetting color = new ColorSetting("Кастомный цвет", "Кастомный цвет круга")
            .value(ColorAssist.getClientColor())
            .visible(() -> colorMode.get().equals("Кастом"));

    JumpCircleStorage storage = new JumpCircleStorage(this);
    JumpCircleHandler handler = new JumpCircleHandler(this);
    JumpCircleUpdater updater = new JumpCircleUpdater(this);

    public JumpCircleModule() {
        super("JumpCircle", ModuleCategory.RENDER);
        setup( lifeTime, size, colorMode, color);
    }

    @EventHandler
    private void jumpEventReceive(JumpEvent jumpEvent) {
        PlayerEntity player = jumpEvent.getPlayer();

            storage.add(player);
        }


    @EventHandler
    private void renderEventReceive(WorldRenderEvent renderEvent) {
        if (storage.isPresent()) {
            updater.update();
            handler.provideRender(renderEvent.getStack(), renderEvent.getPartialTicks());
        }
    }
}
