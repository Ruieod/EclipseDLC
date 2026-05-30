package code.essence.features.impl.combat;

import code.essence.display.hud.Notifications;
import code.essence.events.keyboard.KeyEvent;
import code.essence.features.impl.render.Hud;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.BindSetting;
import code.essence.features.module.setting.implement.BooleanSetting;
import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.client.Instance;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.client.sound.SoundManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;


@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ElytraAura extends Module {

    public static ElytraAura getInstance() {
        return Instance.get(ElytraAura.class);
    }


    public static ElytraAura instance;
    public SliderSettings elytraFindRange = new SliderSettings("Дистанция наводки",
            "Дальность поиска цели во время полета на элитре")
            .setValue(32).range(6F, 64F);

    public SliderSettings elytraForward = new SliderSettings("Значение перегона", "Перегон вперёд по движению цели")
            .setValue(3).range(0F, 6F);

    final BooleanSetting forward = new BooleanSetting("Кнопка перегона", "Вкл/выкл перегон по кнопке");

    public static boolean shouldElytraTarget = false;



    public BooleanSetting defensiveEnabled = new BooleanSetting(
            "Отлетать после удара",
            "Включить/выключить анти-отлёт в элитре"
    );

    public SelectSetting defensiveType = new SelectSetting(
            "Тип отлета",
            "Режим защитного отлёта/анти-аима"
    ).value("Рандомный", "Горизонтальный", "Односторонний", "Крутящийся")
            .selected("Горизонтальный");

    public SliderSettings strenghtRollSetting = new SliderSettings(
            "Сила кручения",
            "Шаг прокрутки по Yaw в режиме Roll (градусы)"
    ).setValue(24f).range(8f, 32f)
            .visible(() -> defensiveType.isSelected("Roll"));

    public BooleanSetting jitterOnDefensiveSetting = new BooleanSetting(
            "Тряска YAW при отлете",
            "Случайный джиттер по Yaw при активном анти-отлёте"
    ).setValue(true);
    public BooleanSetting lock = new BooleanSetting(
            "Видеть наводку",
            ""
    ).setValue(true);
    public SliderSettings defensivePitchOffset = new SliderSettings(
            "Pitch отлета",
            "Смещение по Pitch для анти-аима (градусы)"
    ).setValue(-18f).range(-90f, 90f);

    public BooleanSetting rangeByter = new BooleanSetting(
            "Байт по дистанции",
            "Кружить вокруг цели на элитрах (байт по дистанции)"
    ).setValue(false);

    public SliderSettings defensiveTime = new SliderSettings(
            "Время отлета",
            "Длительность анти-отлёта после удара (мс)"
    ).setValue(300f).range(50f, 1000f);

    public ElytraAura() {
        super("ElytraAura", "Elytra Target", ModuleCategory.COMBAT);
        setup(
                elytraFindRange, elytraForward,lock, forward,
                defensiveEnabled, defensiveType, strenghtRollSetting,
                jitterOnDefensiveSetting, defensivePitchOffset,
                rangeByter, defensiveTime
        );
    }

    @EventHandler
    private void onEventKey(KeyEvent e) {
        if (e.isKeyDown(forward.getKey())) {
            float volume = SoundManager.getClientVolume();
            shouldElytraTarget = !shouldElytraTarget;
            Notifications.getInstance().addList(
                    "Elytra Forward " + (shouldElytraTarget ? "enabled!" : "disabled"),
                    1500, null
            );
            SoundManager.playSound(shouldElytraTarget ? SoundManager.ENABLE_MODULE : SoundManager.DISABLE_MODULE);
        }
    }


    public float getElytraSearchRange() {
        return elytraFindRange.getValue();
    }
}