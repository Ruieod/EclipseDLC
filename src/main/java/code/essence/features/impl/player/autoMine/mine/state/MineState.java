package code.essence.features.impl.player.autoMine.mine.state;

import code.essence.utils.display.interfaces.QuickImports;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;

/**
 * @author nikitavodolaz
 * @since 10.02.2026
 */

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MineState implements QuickImports {
    long duration;

    public void onTick() {
        duration = getMineDuration();
    }

    public static long getMineDuration() {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ArmorStandEntity armorStandEntity) {
                String string = armorStandEntity.getName().getString();

                if (string.contains("Обновление через")) {
                    int id = armorStandEntity.getId();

                    Entity nextArmorEntity = mc.world.getEntityById(id + 1);
                    if (nextArmorEntity != null) {
                        String duration = nextArmorEntity.getName().getString().replaceAll("[^0-9:]", "");
                        String[] mmss = duration.split(":");

                        if (mmss.length > 1) {
                            return Integer.parseInt(mmss[0]) * 60L + Integer.parseInt(mmss[1]);
                        } else {
                            return Integer.parseInt(mmss[0]);
                        }
                    }
                }
            }
        }

        return 0;
    }
}
