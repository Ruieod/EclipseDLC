package code.essence.features.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.Hand;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.events.item.HandOffsetEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ViewModel extends Module {

    SliderSettings mainHandXSetting = new SliderSettings("Основная рука X", "Настройка значения X для основной руки")
            .setValue(0.0F).range(-1.0F, 1.0F);

    SliderSettings mainHandYSetting = new SliderSettings("Основная рука Y", "Настройка значения Y для основной руки")
            .setValue(0.0F).range(-1.0F, 1.0F);

    SliderSettings mainHandZSetting = new SliderSettings("Основная рука Z", "Настройка значения Z для основной руки")
            .setValue(0.0F).range(-2.5F, 2.5F);

    SliderSettings offHandXSetting = new SliderSettings("Второстепенная рука X", "Настройка значения X для второстепенной руки")
            .setValue(0.0F).range(-1.0F, 1.0F);

    SliderSettings offHandYSetting = new SliderSettings("Второстепенная рука Y", "Настройка значения Y для второстепенной руки")
            .setValue(0.0F).range(-1.0F, 1.0F);

    SliderSettings offHandZSetting = new SliderSettings("Второстепенная рука Z", "Настройка значения Z для второстепенной руки")
            .setValue(0.0F).range(-2.5F, 2.5F);

    SliderSettings mainHandScaleSetting = new SliderSettings("Размер основной руки", "Настройка размера предмета в основной руке")
            .setValue(1.0F).range(0.1F, 2.0F);

    SliderSettings offHandScaleSetting = new SliderSettings("Размер второй руки", "Настройка размера предмета во второстепенной руке")
            .setValue(1.0F).range(0.1F, 2.0F);

    public ViewModel() {
        super("ViewModel", "ViewModel", ModuleCategory.RENDER);
        setup(mainHandXSetting, mainHandYSetting, mainHandZSetting, offHandXSetting, offHandYSetting, offHandZSetting, mainHandScaleSetting, offHandScaleSetting);
    }

    @EventHandler
    public void onHandOffset(HandOffsetEvent e) {
        Hand hand = e.getHand();
        if (hand.equals(Hand.MAIN_HAND) && e.getStack().getItem() instanceof CrossbowItem) return;

        MatrixStack matrix = e.getMatrices();

        if (hand.equals(Hand.MAIN_HAND)) {
            matrix.translate(mainHandXSetting.getValue(), mainHandYSetting.getValue(), mainHandZSetting.getValue());
            float scale = mainHandScaleSetting.getValue();
            if (scale != 1.0F) {
                matrix.scale(scale, scale, scale);
            }
        } else {
            matrix.translate(offHandXSetting.getValue(), offHandYSetting.getValue(), offHandZSetting.getValue());
            float scale = offHandScaleSetting.getValue();
            if (scale != 1.0F) {
                matrix.scale(scale, scale, scale);
            }
        }
    }
}