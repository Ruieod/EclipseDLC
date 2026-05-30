package code.essence.features.impl.misc.creeperFarm;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * @author nikitavodolaz
 * @since 07.02.2026
 */

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreeperFarmController {
    CreeperStorage creeperStorage = new CreeperStorage();

    public void onRotationUpdate() {

    }

    public void onTick() {

    }
}