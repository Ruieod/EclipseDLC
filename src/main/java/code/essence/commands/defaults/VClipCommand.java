package code.essence.commands.defaults;

import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import code.essence.utils.client.managers.api.command.Command;
import code.essence.utils.client.managers.api.command.argument.IArgConsumer;
import code.essence.utils.client.managers.api.command.exception.CommandException;
import code.essence.utils.client.managers.api.command.helpers.TabCompleteHelper;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class VClipCommand extends Command {

    public VClipCommand() {
        super("vclip", "vc");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMin(1);
        args.requireMax(1);
        
        if (mc.player == null || mc.world == null) {
            logDirect(Formatting.RED + "Игрок не загружен");
            return;
        }

        BlockPos playerPos = mc.player.getBlockPos();
        float yOffset;

        String direction = args.getString().toLowerCase();

        switch (direction) {
            case "up" -> yOffset = findOffset(playerPos, true);
            case "down" -> yOffset = findOffset(playerPos, false);
            default -> yOffset = parseOffset(direction);
        }

        if (yOffset != 0) {
            int elytraSlot = getElytraSlot();
            boolean hasElytra = elytraSlot != -1 && elytraSlot != -2;

            if (hasElytra) {
                switchElytra(elytraSlot, true);
            }

            teleport(yOffset, hasElytra);

            if (hasElytra) {
                switchElytra(elytraSlot, false);
            }
        } else {
            logDirect(Formatting.RED + "Не удалось выполнить телепортацию.");
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper()
                    .prepend("up", "down")
                    .filterPrefix(args.peekString())
                    .stream();
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Телепортация вверх/вниз по вертикали";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Телепортирует игрока вверх или вниз по вертикали.",
                "",
                "Использование:",
                "> vclip up - Телепортация вверх до ближайшего свободного пространства",
                "> vclip down - Телепортация вниз до ближайшего свободного пространства",
                "> vclip <distance> - Телепортация на указанное расстояние",
                "",
                "Примеры:",
                "> vclip 10 - Телепортация на 10 блоков вверх",
                "> vclip -5 - Телепортация на 5 блоков вниз"
        );
    }

    private float findOffset(BlockPos playerPos, boolean toUp) {
        int startY = toUp ? 3 : -1;
        int endY = toUp ? 255 : -255;
        int step = toUp ? 1 : -1;

        for (int i = startY; i != endY; i += step) {
            BlockPos targetPos = playerPos.add(0, i, 0);
            if (mc.world.getBlockState(targetPos).isAir()) {
                return i + (toUp ? 1 : -1);
            }
            if (mc.world.getBlockState(targetPos).isOf(Blocks.BEDROCK) && !toUp) {
                logDirect(Formatting.RED + "Тут нельзя телепортироваться под землю.");
                return 0;
            }
        }
        return 0;
    }

    private void teleport(float yOffset, boolean elytra) {
        if (elytra) {
            // С элитрой - используем эксплоит fall flying
            for (int i = 0; i < 2; i++) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, false));
            }
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    mc.player.getX(), mc.player.getY() + yOffset, mc.player.getZ(), false, false));
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.setPosition(mc.player.getX(), mc.player.getY() + yOffset, mc.player.getZ());
            
            String blockUnit = getBlockUnit(yOffset);
            logDirect(Formatting.GRAY + "Попытка телепортироваться с элитрой...");
            logDirect(String.format("Вы были успешно телепортированы на %.1f %s по вертикали", yOffset, blockUnit));
            return;
        }

        // Без элитры - отправляем много пакетов для обхода античита
        int packetsCount = calculatePacketsCount(yOffset);
        for (int i = 0; i < packetsCount; i++) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(mc.player.isOnGround(), false));
        }
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), mc.player.getY() + yOffset, mc.player.getZ(), false, false));
        mc.player.setPosition(mc.player.getX(), mc.player.getY() + yOffset, mc.player.getZ());
        
        String blockUnit = getBlockUnit(yOffset);
        logDirect(String.format("Вы были успешно телепортированы на %.1f %s по вертикали", yOffset, blockUnit));
    }

    private String getBlockUnit(float offset) {
        float absOffset = Math.abs(offset);
        if (absOffset == 1) return "блок";
        if (absOffset >= 2 && absOffset <= 4) return "блока";
        return "блоков";
    }

    private float parseOffset(String distance) {
        try {
            return Float.parseFloat(distance);
        } catch (NumberFormatException e) {
            logDirect(Formatting.RED + distance + Formatting.GRAY + " не является числом!");
            return 0;
        }
    }

    private int calculatePacketsCount(float yOffset) {
        return Math.max((int) (Math.abs(yOffset) / 10), 3);
    }

    private void switchElytra(int elytraSlot, boolean equip) {
        final int chestplateSlot = 6;
        if (equip) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, chestplateSlot, 0, SlotActionType.PICKUP, mc.player);
        } else {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, chestplateSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
        }
    }

    private int getElytraSlot() {
        // Проверяем надета ли элитра
        ItemStack chestStack = mc.player.getInventory().getArmorStack(2);
        if (chestStack != null && chestStack.isOf(Items.ELYTRA)) {
            return -2; // Элитра уже надета
        }

        // Ищем элитру в инвентаре
        int slot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.ELYTRA)) {
                slot = i;
                break;
            }
        }

        // Конвертируем слот для GUI
        if (slot != -1 && slot < 9) {
            slot = slot + 36;
        }
        return slot;
    }
}
