package code.essence.features.impl.player;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.MultiSelectSetting;
import code.essence.events.block.PushEvent;
import code.essence.events.player.PlayerCollisionEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoPush extends Module {
    MultiSelectSetting ignoreSetting = new MultiSelectSetting("Игнорировать", "Разрешает выбранные вами действия")
            .value("Вода", "Коллизия блоков", "Коллизия энтити", "Рыхлый снег", "Сладкие ягоды");
    public NoPush() {
        super("NoPush", "NoPush", ModuleCategory.PLAYER);
        setup(ignoreSetting);
    }

    @EventHandler
    public void onPush(PushEvent e) {
        switch (e.getType()) {
            case PushEvent.Type.COLLISION -> e.setCancelled(ignoreSetting.isSelected("Коллизия энтити"));
            case PushEvent.Type.WATER -> e.setCancelled(ignoreSetting.isSelected("Вода"));
            case PushEvent.Type.BLOCK -> e.setCancelled(ignoreSetting.isSelected("Коллизия блоков"));
        }
    }

    @EventHandler
    
    public void onPlayerCollision(PlayerCollisionEvent e) {
        Block block = e.getBlock();
        if (block.equals(Blocks.POWDER_SNOW)) e.setCancelled(ignoreSetting.isSelected("Рыхлый снег"));
        else if (block.equals(Blocks.SWEET_BERRY_BUSH)) e.setCancelled(ignoreSetting.isSelected("Сладкие ягоды"));
    }
}
