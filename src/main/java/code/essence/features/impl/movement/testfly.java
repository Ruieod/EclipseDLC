/*
package code.essence.features.impl.movement;

import code.essence.features.impl.misc.ElytraHelper;
import code.essence.utils.client.text.TextHelper;
import code.essence.utils.interactions.inv.InventoryTask;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.math.time.TimerUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.client.chat.ChatMessage;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.events.player.MoveEvent;
import code.essence.events.player.TickEvent;
import code.essence.utils.client.Instance;
import code.essence.utils.math.time.StopWatch;
import org.jetbrains.annotations.Nullable;

import static code.essence.features.impl.misc.ElytraHelper.getChestPlateSlot;
import static code.essence.utils.interactions.inv.InventoryToolkit.getItemSlot;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class testfly extends Module {
    public static Fly getInstance() {
        return Instance.get(Fly.class);
    }

    SelectSetting mode = new SelectSetting("Режим", "Выберите режим полета")
            .value("Ванилла", "TP")
            .selected("Ванилла");

    SliderSettings cooldownSetting = new SliderSettings("Кулдаун", "Тиков между телепортами (режим Тест)")
            .setValue(5F).range(1F, 20F);

    @NonFinal
    StopWatch timer = new StopWatch();
    @NonFinal
    int teleportCooldown = 0;
    @NonFinal
    Vec3d LastTeleportPos = Vec3d.ZERO;

    static int ticks;
    private static int oldSlot;

    public testfly() {
        super("testfly", ModuleCategory.MOVEMENT);
        setup(cooldownSetting);
    }

    TimerUtil t = new TimerUtil();

    private void sendTPPacket(Vec3d pos) {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                pos.x, pos.y, pos.z, true, true));
    }
    @Override
    public void activate() {
        if (mode.isSelected("Ванилла")) {
            mc.player.getAbilities().allowFlying = true;
        }
        super.activate();
    }


   @Override
    public void deadctivate() {
       teleportCooldown = 0;
       lastTeleportPos = Vec3d.ZERO;
       if (mode.isSelected("Ванилла")) {
           mc.player.getAbilities().setFlySpeed(0.05f);
           if (mc.player.getAbilities().flying) {
               mc.player.getAbilities().allowFlying = true;
               mc.player.getAbilities().flying = true;
           } else {
               mc.player.getAbilities().allowFlying = false;
               mc.player.getAbilities().flying = false;
           }
       }
        super.deactivate();
   }
}
*/
