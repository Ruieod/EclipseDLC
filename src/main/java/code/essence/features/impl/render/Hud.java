package code.essence.features.impl.render;

import code.essence.features.module.setting.implement.BooleanSetting;
import code.essence.features.module.setting.implement.SelectSetting;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.features.module.setting.implement.MultiSelectSetting;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.client.Instance;

import java.awt.*;

public class  Hud extends Module {
    public static Hud getInstance() {
        return Instance.get(Hud.class);
    }

    public static MultiSelectSetting interfaceSettings = new MultiSelectSetting("Элементы", "Настройка элементов интерфейса")
            .value("Watermark", "Keybinds", "Potions", "Staff list", "Target Hud", "Cooldowns", "Inventory", "Info", "Notifications", "Armor", "Binds")
            .selected("Watermark", "Keybinds", "Potions", "Staff list", "Target Hud",  "Cooldowns", "Inventory", "Info", "Notifications", "Armor", "Binds");

    public MultiSelectSetting notificationSettings = new MultiSelectSetting("Уведомлять об", "Выберите, когда будут появляться уведомления")
            .value("Переключение модулей", "Входе админа", "Выходе админа", "Поднятии предмета", "Ломании щита", "Полученном зелье", "Заканчивающемся зелье")
            .selected("Переключение модулей", "Поднятии предмета", "Ломании щита").visible(()-> interfaceSettings.isSelected("Notifications"));


    public static SelectSetting targetHudMode = new SelectSetting("Режим Target Hud", "Выбор варианта Target Hud")
            .value("1", "2")
            .selected("2")
            .visible(() -> interfaceSettings.isSelected("Target Hud"));

    public static BooleanSetting blur = new BooleanSetting("Блюр", "Выберите состояние блюра").setValue(true);

    public static BooleanSetting hidecoords = new BooleanSetting("Скрывать координаты", "").setValue(true).visible(()-> interfaceSettings.isSelected("Notifications"));




    public Hud() {
        super("Hud", ModuleCategory.RENDER);
        setup( interfaceSettings, notificationSettings,targetHudMode, hidecoords, blur);
    }
}