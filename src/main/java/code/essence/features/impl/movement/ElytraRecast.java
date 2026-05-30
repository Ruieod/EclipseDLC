package code.essence.features.impl.movement;

import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.events.player.TickEvent;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ElytraRecast extends Module {

    public ElytraRecast() {
        super("ElytraRecast", "ElytraRecast", ModuleCategory.MOVEMENT);
        setup();
    }

    
    private boolean shouldRecast() {
        if (mc.player == null) return false;

        ItemStack chestItem = mc.player.getEquippedStack(EquipmentSlot.CHEST);

        
        boolean elytraNotBroken = chestItem.isOf(Items.ELYTRA) 
                && (!chestItem.isDamageable() || (chestItem.getMaxDamage() - chestItem.getDamage() > 1));

        return !mc.player.getAbilities().flying 
                && !mc.player.hasVehicle() 
                && !mc.player.isClimbing() 
                && !mc.player.isTouchingWater() 
                && !mc.player.hasStatusEffect(StatusEffects.LEVITATION)
                && elytraNotBroken
                && mc.options.jumpKey.isPressed();
    }

    
    public boolean recastElytra() {
        if (shouldRecast()) {
            PlayerInteractionHelper.startFallFlying();
            return true;
        }
        return false;
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (PlayerInteractionHelper.nullCheck()) return;
        recastElytra();
    }
}
