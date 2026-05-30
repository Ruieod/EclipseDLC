package code.essence.features.impl.movement;


import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.events.player.TickEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SlimeBlock;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.util.math.BlockPos;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HighJump extends Module {
    SelectSetting modeSetting = new SelectSetting("Режим", "Режим прыжка")
            .value("Лодка", "Экран шалкера", "Буст слизи", "ФанТайм песок душ").selected("Лодка");
    SliderSettings timer2 = new SliderSettings("Скорость буста", "").setValue(2.4f).range(1F, 6F).visible(()->modeSetting.isSelected("Экран шалкера"));

    @NonFinal
    private boolean wasInShulkerScreen = false;
    @NonFinal
    private boolean wasOnSlimeBlock = false;
    public HighJump() {
        super("HighJump", "HighJump", ModuleCategory.MOVEMENT);
        setup(modeSetting,timer2);
    }

    @EventHandler
    
    private void tickEvent(TickEvent event) {
        if (modeSetting.isSelected("Экран шалкера")) {
            if (mc.currentScreen instanceof ShulkerBoxScreen) {
                mc.player.addVelocity(0, timer2.getValue(), 0);
            }

        }

        if (modeSetting.isSelected("ФанТайм песок душ") && mc.player.isTouchingWater() && !mc.player.isSubmergedInWater()) {
            mc.player.addVelocity(0, 0.56, 0);
        }

        if (modeSetting.isSelected("Лодка")) {
            if (mc.currentScreen instanceof ShulkerBoxScreen) {
                float yaw = (float) Math.toRadians(mc.player.getYaw());
                double x = -Math.sin(yaw) * 1.0;
                double z = Math.cos(yaw) * 1.0;
                mc.player.addVelocity(0, 1, 0);
                mc.player.setPos(mc.player.getX(), mc.player.getY() + 0.24, mc.player.getZ());
            }
            if (mc.currentScreen instanceof ShulkerBoxScreen) {
                wasInShulkerScreen = true;
            } else if (wasInShulkerScreen && mc.currentScreen == null && isNearShulkerBox()) {
                wasInShulkerScreen = false;
            }
        }


















        if (modeSetting.isSelected("Буст слизи")) {
            if (mc.player.isOnGround() && isOnSlimeBlock()) {
                wasOnSlimeBlock = true;
            } else if (wasOnSlimeBlock && !mc.player.isOnGround() && mc.player.getVelocity().getY() > 0) {
                mc.player.addVelocity(0, 1.35, 0);
                wasOnSlimeBlock = false;
            } else if (!isOnSlimeBlock()) {
                wasOnSlimeBlock = false;
            }
        }
    }

    private boolean isNearShulkerBox() {
        BlockPos playerPos = mc.player.getBlockPos();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isOnSlimeBlock() {
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos belowPos = playerPos.down();
        return mc.world.getBlockState(belowPos).getBlock() instanceof SlimeBlock;
    }
}