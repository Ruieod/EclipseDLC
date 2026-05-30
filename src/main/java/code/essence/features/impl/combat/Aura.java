package code.essence.features.impl.combat;

import code.essence.events.player.TickEvent;
import code.essence.features.impl.combat.ElytraTargetUtil.ElytraOptimizedMode;
import code.essence.features.impl.combat.ElytraTargetUtil.ElytraTargetUtil;
import code.essence.features.impl.movement.TargetStrafe;

import code.essence.utils.display.render.geometry.Render3D;
import code.essence.utils.features.aura.point.MultiPoint;
import code.essence.utils.features.aura.rotations.constructor.LinearConstructor;
import code.essence.utils.features.aura.rotations.constructor.RotateConstructor;
import code.essence.utils.features.aura.rotations.impl.*;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.features.aura.warp.TurnsConfig;
import code.essence.utils.features.aura.warp.Turns;
import code.essence.utils.features.aura.warp.TurnsConnection;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.client.managers.event.types.EventType;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.*;
import code.essence.utils.client.Instance;
import code.essence.utils.math.task.TaskPriority;
import code.essence.Essence;
import code.essence.events.packet.PacketEvent;
import code.essence.events.player.RotationUpdateEvent;
import code.essence.display.hud.Notifications;
import code.essence.utils.features.aura.striking.StrikeManager;
import code.essence.utils.features.aura.striking.StrikerConstructor;
import code.essence.utils.features.aura.target.TargetFinder;
import code.essence.features.impl.render.Hud;
import net.minecraft.util.shape.VoxelShapes;

import java.awt.*;

import static code.essence.features.impl.combat.ElytraTargetUtil.ElytraTargetUtil.*;


