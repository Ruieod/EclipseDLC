package code.essence.features.impl.combat;


import code.essence.events.packet.PacketEvent;
import code.essence.events.render.DrawEvent;
import code.essence.features.impl.movement.GuiMove;
import code.essence.utils.interactions.inv.InventoryTask;
import code.essence.utils.interactions.inv.InventoryFlowManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.BooleanSetting;
import code.essence.features.module.setting.implement.MultiSelectSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.events.player.TickEvent;
import code.essence.utils.display.render.geometry.Render2D;
import code.essence.utils.math.script.Script;
import com.mojang.blaze3d.systems.RenderSystem;


import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoTotem extends Module {
    SliderSettings healthThreshold = new SliderSettings("Порог здоровья", "Минимальное здоровье для экипировки тотема")
            .setValue(4.5F).range(1F, 20F).step(0.5f);

    MultiSelectSetting settings = new MultiSelectSetting("Настройки", "Дополнительные условия для экипировки тотема")
            .value("Сохранять талики", "Возвращать предмет", "Не брать если шар", "Не брать если ешь")
            .selected("Сохранять талики", "Возвращать предмет");

    MultiSelectSetting modes = new MultiSelectSetting("Учитывать", "Дополнительные условия для экипировки тотема")
            .value("Здоровье на элитре", "Падение", "Золотые серца", "Кристалы", "Обсидиан", "Булава", "Динамит")
            .selected("Здоровье на элитре", "Падение", "Золотые серца", "Кристалы", "Обсидиан", "Булава");

    SliderSettings elytraHealth = new SliderSettings("Здоровье на элитре", "Здоровье для экипировки тотема при полёте на элитре")
            .setValue(8.5F).range(1F, 20F)
            .visible(() -> modes.isSelected("Здоровье на элитре"));

    SliderSettings crystalDistance = new SliderSettings("Дистанция до кристала", "Максимальная дистанция до кристалла для экипировки тотема")
            .setValue(4F).range(1F, 6F)
            .visible(() -> modes.isSelected("Кристалы"));

    SliderSettings obsidianDistance = new SliderSettings("Дистанция до обсидиана", "Максимальная дистанция до обсидиана для экипировки тотема")
            .setValue(4F).range(1F, 6F)
            .visible(() -> modes.isSelected("Обсидиан"));

    SliderSettings tntDistance = new SliderSettings("Дистанция до динамита", "Максимальная дистанция до динамита для экипировки тотема")
            .setValue(10F).range(1F, 50F)
            .visible(() -> modes.isSelected("Динамит"));

    BooleanSetting drawCounter = new BooleanSetting("Рендерить количество", "Отображать количество тотемов на экране")
            .setValue(false);

    final Script script = new Script();
    int oldSlot = -1;
    ItemStack backItemStack = ItemStack.EMPTY;
    boolean totemIsUsed = false;
    long lastTotemUseTime = 0;
    boolean returningItem = false;
    boolean swappingTotem = false;
    long lastSwapAttempt = 0;
    long lastSuccessfulSwap = 0;

    public AutoTotem() {
        super("AutoTotem", "AutoTotem", ModuleCategory.COMBAT);
        setup(healthThreshold, settings, modes, elytraHealth, crystalDistance, obsidianDistance, tntDistance, drawCounter);
    }

    @EventHandler

    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }

        script.update();

        float health = mc.player.getHealth();
        float effectiveHealth = health;

        if (modes.isSelected("Золотые серца")) {
            effectiveHealth += mc.player.getAbsorptionAmount();
        }

        boolean shouldSwap = effectiveHealth <= healthThreshold.getValue() ||
                (totemIsUsed && getTotemCount() > 0 && System.currentTimeMillis() - lastTotemUseTime >= 500);

        if (modes.isSelected("Здоровье на элитре") && mc.player.isGliding() && health <= elytraHealth.getValue()) {
            shouldSwap = true;
        }

        if (modes.isSelected("Падение") && mc.player.fallDistance > 10) {
            shouldSwap = true;
        }

        if (modes.isSelected("Кристалы")) {
            double dist = getClosestCrystalDistance();
            if (dist <= crystalDistance.getValue()) {
                if (settings.isSelected("Не брать если шар") && isHoldingSkull()) {
                    shouldSwap = effectiveHealth <= healthThreshold.getValue();
                } else {
                    shouldSwap = true;
                }
            }
        }

        if (modes.isSelected("Обсидиан")) {
            double dist = getClosestObsidianDistance();
            if (dist <= obsidianDistance.getValue()) {
                if (settings.isSelected("Не брать если шар") && isHoldingSkull()) {
                    shouldSwap = effectiveHealth <= healthThreshold.getValue();
                } else {
                    shouldSwap = true;
                }
            }
        }

        if (modes.isSelected("Динамит")) {
            double dist = getClosestTntDistance();
            if (dist <= tntDistance.getValue()) {
                shouldSwap = true;
            }
        }

        if (modes.isSelected("Булава") && checkForMaceInEnemyHand()) {
            shouldSwap = true;
        }

        if (modes.isSelected("Не брать если ешь") && mc.player.isUsingItem() &&
                mc.player.getActiveItem().contains(DataComponentTypes.FOOD)) {
            shouldSwap = false;
        }

        ItemStack offhandStack = mc.player.getOffHandStack();
        boolean isTotemInOffhand = isTotemInOffhand();
        boolean isEnchantedTotemInOffhand = offhandStack.getItem() == Items.TOTEM_OF_UNDYING &&
                EnchantmentHelper.hasEnchantments(offhandStack);

        boolean isTotemOnCooldown = isTotemInOffhand &&
                mc.player.getItemCooldownManager().isCoolingDown(Items.TOTEM_OF_UNDYING.getDefaultStack());

        if (isTotemOnCooldown && backItemStack != ItemStack.EMPTY && oldSlot != -1 &&
                settings.isSelected("Возвращать предмет") && !returningItem) {

            Slot returnSlot = InventoryTask.slots().filter(s -> s.id == oldSlot).findFirst().orElse(null);
            if (returnSlot != null && !returnSlot.getStack().isEmpty() &&
                    returnSlot.getStack().getItem() == backItemStack.getItem()) {
                if (InventoryFlowManager.script.isFinished()) {
                    returningItem = true;
                    InventoryTask.swapHand(returnSlot, Hand.OFF_HAND, GuiMove.mode.isSelected("РиллиВорлд") ? false : true, true);
                    backItemStack = ItemStack.EMPTY;
                    oldSlot = -1;
                    returningItem = false;
                    return;
                }
            } else {
                oldSlot = -1;
                backItemStack = ItemStack.EMPTY;
            }
        }

        boolean hasNormalTotem = isTotemInOffhand() && !isEnchantedTotemInOffhand;

        if (hasNormalTotem && !shouldSwap) {
            swappingTotem = false;
        }

        boolean totemWasUsed = totemIsUsed && !isTotemInOffhand() && System.currentTimeMillis() - lastTotemUseTime > 200;
        boolean canReturnAfterUse = totemWasUsed && mc.player.isOnGround() && mc.player.fallDistance < 6;
        boolean shouldReturn = (!shouldSwap && isTotemInOffhand()) || canReturnAfterUse;

        if (shouldReturn && oldSlot != -1 && settings.isSelected("Возвращать предмет")) {
            Slot returnSlot = InventoryTask.slots().filter(s -> s.id == oldSlot).findFirst().orElse(null);
            if (returnSlot != null && !returnSlot.getStack().isEmpty() &&
                    returnSlot.getStack().getItem() == backItemStack.getItem()) {
                if (InventoryFlowManager.script.isFinished() && !returningItem) {
                    returningItem = true;
                    InventoryTask.swapHand(returnSlot, Hand.OFF_HAND, GuiMove.mode.isSelected("РиллиВорлд") ? false : true, true);
                    backItemStack = ItemStack.EMPTY;
                    oldSlot = -1;
                    totemIsUsed = false;
                    returningItem = false;
                    return;
                }
            } else {
                oldSlot = -1;
                backItemStack = ItemStack.EMPTY;
            }
        } else if (returningItem && !shouldReturn) {
            returningItem = false;
        }

        if (shouldSwap && (!hasNormalTotem || isEnchantedTotemInOffhand)) {
            if (hasNormalTotem && !isEnchantedTotemInOffhand) {
                swappingTotem = false;
                return;
            }

            if (swappingTotem) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastSwapAttempt < 100 || currentTime - lastSuccessfulSwap < 200) {
                return;
            }

            if (!InventoryFlowManager.script.isFinished()) {
                return;
            }

            Slot totemSlot = findTotemSlot();
            if (totemSlot == null) {
                return;
            }

            if (mc.player.getItemCooldownManager().isCoolingDown(Items.TOTEM_OF_UNDYING.getDefaultStack())) {
                if (backItemStack != ItemStack.EMPTY && oldSlot != -1 &&
                        settings.isSelected("Возвращать предмет") && !returningItem) {
                    Slot returnSlot = InventoryTask.slots().filter(s -> s.id == oldSlot).findFirst().orElse(null);
                    if (returnSlot != null && !returnSlot.getStack().isEmpty() &&
                            returnSlot.getStack().getItem() == backItemStack.getItem()) {
                        if (InventoryFlowManager.script.isFinished()) {
                            returningItem = true;
                            InventoryTask.swapHand(returnSlot, Hand.OFF_HAND, GuiMove.mode.isSelected("РиллиВорлд") ? false : true, true);
                            backItemStack = ItemStack.EMPTY;
                            oldSlot = -1;
                            returningItem = false;
                            return;
                        }
                    } else {
                        oldSlot = -1;
                        backItemStack = ItemStack.EMPTY;
                    }
                }
                return;
            }

            if (isTotemInOffhand()) {
                ItemStack currentOffhand = mc.player.getOffHandStack();
                ItemStack foundTotem = totemSlot.getStack();

                if (currentOffhand.getItem() == foundTotem.getItem()) {
                    boolean currentEnchanted = EnchantmentHelper.hasEnchantments(currentOffhand);
                    boolean foundEnchanted = EnchantmentHelper.hasEnchantments(foundTotem);

                    if (currentEnchanted == foundEnchanted) {
                        return;
                    }

                    if (!currentEnchanted && foundEnchanted) {
                        return;
                    }
                }
            }

            lastSwapAttempt = currentTime;
            swappingTotem = true;

            if (!offhandStack.isEmpty() && oldSlot == -1 && settings.isSelected("Возвращать предмет")) {
                oldSlot = totemSlot.id;
                backItemStack = offhandStack.copy();
            }
            InventoryTask.swapHand(totemSlot, Hand.OFF_HAND, GuiMove.mode.isSelected("РиллиВорлд") ? false : true, true);
            totemIsUsed = false;

            script.cleanup().addTickStep(2, () -> {
                if (isTotemInOffhand() && !EnchantmentHelper.hasEnchantments(mc.player.getOffHandStack())) {
                    lastSuccessfulSwap = System.currentTimeMillis();
                }
                swappingTotem = false;
            });
        } else if (swappingTotem && hasNormalTotem) {
            swappingTotem = false;
        }
    }

    private boolean isHoldingSkull() {
        ItemStack mainHand = mc.player.getMainHandStack();
        ItemStack offHand = mc.player.getOffHandStack();

        return isSkull(mainHand) || isSkull(offHand);
    }

    private boolean isSkull(ItemStack stack) {
        if (stack.isEmpty()) return false;

        return stack.getItem() == Items.SKELETON_SKULL ||
                stack.getItem() == Items.WITHER_SKELETON_SKULL ||
                stack.getItem() == Items.ZOMBIE_HEAD ||
                stack.getItem() == Items.PLAYER_HEAD ||
                stack.getItem() == Items.CREEPER_HEAD ||
                stack.getItem() == Items.DRAGON_HEAD ||
                stack.getItem() == Items.PIGLIN_HEAD;
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof EntityStatusS2CPacket statusPacket) {
            if (statusPacket.getStatus() == 35 && statusPacket.getEntity(mc.world) == mc.player) {
                totemIsUsed = true;
                lastTotemUseTime = System.currentTimeMillis();
            }
        }
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (!drawCounter.isValue() || mc.player == null || mc.world == null || mc.player.isCreative()) return;
        
        DrawContext context = e.getDrawContext();
        MatrixStack matrix = context.getMatrices();
        
        float x = mc.getWindow().getScaledWidth() / 2.0f + 120f;
        float y = mc.getWindow().getScaledHeight() - 17;
        int totemX = (int)(x - 128);
        int totemY = (int)(y - 37);
        
        ItemStack totemStack = new ItemStack(Items.TOTEM_OF_UNDYING);
        
        RenderSystem.disableBlend();
        Render2D.defaultDrawStack(context, totemStack, totemX, totemY, false, false, 1.0f);
        RenderSystem.enableBlend();
        
        String count = String.valueOf(getTotemCount());
        int countWidth = mc.textRenderer.getWidth(count);
        matrix.push();
        matrix.translate(0.0F, 0.0F, 300.0F);
        context.drawText(mc.textRenderer, count,
                (int)(totemX + 16.5f - countWidth),
                totemY + 9,
                0xFFFFFF, true);
        matrix.pop();
    }

    private boolean checkForMaceInEnemyHand() {
        if (!modes.isSelected("Булава")) return false;
        Box box = mc.player.getBoundingBox().expand(30.0);
        List<AbstractClientPlayerEntity> players = mc.world.getEntitiesByClass(
                AbstractClientPlayerEntity.class, box, e -> true);
        for (AbstractClientPlayerEntity enemy : players) {
            if (enemy == mc.player) continue;
            ItemStack main = enemy.getMainHandStack();
            ItemStack off = enemy.getOffHandStack();
            if (main.getItem() == Items.MACE || off.getItem() == Items.MACE) {
                return true;
            }
        }
        return false;
    }

    private double getClosestCrystalDistance() {
        double minDist = Double.MAX_VALUE;
        Vec3d playerPos = mc.player.getPos();
        Box box = mc.player.getBoundingBox().expand(crystalDistance.getValue());
        List<EndCrystalEntity> crystals = mc.world.getEntitiesByClass(EndCrystalEntity.class, box, e -> true);
        for (EndCrystalEntity crystal : crystals) {
            double dist = playerPos.distanceTo(crystal.getPos());
            if (dist < minDist) {
                minDist = dist;
            }
        }
        return minDist;
    }

    private double getClosestObsidianDistance() {
        double minDist = Double.MAX_VALUE;
        BlockPos playerBlockPos = mc.player.getBlockPos();
        int dist = (int) Math.ceil(obsidianDistance.getValue());
        for (int x = -dist; x <= dist; x++) {
            for (int y = -dist; y <= dist; y++) {
                for (int z = -dist; z <= dist; z++) {
                    BlockPos pos = playerBlockPos.add(x, y, z);
                    if (mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN)) {
                        double d = MathHelper.sqrt((float) playerBlockPos.getSquaredDistance(pos));
                        if (d < minDist) {
                            minDist = d;
                        }
                    }
                }
            }
        }
        return minDist;
    }

    private double getClosestTntDistance() {
        double minDist = Double.MAX_VALUE;
        Vec3d playerPos = mc.player.getPos();
        Box box = mc.player.getBoundingBox().expand(tntDistance.getValue());
        List<TntEntity> tntEntities = mc.world.getEntitiesByClass(TntEntity.class, box, e -> true);
        for (TntEntity tnt : tntEntities) {
            double dist = playerPos.distanceTo(tnt.getPos());
            if (dist < minDist) {
                minDist = dist;
            }
        }
        return minDist;
    }

    private boolean isTotemInOffhand() {
        ItemStack offhandStack = mc.player.getOffHandStack();
        return offhandStack.getItem() == Items.TOTEM_OF_UNDYING;
    }

    private Slot findTotemSlot() {
        if (settings.isSelected("Сохранять талики")) {
            Slot nonEnchantedSlot = null;
            Slot enchantedSlot = null;
            for (Slot slot : InventoryTask.slots().toList()) {
                ItemStack stack = slot.getStack();
                if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                    if (!EnchantmentHelper.hasEnchantments(stack)) {
                        nonEnchantedSlot = slot;
                    } else {
                        enchantedSlot = slot;
                    }
                }
            }
            if (nonEnchantedSlot != null) {
                return nonEnchantedSlot;
            }
            if (enchantedSlot != null) {
                return enchantedSlot;
            }
        } else {
            return InventoryTask.getSlot(Items.TOTEM_OF_UNDYING);
        }
        return null;
    }

    private int getTotemCount() {
        return (int) mc.player.getInventory().main.stream()
                .filter(s -> s.getItem() == Items.TOTEM_OF_UNDYING)
                .count();
    }

    @Override
    public void deactivate() {
        oldSlot = -1;
        backItemStack = ItemStack.EMPTY;
        totemIsUsed = false;
        lastTotemUseTime = 0;
        returningItem = false;
        swappingTotem = false;
        lastSwapAttempt = 0;
        lastSuccessfulSwap = 0;
        script.cleanup();
        super.deactivate();
    }
}