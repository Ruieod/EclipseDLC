package code.essence.features.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import code.essence.common.repository.friend.FriendUtils;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.events.player.BoundingBoxControlEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HitBoxModule extends Module {
    SliderSettings xzExpandSetting = new SliderSettings("Расширение XZ", "Позволяет расширить хитбокс по осям XZ")
            .setValue(0.2F).range(0.0F, 3.0F);

    SliderSettings yExpandSetting = new SliderSettings("Расширение Y", "Позволяет расширить хитбокс по оси Y")
            .setValue(0.0F)
            .range(0.0F, 3.0F);

    public HitBoxModule() {
        super("HitBox", "HitBox", ModuleCategory.COMBAT);
        setup(xzExpandSetting, yExpandSetting);
    }

    @EventHandler
    public void onBoundingBoxControl(BoundingBoxControlEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            Box box = event.getBox();

            float xzExpand = xzExpandSetting.getValue();
            float yExpand = yExpandSetting.getValue();
            Box changedBox = new Box(box.minX - xzExpand / 2.0f, box.minY,
                    box.minZ - xzExpand / 2.0f, box.maxX + xzExpand / 2.0f,
                    box.maxY + yExpand, box.maxZ + xzExpand / 2.0f);

            if (living != mc.player && !FriendUtils.isFriend(living)) {
                event.setBox(changedBox);
            }
        }
    }
}
