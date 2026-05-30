package code.essence.utils.interactions.item;

import code.essence.display.hud.CoolDowns;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.*;
import code.essence.utils.display.interfaces.QuickImports;

@UtilityClass
public class ItemTask implements QuickImports {

    public int maxUseTick(Item item) {
        return maxUseTick(item.getDefaultStack());
    }

    public int maxUseTick(ItemStack stack) {
        return switch (stack.getUseAction()) {
            case EAT, DRINK -> 32;
            case CROSSBOW, SPEAR -> 10;
            case BOW -> 20;
            case BLOCK -> 0;
            default -> stack.getMaxUseTime(mc.player);
        };
    }

    public float getCooldownProgress(Item item) {
        CoolDowns coolDowns = CoolDowns.getInstance();
        if (coolDowns != null) {
            return coolDowns.list.stream()
                    .filter(c -> c.item().equals(item))
                    .findFirst()
                    .map(coolDown -> {
                        
                        if (mc.player.getItemCooldownManager().isCoolingDown(item.getDefaultStack())) {
                            
                            return (float) (-coolDown.time().elapsedTime() / 1000.0);
                        }
                        return 0f;
                    })
                    .orElse(0f);
        }
        return 0;
    }
}
