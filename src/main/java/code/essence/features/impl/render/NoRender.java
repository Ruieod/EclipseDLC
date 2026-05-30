package code.essence.features.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.MultiSelectSetting;
import code.essence.utils.client.Instance;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.events.render.DrawEvent;
import code.essence.events.render.WorldRenderEvent;
import code.essence.utils.client.TitleHider;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import code.essence.utils.display.interfaces.QuickImports;

@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class NoRender extends Module {
    public static NoRender getInstance() {
        return Instance.get(NoRender.class);
    }

    public MultiSelectSetting modeSetting = new MultiSelectSetting("Элементы", "Выберите элементы для игнорирования")
            .value("Огонь", "Плохие эффекты", "Оверлей блоков", "Тряска экрана", 
                   "Анимация тотема", "Боссбар", "Тени", "Дым", "Виньетка", 
                   "Стрелы в игроке", "Свечение игроков", "Оверлей лавы", "Оверлей воды", 
                   "Погода", "Скорборд", "Сердца иссушения", "Пузырьки воздуха", "Арморстенды", "Друзья", 
                   "Тайтлы", "Растительность")
            .selected("Огонь", "Плохие эффекты", "Оверблей блоков");

    public NoRender() {
        super("NoRender","NoRender",ModuleCategory.RENDER);
        setup(modeSetting);
    }

    public boolean shouldHideTotem() {
        return isState() && modeSetting.isSelected("Анимация тотема");
    }

    public boolean shouldHideBossbar() {
        return isState() && modeSetting.isSelected("Боссбар");
    }

    public boolean shouldHideShadows() {
        return isState() && modeSetting.isSelected("Тени");
    }

    public boolean shouldHideSmoke() {
        return isState() && modeSetting.isSelected("Дым");
    }

    public boolean shouldHideVignette() {
        return isState() && modeSetting.isSelected("Виньетка");
    }

    public boolean shouldHideStuckArrows() {
        return isState() && modeSetting.isSelected("Стрелы в игроке");
    }

    public boolean shouldHidePlayerGlow() {
        return isState() && modeSetting.isSelected("Свечение игроков");
    }

    public boolean shouldHideLavaOverlay() {
        return isState() && modeSetting.isSelected("Оверлей лавы");
    }

    public boolean shouldHideWaterOverlay() {
        return isState() && modeSetting.isSelected("Оверлей воды");
    }

    public boolean shouldHideWeather() {
        return isState() && modeSetting.isSelected("Погода");
    }

    public boolean shouldHideScoreboard() {
        return isState() && modeSetting.isSelected("Скорборд");
    }

    public boolean shouldHideWitherHearts() {
        return isState() && modeSetting.isSelected("Сердца иссушения");
    }

    public boolean shouldHideAirBubbles() {
        return isState() && modeSetting.isSelected("Пузырьки воздуха");
    }

    public boolean shouldHideArmorStands() {
        return isState() && modeSetting.isSelected("Арморстенды");
    }

    public boolean shouldHideBadEffects() {
        return isState() && modeSetting.isSelected("Плохие эффекты");
    }

    public boolean shouldHideFriends() {
        return isState() && modeSetting.isSelected("Друзья");
    }

    public boolean shouldHideTitles() {
        return isState() && modeSetting.isSelected("Тайтлы");
    }

    public boolean shouldHideVegetation() {
        return isState() && modeSetting.isSelected("Растительность");
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (shouldHideTitles() && mc.inGameHud != null) {
            TitleHider.hideTitles(mc.inGameHud);
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {

        if (!shouldHideVegetation() || mc.world == null || mc.player == null) {
            return;
        }
        

    }
}
