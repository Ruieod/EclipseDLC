package code.essence.features.impl.misc;


import code.essence.utils.features.aura.rotations.impl.SPAngle;
import code.essence.utils.features.aura.warp.Turns;
import code.essence.utils.features.aura.warp.TurnsConfig;
import code.essence.utils.features.aura.warp.TurnsConnection;
import code.essence.utils.interactions.inv.InventoryTask;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.math.task.TaskPriority;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.BooleanSetting;
import code.essence.features.module.setting.implement.MultiSelectSetting;
import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.math.time.StopWatch;
import code.essence.events.player.TickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChestStealer extends Module {
    StopWatch stopWatch = new StopWatch();
    StopWatch openDelayWatch = new StopWatch();
    Map<String, Item> itemMap = new HashMap<>();



    SPAngle spAngle = new SPAngle();
    SelectSetting modeSetting = new SelectSetting("Тип", "Выбирает тип стила")
            .value("ФанТайм", "Избранные предметы", "Обычный");
    SliderSettings delaySetting = new SliderSettings("Задержка", "Задержка между кликами по слоту")
            .setValue(100).range(0, 1000).visible(() -> {
                String mode = modeSetting.getSelected();
                return mode.equals("Избранные предметы") || mode.equals("Обычный");
            });
    BooleanSetting autoOpen = new BooleanSetting("Авто открытие", "Наводится на сундук и открывает его сам")
            .visible(() -> modeSetting.getSelected().equals("Обычный"));

    MultiSelectSetting itemSettings = new MultiSelectSetting("Предметы", "Выберите предметы, которые вор будет подбирать")
            .value("Голова игрока", "Тотем бессмертия", "Элитры", "Незеритовый меч", "Незеритовый шлем", "Незеритовый нагрудник", "Незеритовые поножи", "Незеритовые ботинки", "Незеритовый слиток", "Незеритовый лом")
            .visible(() -> modeSetting.getSelected().equals("Избранные предметы"));

    public ChestStealer() {
        super("ChestStealer", "ChestStealer", ModuleCategory.MISC);
        setup(modeSetting, delaySetting, autoOpen, itemSettings);
        initializeItemMap();
    }

    private void initializeItemMap() {
        itemMap.put("Голова игрока", Items.PLAYER_HEAD);
        itemMap.put("Тотем бессмертия", Items.TOTEM_OF_UNDYING);
        itemMap.put("Элитры", Items.ELYTRA);
        itemMap.put("Незеритовый меч", Items.NETHERITE_SWORD);
        itemMap.put("Незеритовый шлем", Items.NETHERITE_HELMET);
        itemMap.put("Незеритовый нагрудник", Items.NETHERITE_CHESTPLATE);
        itemMap.put("Незеритовые поножи", Items.NETHERITE_LEGGINGS);
        itemMap.put("Незеритовые ботинки", Items.NETHERITE_BOOTS);
        itemMap.put("Незеритовый слиток", Items.NETHERITE_INGOT);
        itemMap.put("Незеритовый лом", Items.NETHERITE_PICKAXE);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        String mode = modeSetting.getSelected();
        switch (mode) {
            case "ФанТайм" -> {
                if (mc.currentScreen instanceof GenericContainerScreen sh && sh.getTitle().getString().toLowerCase().contains("мистический") && !mc.player.getItemCooldownManager().isCoolingDown(Items.GUNPOWDER.getDefaultStack())) {
                    sh.getScreenHandler().slots.stream().filter(s -> s.hasStack() && !s.inventory.equals(mc.player.getInventory()) && stopWatch.every(150))
                            .forEach(s -> InventoryTask.clickSlot(s, 0, SlotActionType.QUICK_MOVE, true));
                }
            }
            case "Избранные предметы", "Обычный" -> {
                if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler sh) sh.slots.forEach(s -> {
                    boolean isDefaultMode = mode.equals("Обычный");
                    if (s.hasStack() && !s.inventory.equals(mc.player.getInventory()) && (isDefaultMode || whiteList(s.getStack().getItem())) && stopWatch.every(delaySetting.getValue())) {
                        InventoryTask.clickSlot(s, 0, SlotActionType.QUICK_MOVE, true);
                    }
                });

                if (mode.equals("Обычный") && autoOpen.isValue() && mc.currentScreen == null) {

                    BlockPos nearestChest = findNearestChest();
                    if (nearestChest != null) {
                        double distanceSq = mc.player.squaredDistanceTo(Vec3d.ofCenter(nearestChest));
                        if (distanceSq <= 200.25 && openDelayWatch.finished(200)) {
                            Turns currentAngle = TurnsConnection.INSTANCE.getCurrentAngle();
                            Turns baseAngle = currentAngle != null ? currentAngle : new Turns(mc.player.getYaw(), mc.player.getPitch());
                            float[] rot = rotations(nearestChest);
                            Turns targetAngle = new Turns(rot[0], rot[1]);
                            TurnsConnection.INSTANCE.rotateTo(spAngle.limitAngleChange(baseAngle, targetAngle, Vec3d.ofCenter(nearestChest), null), TurnsConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_3, this);

                            if (isAimedAtChest(nearestChest, 12f)) {
                                Vec3d center = Vec3d.ofCenter(nearestChest).add(0.2, 0.375, 0.2);
                                Direction side = Direction.getFacing(center.x - mc.player.getX(), center.y - mc.player.getY(), center.z - mc.player.getZ()).getOpposite();

                                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(center, side, nearestChest, false));
                                    mc.player.swingHand(Hand.MAIN_HAND);
                                    openDelayWatch.reset();
                                }
                            }
                        }
                    }
                }
            }
        }



    private boolean isAimedAtChest(BlockPos chest, float maxAngleDeg) {
        float[] targetRot = rotations(chest);
        Turns current = TurnsConnection.INSTANCE.getCurrentAngle();
        float yaw = current != null ? current.getYaw() : mc.player.getYaw();
        float pitch = current != null ? current.getPitch() : mc.player.getPitch();
        float dYaw = Math.abs(MathHelper.wrapDegrees(yaw - targetRot[0]));
        float dPitch = Math.abs(pitch - targetRot[1]);
        return dYaw <= maxAngleDeg && dPitch <= maxAngleDeg;
    }
    @Override
    public void deactivate() {
        TurnsConnection.INSTANCE.clear();
        TurnsConnection.INSTANCE.setRotation(null);
        super.deactivate();
    }

    private float[] rotations(BlockPos pos) {
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d entityPos = Vec3d.ofCenter(pos);

        double x = entityPos.x - playerPos.x;
        double y = entityPos.y - (playerPos.y + 0.5);
        double z = entityPos.z - playerPos.z;

        double distanceXZ = Math.sqrt(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * (180 / Math.PI)) - 90.0F;
        float pitch = (float) (-(Math.atan2(y, distanceXZ) * (180 / Math.PI)));
        return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.clamp(pitch, -90f, 90f)};
    }

    private BlockPos findNearestChest() {
        BlockPos playerPos = mc.player.getBlockPos();
        int range = 6;
        BlockPos.Mutable nearest = new BlockPos.Mutable(0, 0, 0);
        double nearestDistSq = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.iterate(playerPos.add(-range, -range, -range), playerPos.add(range, range, range))) {
            var state = mc.world.getBlockState(pos);
            if (state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST) ||state.isOf(Blocks.ENDER_CHEST) ) {
                double distSq = mc.player.squaredDistanceTo(Vec3d.ofCenter(pos));
                if (distSq < nearestDistSq && distSq <= 20.25) {
                    nearestDistSq = distSq;
                    nearest.set(pos);
                }
            }
        }
        return nearestDistSq < Double.MAX_VALUE ? nearest.toImmutable() : null;
    }

    private boolean whiteList(Item item) {
        List<String> selected = itemSettings.getSelected();
        if (selected == null || selected.isEmpty()) {
            return false;
        }
        
        for (String selectedName : selected) {
            Item mappedItem = itemMap.get(selectedName);
            if (mappedItem != null && mappedItem.equals(item)) {
                return true;
            }
        }
        
        if (mc.world != null && mc.player != null) {
            ItemStack stack = item.getDefaultStack();
            String itemNameLower = stack.getName().getString().toLowerCase();
            
            for (String selectedName : selected) {
                String selectedLower = selectedName.toLowerCase();
                if (itemNameLower.contains(selectedLower) || selectedLower.contains(itemNameLower)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