@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Aura extends Module {

    public static float RANGE_MARGIN = 0.153F;

    public static Aura getInstance() {
        return Instance.get(Aura.class);
    }

    TargetFinder targetSelector = new TargetFinder();
    MultiPoint pointFinder = new MultiPoint();

    @NonFinal
    LivingEntity target, lastTarget;

    @NonFinal
    long shiftTapEndTime = 0;

    public static boolean fakeRotate = true;

    @NonFinal
    public float TPS = 20;
    @NonFinal
    public float adjustTicks = 0;
    @NonFinal
    public long timestamp;

    @NonFinal
    @Getter
    public static float legitSprintNeed;


    public static   RadioSetting aimMode = new RadioSetting("Наводка", "Выберите тип наводки",
            new String[]{"Матрикс", "ФанТайм", "ХолиВорлд", "СпукиТайм", "РилиВорлд"}, "СпукиТайм");

    MultiSelectSetting targetType = new MultiSelectSetting("Тип таргета", "Фильтрует весь список целей по типу")
            .value("Игроки", "Мобы", "Животные", "Друзья", "Подставка для брони")
            .selected("Игроки", "Мобы", "Животные");
    public static SliderSettings attackRange = new SliderSettings("Дистанция удара", "Дальность атаки до цели")
            .setValue(3).range(1F, 6F);

    public static SliderSettings lookRange = new SliderSettings("Дистанция поиска", "Диапазон поиска до цели")
            .setValue(1.5f).range(0F, 10F);

    MultiSelectSetting attackSetting = new MultiSelectSetting("Настройки", "Позволяет настроить работу функции")
            .value("Только криты", "Ломать щит", "Отжимать щит", "Не бить если ешь", "Игнорировать стены", "Синхронизировать с ТПС", "Не бить на элитре")
            .selected("Только криты", "Ломать щит");

    RadioSetting correctionType = new RadioSetting("Коррекция", "Выбор коррекции движения игрока",
            new String[]{"Сфокусированная", "Свободная", "Незаметная"}, "Сфокусированная");

    RadioSetting sprintReset = new RadioSetting("Сброс спринта", "Выбор сброса спринта перед ударом",
            new String[]{"Легитно", "Пакетно", "Не сбрасывать"}, "Легитно");
    RadioSetting cps = new RadioSetting("Пвп", "Выбор версии пвп",
            new String[]{"1.9", "1.8"}, "1.9");


    BooleanSetting smartCrits = new BooleanSetting("Криты только с пробелом", "Криты только при нажатии пробела")
            .setValue(true).visible(() -> attackSetting.isSelected("Только криты"));


    public Aura() {
        super("Aura", ModuleCategory.COMBAT);
        setup(aimMode, targetType, attackRange, lookRange, attackSetting, smartCrits, correctionType, sprintReset, cps);
    }

    @Override
    public void activate() {
       
      
       code.essence.utils.features.aura.context.AutoRegressionContext autoContext =
                code.essence.utils.features.aura.context.AutoRegressionContext.getInstance();
        autoContext.setCdMinecraft(1000 / 12);
        code.essence.utils.features.aura.context.AutoRegressionContext.hitContentClear();
        timestamp = System.nanoTime();
        super.activate();
    }

    @Override
    public void deactivate() {
        targetSelector.releaseTarget();
        target = null;
//        TurnsConnection.INSTANCE.clear();
//        TurnsConnection.INSTANCE.setRotation(null);
        timestamp = 0;
        TPS = 20;
        adjustTicks = 0;
        super.deactivate();
    }
    @EventHandler
    public void onPacket(PacketEvent e) {
        if (attackSetting.isSelected("Синхронизировать с ТПС") && e.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            long currentTime = System.nanoTime();
            if (timestamp == 0) {
                timestamp = currentTime;
                return;
            }
            long delay = currentTime - timestamp;

            float maxTPS = 20;
            float rawTPS = maxTPS * (1e9f / delay);

            float boundedTPS = MathHelper.clamp(rawTPS, 0, maxTPS);

            TPS = (float) limitDecimals(boundedTPS, 2);

            adjustTicks = boundedTPS - maxTPS;

            timestamp = currentTime;
        }
        if (e.getPacket() instanceof EntityStatusS2CPacket status && status.getStatus() == 30) {
            Entity entity = status.getEntity(mc.world);
            if (entity != null && entity.equals(target) && Hud.getInstance().notificationSettings.isSelected("Ломании щита")) {
                Notifications.getInstance().addList(Text.literal("Сломали щит игроку - ").append(entity.getDisplayName()), 2000);
            }
        }
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        switch (e.getType()) {
            case EventType.PRE -> {
                target = updateTarget();
                if (target != null) {
                    rotateToTarget(getConfig());
                    lastTarget = target;
                } else {
//                    TurnsConnection.INSTANCE.clear();
//                    TurnsConnection.INSTANCE.setRotation(null);
                }
            }
            case EventType.POST -> {
                if (target != null) {
                    if (ElytraTargetUtil.isElytraFlying() && shouldUseElytraMode()) {
                        ElytraTargetUtil.rotateToTargetElytraUltraFast();
                        ElytraTargetUtil.attackInElytra();
                        return;
                    }
                    else {
                        Essence.getInstance().getAttackPerpetrator().performAttack(getConfig());
                    }
                }
            }
        }
    }


    private LivingEntity updateTarget() {
        TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(targetType.getSelected());


        float range = attackRange.getValue() + RANGE_MARGIN + (lookRange.getValue());
        if (ElytraAura.getInstance().isState() && isElytraFlying()) {
            range = ElytraAura.getInstance().getElytraSearchRange();
        }
        targetSelector.searchTargets(mc.world.getEntities(), range, 360, attackSetting.isSelected("Игнорировать стены"));
        targetSelector.validateTarget(filter::isValid);

        if (targetSelector.getCurrentTarget() != null && ElytraAura.getInstance().isState() && isElytraFlying()) {
            float elytraRange = ElytraAura.getInstance().getElytraSearchRange();
            if (targetSelector.getCurrentTarget().distanceTo(mc.player) > elytraRange) {
                return null;
            }

            return targetSelector.getCurrentTarget();
        }

        return targetSelector.getCurrentTarget();
    }

    private void rotateToTarget(StrikerConstructor.AttackPerpetratorConfigurable config) {
        StrikeManager attackHandler = Essence.getInstance().getAttackPerpetrator().getAttackHandler();
        TurnsConnection controller = TurnsConnection.INSTANCE;
        Turns.VecRotation rotation = new Turns.VecRotation(config.getAngle(), config.getAngle().toVector());
        TurnsConfig rotationConfig = getRotationConfig();

        if (ElytraTargetUtil.isElytraFlying() && shouldUseElytraMode()) {
            ElytraTargetUtil.rotateToTargetElytraUltraFast();
            return;
        }

        boolean shouldRotate = switch (aimMode.get()) {
            case "Snap" -> attackHandler.canAttack(config, 1) || !attackHandler.getAttackTimer().finished(70);
            case "ФанТайм" -> attackHandler.canAttack(config, 1) || !attackHandler.getAttackTimer().finished(15);
            case "СпукиТайм" -> true;
            case "РилиВорлд" -> true;
            case "Матрикс" -> true;
            case "ХолиВорлд" -> true;
              default -> false;
        };

        if (shouldRotate) {

            Turns currentAngle = controller.getRotation();
            Turns targetAngle = config.getAngle();
            Turns playerAngle = new Turns(mc.player.getYaw(), mc.player.getPitch());


            if (target != null) {

            }


            controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);

        }
    }

    public StrikerConstructor.AttackPerpetratorConfigurable getConfig() {
        float range = attackRange.getValue() + RANGE_MARGIN;
        Vec3d vec;
        Box hitbox = target.getBoundingBox();

        if (mc.player.isGliding() && ElytraAura.getInstance().state && (target.isGliding() ))
            vec = ElytraTargetUtil.getElytraResolvedPoint(target, false);
        else
            vec = pointFinder.computeVector(target, range, TurnsConnection.INSTANCE.getRotation(), getSmoothMode().randomValue(), attackSetting.isSelected("Игнорировать стены")).getLeft().subtract(mc.player.getEyePos());

        return new StrikerConstructor.AttackPerpetratorConfigurable(target, MathAngle.fromVec3d(vec), range, attackSetting.getSelected(), aimMode, target.getBoundingBox());
    }

    @EventHandler
    public void onRenderTargetPredict(TickEvent e) {
        if (target == null) {
//            TurnsConnection.INSTANCE.clear();
//
//            TurnsConnection.INSTANCE.setRotation(null);
        }

        if (ElytraAura.getInstance().state && target.isGliding()) {
            Vec3d motion = target.getVelocity();
            Vec3d shift = motion.multiply(ElytraAura.getInstance().elytraForward.getValue());
            Vec3d predictPos = target.getPos().add(0, target.getHeight() / 2.0, 0).add(shift);

            Render3D.drawShapeAlternative(new BlockPos((int) predictPos.x, (int) predictPos.y, (int) predictPos.z), VoxelShapes.fullCube(), new Color(255, 0, 0).getRGB(), 4, true, true);
        }
    }

    public TurnsConfig getRotationConfig() {
        boolean visibleCorrection = !correctionType.get().equals("Незаметная");
        boolean freeCorrection = !aimMode.get().equals("Legit") && correctionType.get().equals("Свободная");
        if (TargetStrafe.getInstance().isState() && TargetStrafe.getInstance().mode.isSelected("Grim") && target !=null) { freeCorrection = false; }
        return new TurnsConfig(getSmoothMode(), visibleCorrection, freeCorrection);
    }

    public RotateConstructor getSmoothMode() {
        if (ElytraTargetUtil.isElytraFlying() && shouldUseElytraMode()) {
            return new ElytraOptimizedMode();
        }
        return switch (aimMode.get()) {
            case "ФанТайм" -> new FTAngle();
            case "ХолиВорлд" -> new HWAngle();
            case "СпукиТайм" -> new SPAngle();
            case "РилиВорлд" -> new RWAngle();
            case "Snap" -> new SnapAngle();
            case "Матрикс" -> new MatrixAngle();
            default -> new LinearConstructor();
        };
    }
    private double limitDecimals(double value, int decimalPlaces) {
        return Math.round(value * Math.pow(10, decimalPlaces)) / Math.pow(10, decimalPlaces);
    }


}

