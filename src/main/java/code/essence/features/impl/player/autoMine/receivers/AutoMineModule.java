package code.essence.features.impl.player.autoMine.receivers;

import code.essence.events.packet.PacketEvent;
import code.essence.events.player.InputEvent;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import code.essence.events.player.RotationUpdateEvent;
import code.essence.events.player.TickEvent;
import code.essence.events.render.DrawEvent;
import code.essence.events.render.WorldLoadEvent;
import code.essence.events.render.WorldRenderEvent;
import code.essence.features.impl.movement.autoMystGlider.parsers.SpookyParser;
import code.essence.features.impl.player.autoMine.controllers.AutoMineController;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.TextSetting;
import code.essence.utils.client.managers.event.EventHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * @author nikitavodolaz
 * @since 08.02.2026
 */

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoMineModule extends Module {
    AutoMineController controller = new AutoMineController();

    TextSetting anarchyNumber = new TextSetting("Anarchy", "")
            .setText("Anarchy")
            .setMin(3)
            .setMax(4);

    TextSetting homeName = new TextSetting("Home", "")
            .setText("Home")
            .setMax(16)
            .setMin(0);

    public AutoMineModule() {
        super("AutoMine", ModuleCategory.PLAYER);
        setup(anarchyNumber, homeName);
    }

    @EventHandler
    public void tickEventReceive(TickEvent tickEvent) {
        if (SpookyParser.isAnarchy(-1)) return;
        controller.onTick();
    }

    @EventHandler
    public void rotationUpdateEventReceive(RotationUpdateEvent rotationUpdateEvent) {
        if (SpookyParser.isAnarchy(-1)) return;
        controller.onRotationUpdate(rotationUpdateEvent);
    }

    @EventHandler
    public void worldChangeEventReceive(WorldLoadEvent worldLoadEvent) {
        if (SpookyParser.isAnarchy(-1)) return;
        if (controller != null) controller.onWorldChange();
    }

    @EventHandler
    public void packetEventReceive(PacketEvent packetEvent) {
        if (SpookyParser.isAnarchy(-1)) return;
        if (packetEvent.getType() != PacketEvent.Type.RECEIVE) return;
        if (mc.player == null || controller == null) return;

        if (packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessage) {
            var content = gameMessage.content();
            if (content == null) return;
            String message = content.getString();
            if (message != null && (message.contains("Вы уже подключены на этот сервер!") || message.contains("Вы уже подключены к этому серверу!"))) {
                controller.notifyAlreadyConnected();
            }
        }
    }

    @EventHandler
    public void drawEventReceive(DrawEvent drawEvent) {
        if (SpookyParser.isAnarchy(-1)) return;

        controller.onDraw(drawEvent);
    }

    @EventHandler
    public void inputEventReceive(InputEvent inputEvent) {
        if (SpookyParser.isAnarchy(-1)) return;

        controller.onInput(inputEvent);
    }

    @EventHandler
    public void worldRenderEventReceive(WorldRenderEvent worldRenderEvent) {
        if (SpookyParser.isAnarchy(-1)) return;

        controller.onWorldRender(worldRenderEvent);
    }

}
