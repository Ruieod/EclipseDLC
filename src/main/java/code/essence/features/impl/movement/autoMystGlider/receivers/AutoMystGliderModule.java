package code.essence.features.impl.movement.autoMystGlider.receivers;

import code.essence.events.keyboard.KeyEvent;
import code.essence.events.packet.PacketEvent;
import code.essence.events.player.InputEvent;
import code.essence.events.player.MoveEvent;
import code.essence.events.player.RotationUpdateEvent;
import code.essence.events.player.TickEvent;
import code.essence.features.impl.movement.autoMystGlider.controllers.AutoGliderController;
import code.essence.features.impl.movement.autoMystGlider.states.ConnectionState;
import code.essence.features.impl.movement.autoMystGlider.states.GlidePhase;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.BindSetting;
import code.essence.features.module.setting.implement.TextSetting;
import code.essence.utils.client.chat.ChatMessage;
import code.essence.utils.client.managers.event.EventHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author nikitavodolaz
 * @since 01.02.2026
 */

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoMystGliderModule extends Module {
    AutoGliderController autoGliderController;

    final BindSetting bindSetting = new BindSetting("Point", "")
            .setKey(GLFW.GLFW_KEY_V);

    final TextSetting anarchyText = new TextSetting("Anarchy", "")
            .setText("502")
            .setMax(3)
            .setMin(3);

    final TextSetting homeName = new TextSetting("Home", "")
            .setText("hata");

    public AutoMystGliderModule() {
        super("AutoMystGlider", ModuleCategory.MOVEMENT);
        setup(bindSetting, anarchyText, homeName);
    }

    @EventHandler
    public void inputEventReceive(InputEvent inputEvent) {
        if (mc.player == null || autoGliderController == null) return;
        autoGliderController.onInput(inputEvent);
    }

    @EventHandler
    public void tickEventReceive(TickEvent tickEvent) {
        if (mc.player == null || autoGliderController == null) return;
        autoGliderController.onTick();
    }

    @EventHandler
    public void moveEventReceive(MoveEvent moveEvent) {
        if (mc.player == null || autoGliderController == null) return;
        autoGliderController.onMove(moveEvent);
    }

    @EventHandler
    public void packetEventReceive(PacketEvent packetEvent) {
        if (packetEvent.getPacket() instanceof GameMessageS2CPacket p) {
            String s = p.content().getString();
            s = s.replaceAll("§.", "");

            Pattern pattern = Pattern.compile("(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)");

            for (String line : s.split("\n")) {
                if (line.toLowerCase().contains("появился на координатах") || line.toLowerCase().contains("координаты:")) {
                    Matcher matcher = pattern.matcher(line);

                    if (matcher.find()) {
                        int x = Integer.parseInt(matcher.group(1));
                        int y = Integer.parseInt(matcher.group(2));
                        int z = Integer.parseInt(matcher.group(3));

                        mystLocation(new Vec3d(x, y, z));
                    }
                }
            }
        }

        if (mc.player == null || autoGliderController == null) return;
        autoGliderController.onPacket(packetEvent);
    }

    @EventHandler
    public void rotationUpdateReceive(RotationUpdateEvent rotationUpdateEvent) {
        if (mc.player == null || autoGliderController == null) return;
        autoGliderController.onRotationUpdate(rotationUpdateEvent);
    }

    @EventHandler
    public void keyEventReceive(KeyEvent keyEvent) {
        if (keyEvent.key() == bindSetting.getKey() && keyEvent.action() == 1) {
            mystLocation(mc.player.getPos());
        }
    }

    private void mystLocation(Vec3d vec3d) {
        // Если в фазе WAITING_COORDS — координаты пришли, создаём новый контроллер и летим
        if (autoGliderController != null) {
            GlidePhase phase = autoGliderController.getConnectionHandler().getConnection().getPhase();
            if (phase == GlidePhase.WAITING_COORDS) {
                ChatMessage.brandmessage("Координаты получены! Летим на " + vec3d);
            }
            autoGliderController.reload();
        }

        autoGliderController = new AutoGliderController(vec3d, this);
        logDirect("Place marker on coordinate: " + vec3d.toString());
    }

    @Override
    public void deactivate() {
        if (autoGliderController != null) {
            autoGliderController.reload();
        }

        super.deactivate();
    }

    @Override
    public void activate() {
        if (autoGliderController != null) {
            autoGliderController.reload();
        }

        // Создаём контроллер с заглушечными координатами (реальные придут из чата)
        autoGliderController = new AutoGliderController(new Vec3d(0, 0, 0), this);

        // Стартуем цикл: /hub → API → анархия → ждём координаты
        ConnectionState conn = autoGliderController.getConnectionHandler().getConnection();
        conn.setConnected(false);
        conn.transitionTo(GlidePhase.HUB_TO_NEXT);

        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendCommand("hub");
        }

        ChatMessage.brandmessage("[AutoMyst] Модуль включён. Фаза: HUB_TO_NEXT — ищем ивент из API...");

        super.activate();
    }
}
