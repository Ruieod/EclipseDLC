package code.essence.features.impl.combat;

import code.essence.Essence;
import code.essence.events.player.TickEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.BooleanSetting;
import code.essence.features.module.setting.implement.MultiSelectSetting;
import code.essence.features.module.setting.implement.RadioSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.client.Instance;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.features.aura.striking.StrikerConstructor;
import code.essence.utils.features.aura.target.TargetFinder;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.features.aura.warp.TurnsConnection;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

@Getter
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class TriggerBot extends Module implements QuickImports {

    public static TriggerBot getInstance() {
        return Instance.get(TriggerBot.class);
    }

    @NonFinal
    LivingEntity target, lastTarget;

    MultiSelectSetting targetType = new MultiSelectSetting("Тип таргета", "Фильтр целей по типу")
            .value("Игроки", "Мобы", "Животные", "Друзья", "Подставка для брони")
            .selected("Игроки", "Мобы", "Животные");

    SliderSettings attackRange = new SliderSettings("Дистанция удара", "Максимальная дистанция атаки")
            .setValue(3).range(1F, 6F);

    MultiSelectSetting attackSetting = new MultiSelectSetting("Настройки", "Опции атаки")
            .value("Только криты", "Ломать щит", "Отжимать щит", "Не бить если ешь")
            .selected("Только криты", "Ломать щит");

    RadioSetting sprintReset = new RadioSetting("Сброс спринта", "Сброс спринта перед ударом",
            new String[]{"Легитно", "Пакетно", "Не сбрасывать"}, "Легитно");

    BooleanSetting targetEsp = new BooleanSetting("Target ESP", "Показывать ESP на цели")
            .setValue(false);

    BooleanSetting smartCrits = new BooleanSetting("Криты только с пробелом", "Криты только при нажатии пробела")
            .setValue(true).visible(() -> attackSetting.isSelected("Только криты"));

    public TriggerBot() {
        super("TriggerBot", "Trigger Bot", ModuleCategory.COMBAT);
        setup(targetType, attackRange, attackSetting, sprintReset, targetEsp, smartCrits);
    }

    @Override
    public void deactivate() {
        target = null;
        lastTarget = null;
        super.deactivate();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null || mc.interactionManager == null) return;

        target = null;

        if (!(mc.crosshairTarget instanceof EntityHitResult entityHit)) return;

        if (!(entityHit.getEntity() instanceof LivingEntity living)) return;

        TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(targetType.getSelected());
        if (!filter.isValid(living)) return;

        float range = attackRange.getValue() + Aura.RANGE_MARGIN;
        if (mc.player.distanceTo(living) > range) return;

        target = living;
        lastTarget = living;

        TurnsConnection.INSTANCE.setRotation(null);

        Vec3d vec = MathAngle.getClosestVec(living).subtract(mc.player.getEyePos());
        StrikerConstructor.AttackPerpetratorConfigurable config = new StrikerConstructor.AttackPerpetratorConfigurable(
                living,
                MathAngle.fromVec3d(vec),
                range,
                attackSetting.getSelected(),
                Aura.aimMode,
                living.getBoundingBox(),
                sprintReset.get(),
                smartCrits.isValue()
        );

        Essence.getInstance().getAttackPerpetrator().performAttack(config);
    }
}
