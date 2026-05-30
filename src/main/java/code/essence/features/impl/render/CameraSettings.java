package code.essence.features.impl.render;

import code.essence.features.impl.player.FreeLook;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.MathHelper;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.*;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.client.Instance;
import code.essence.events.keyboard.HotBarScrollEvent;
import code.essence.events.keyboard.KeyEvent;
import code.essence.events.render.CameraEvent;
import code.essence.events.render.FovEvent;
import code.essence.utils.features.aura.utils.MathAngle;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class CameraSettings extends Module {

    public static CameraSettings getInstance() {
        return Instance.get(CameraSettings.class);
    }

    float fov = 110;
    float smoothFov = 30;
    float lastChangedFov = 30;

   static BooleanSetting clipSetting = new BooleanSetting("Сквозь блоки", "Камера проходит сквозь блоки").setValue(true);
    SliderSettings distanceSetting = new SliderSettings("Дистанция камеры", "Настройка расстояния камеры")
            .setValue(3.0F).range(2.0F, 15.0F);
    BindSetting zoomSetting = new BindSetting("Зум", "Клавиша для увеличения камеры");

    public CameraSettings() {
        super("CameraSettings", "CameraSettings", ModuleCategory.RENDER);
        setup(clipSetting, zoomSetting, distanceSetting);
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (e.isKeyDown(zoomSetting.getKey())) {
            fov = Math.min(lastChangedFov, mc.options.getFov().getValue() - 20);
        }
        if (e.isKeyReleased(zoomSetting.getKey(), true)) {
            lastChangedFov = fov;
            fov = mc.options.getFov().getValue();
        }
    }

    @EventHandler
    public void onHotBarScroll(HotBarScrollEvent e) {
        if (PlayerInteractionHelper.isKey(zoomSetting)) {
            fov = (int) MathHelper.clamp(fov - e.getVertical() * 10, 10, mc.options.getFov().getValue());
            e.cancel();
        }
    }

    @EventHandler
    public void onFov(FovEvent e) {
        e.setFov((int) MathHelper.clamp((smoothFov = Calculate.interpolateSmooth(1.6, smoothFov, fov)) + 1, 10, mc.options.getFov().getValue()));
        e.cancel();
    }

    @EventHandler
    public void onCamera(CameraEvent e) {
        e.setCameraClip(clipSetting.isValue());
        e.setDistance(distanceSetting.getValue());
        FreeLook freeLook = Instance.get(FreeLook.class);
        if (!freeLook.isState() || !PlayerInteractionHelper.isKey(freeLook.freeLookSetting)) {
            e.setAngle(MathAngle.cameraAngle());
        }
        e.cancel();
    }
}