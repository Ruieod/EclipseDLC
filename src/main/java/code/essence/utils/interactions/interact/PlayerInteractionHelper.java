package code.essence.utils.interactions.interact;

import code.essence.utils.features.aura.warp.Turns;
import lombok.experimental.UtilityClass;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import org.lwjgl.glfw.GLFW;
import code.essence.features.module.setting.implement.BindSetting;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.client.packet.network.Network;
import code.essence.utils.features.aura.utils.MathAngle;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@UtilityClass
public class PlayerInteractionHelper implements QuickImports {
    public void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        mc.interactionManager.sendSequencedPacket(mc.world, packetCreator);
    }

    public void interactItem(Hand hand) {
        interactItem(hand, MathAngle.cameraAngle());
    }

    public void interactItem(Hand hand, Turns angle) {
        sendSequencedPacket(i -> new PlayerInteractItemC2SPacket(hand, i, angle.getYaw(), angle.getPitch()));
    }

    public void interactEntity(Entity entity) {
        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interactAt(entity, false, Hand.MAIN_HAND, entity.getBoundingBox().getCenter()));
        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(entity, false, Hand.MAIN_HAND));
    }

    public void startFallFlying() {
        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        mc.player.startGliding();
    }

    public void sendPacketWithOutEvent(Packet<?> packet) {
        mc.getNetworkHandler().getConnection().send(packet, null);
    }

    public void grimSuperBypass$$$(double y, Turns angle) {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY() + y, mc.player.getZ(), angle.getYaw(), angle.getPitch(), mc.player.isOnGround(), mc.player.horizontalCollision));
    }

    public String getHealthString(LivingEntity entity) {
        return getHealthString(getHealth(entity));
    }

    public String getHealthString(float hp) {
        return String.format("%.1f", hp).replace(",",".").replace(".0","");
    }

    public float getHealth(LivingEntity entity) {
        float vanillaHp = entity.getHealth() + entity.getAbsorptionAmount();
        float hp = vanillaHp;

        if (entity instanceof PlayerEntity player) switch (Network.server) {
            case "FunTime", "ReallyWorld", "GulPvP", "HolyWorld", "HopLite" -> {
                ScoreboardObjective scoreBoard = player.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
                if (scoreBoard != null) {
                    MutableText text2 = ReadableScoreboardScore.getFormattedScore(player.getScoreboard().getScore(player, scoreBoard), scoreBoard.getNumberFormatOr(StyledNumberFormat.EMPTY));
                    try {
                        float scoreboardHp = Float.parseFloat(ColorAssist.removeFormatting(text2.getString()));
                        
                        if (scoreboardHp > 0) {
                            hp = scoreboardHp;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        
        if (hp <= 0) {
            hp = vanillaHp;
        }

        
        float maxHp = switch (Network.server) {
            case "FunTime", "ReallyWorld", "GulPvP", "HolyWorld", "HopLite" -> 1000f;
            default -> entity.getMaxHealth();
        };

        return MathHelper.clamp(hp, 0, maxHp);
    }


    public void jump() {
        if (mc.player.isSprinting()) {
            float g = mc.player.getYaw() * ((float)Math.PI / 180F);
            mc.player.addVelocityInternal(new Vec3d(-MathHelper.sin(g) * 0.2F, 0.0F, MathHelper.cos(g) * 0.2F));
        }
        mc.player.velocityDirty = true;
    }

    public List<BlockPos> getCube(BlockPos center, float radius) {
        return getCube(center, radius,radius,true);
    }

    public List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY) {
        return getCube(center,radiusXZ,radiusY,true);
    }

    public List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY, boolean down) {
        List<BlockPos> positions = new ArrayList<>();
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();
        int posY = down ? centerY - (int) radiusY : centerY;

        for (int x = centerX - (int) radiusXZ; x <= centerX + radiusXZ; x++) {
            for (int z = centerZ - (int) radiusXZ; z <= centerZ + radiusXZ; z++) {
                for (int y = posY; y <= centerY + radiusY; y++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }

        return positions;
    }

    public List<BlockPos> getCube(BlockPos start, BlockPos end) {
        List<BlockPos> positions = new ArrayList<>();

        for (int x = start.getX(); x <= end.getX(); x++) {
            for (int z = start.getZ(); z <= end.getZ(); z++) {
                for (int y = start.getY(); y <= end.getY(); y++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }

        return positions;
    }

    public InputUtil.Type getKeyType(int key) {
        return key < 8 ? InputUtil.Type.MOUSE : InputUtil.Type.KEYSYM;
    }

    public Stream<Entity> streamEntities() {
        return StreamSupport.stream(mc.world.getEntities().spliterator(), false);
    }

    public boolean canChangeIntoPose(EntityPose pose, Vec3d pos) {
        return mc.player.getWorld().isSpaceEmpty(mc.player, mc.player.getDimensions(pose).getBoxAt(pos).contract(1.0E-7));
    }

    public boolean isPotionActive(RegistryEntry<StatusEffect> statusEffect) {
        return mc.player.getActiveStatusEffects().containsKey(statusEffect);
    }

    public boolean isPlayerInBlock(Block block) {
        return isBoxInBlock(mc.player.getBoundingBox().expand(-1e-3), block);
    }

    public boolean isBoxInBlock(Box box, Block block) {
        return isBox(box,pos -> mc.world.getBlockState(pos).getBlock().equals(block));
    }

    public boolean isBoxInBlocks(Box box, List<Block> blocks) {
        return isBox(box,pos -> blocks.contains(mc.world.getBlockState(pos).getBlock()));
    }

    public boolean isBox(Box box, Predicate<BlockPos> pos) {
        return BlockPos.stream(box).anyMatch(pos);
    }

    public boolean isKey(BindSetting setting) {
        int key = setting.getKey();
        return mc.currentScreen == null && setting.isVisible() && isKey(getKeyType(key), key);
    }

    public boolean isKey(KeyBinding key) {
        return isKey(key.getDefaultKey().getCategory(), key.getDefaultKey().getCode());
    }

    public boolean isKey(InputUtil.Type type, int keyCode) {
        if (keyCode != -1) switch (type) {
            case InputUtil.Type.KEYSYM: return GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == 1;
            case InputUtil.Type.MOUSE: return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), keyCode) == 1;
        }
        return false;
    }

    public boolean isAir(BlockPos blockPos) {
        return isAir(mc.world.getBlockState(blockPos));
    }

    public boolean isAir(BlockState state) {
        return state.isAir() || state.getBlock().equals(Blocks.CAVE_AIR) || state.getBlock().equals(Blocks.VOID_AIR);
    }

    public boolean isChat(Screen screen) {return screen instanceof ChatScreen;}
    public boolean nullCheck() {
        return mc.player == null || mc.world == null;
    }

    /**
     * Находит позицию "в воздухе" рядом с сундуком (для обхода проверки сервера "игрок в блоке").
     * Пробует блок перед сундуком (side), выше сундука и комбинации.
     */
    public Vec3d getSafePositionNearChest(BlockPos chest, Direction side) {
        if (mc.world == null) return null;
        BlockPos[] candidates = {
            chest.offset(side),
            chest.up(),
            chest.offset(side).up(),
            chest.up(2)
        };
        for (BlockPos p : candidates) {
            if (!mc.world.getBlockState(p).isSolid()) {
                return Vec3d.ofCenter(p);
            }
        }
        return null;
    }

    /**
     * Открывает сундук с обходом проверки "игрок в блоке" (только ChestStealer).
     * Отправляет серверу позицию в воздухе, затем interact. Реальную позицию не шлём —
     * сервер при проверке использует последнюю позицию (safe), следующий тик клиент шлёт свою сама.
     */
    public void interactBlockChestWithInBlockBypass(BlockPos chest, Vec3d center, Direction side) {
        Vec3d safe = getSafePositionNearChest(chest, side);
        if (safe != null) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                safe.x, safe.y, safe.z,
                mc.player.getYaw(), mc.player.getPitch(),
                mc.player.isOnGround(), false
            ));
        }
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(center, side, chest, false));
    }
}