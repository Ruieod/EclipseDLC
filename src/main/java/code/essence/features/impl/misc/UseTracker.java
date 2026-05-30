package code.essence.features.impl.misc;

import code.essence.events.packet.PacketEvent;
import code.essence.events.player.TickEvent;
import code.essence.display.hud.Notifications;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.setting.implement.MultiSelectSetting;
import code.essence.utils.math.time.StopWatch;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import code.essence.utils.client.chat.ChatMessage;
import net.minecraft.text.Text;

import java.util.*;

import static code.essence.utils.display.interfaces.QuickImports.mc;

public class UseTracker extends Module {
    private final MultiSelectSetting notifyMode = new MultiSelectSetting("Трекать", "Какие уведомления показывать")
            .value("Снос тотема", "Полученные зелья", "Съеденный предмет")
            .selected("Снос тотема", "Полученные зелья", "Съеденный предмет");

    private final Map<UUID, Map<String, StatusEffectInstance>> playerEffects = new HashMap<>();
    private static final Map<UUID, Boolean> enchantedTotems = new HashMap<>();
    private final Map<UUID, ItemStack> activeUseItem = new HashMap<>();
    private final Map<UUID, Integer> useStartTick = new HashMap<>(); 
    private final StopWatch updateTimer = new StopWatch();
    private static final long UPDATE_INTERVAL_MS = 500L;

    public UseTracker() {
        super("UseTracker", ModuleCategory.MISC);
        setup(notifyMode);
        updateTimer.reset();
    }

    private boolean allowTotem() {
        return notifyMode.isSelected("Снос тотема");
    }

    private boolean allowPotions() {
        return notifyMode.isSelected("Полученные зелья");
    }

    private boolean allowConsume() {
        return notifyMode.isSelected("Съеденный предмет");
    }





    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;


        if (allowConsume()) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == null) continue;
                if (mc.player != null && player.getUuid().equals(mc.player.getUuid())) continue;

                UUID id = player.getUuid();
                boolean using = player.isUsingItem();

                if (using) {

                    if (!activeUseItem.containsKey(id)) {
                        activeUseItem.put(id, player.getActiveItem().copy());
                        useStartTick.put(id, player.age);
                    }
                } else {
                    ItemStack used = activeUseItem.remove(id);
                    Integer startTick = useStartTick.remove(id);
                    
                    if (used != null && !used.isEmpty() && startTick != null) {
                        UseAction action = used.getUseAction();
                        String verb = switch (action) {
                            case DRINK -> "выпил";
                            case EAT -> "съел";
                            default -> null;
                        };
                        
                        if (verb == null) continue;
                        

                        int useDuration = player.age - startTick;
                        int minDuration = 31;
                        
                        if (useDuration < minDuration) {
                            continue;
                        }
                        
                        String itemName = used.getName().getString().replaceAll("§.", "");
                        String effectsStr = getPotionEffectsStringWithDuration(used);
                        
                        String playerName = player.getName().getString();
                        String effectsPart = effectsStr.isEmpty() ? "" : " §8(§7" + effectsStr + "§8)";



                        String chatMsg = "§R" + playerName + "§7 " + verb + " §R" + itemName + effectsPart.replace("§8", "§7") + "§R";
                        ChatMessage.brandmessage(chatMsg);
                    }
                }
            }
        }

        if (!updateTimer.finished(UPDATE_INTERVAL_MS)) return;
        updateTimer.reset();


        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null) continue;
            if (mc.player != null && player.getUuid().equals(mc.player.getUuid())) continue;
            UUID playerId = player.getUuid();
            List<StatusEffectInstance> currentEffectsCopy = new ArrayList<>(player.getStatusEffects());
            Map<String, StatusEffectInstance> currentEffectsMap = new HashMap<>();
            
            for (StatusEffectInstance e : currentEffectsCopy) {
                String key = e.getEffectType().value().getTranslationKey() + ":" + e.getAmplifier();
                currentEffectsMap.put(key, e);
            }

            playerEffects.put(playerId, currentEffectsMap);
        }
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (mc.world == null) return;
        
        if (e.getPacket() instanceof EntityStatusS2CPacket statusPacket) {
            if (statusPacket.getStatus() == 35) {
                net.minecraft.entity.Entity entity = statusPacket.getEntity(mc.world);
                if (!(entity instanceof PlayerEntity player)) return;
                if (mc.player != null && player.getUuid().equals(mc.player.getUuid())) return;
                
                boolean isEnchanted = EnchantmentHelper.hasEnchantments(player.getOffHandStack()) ||
                                     EnchantmentHelper.hasEnchantments(player.getMainHandStack());
                enchantedTotems.put(player.getUuid(), isEnchanted);
                
                if (allowTotem()) {
                    String message = "§R" + player.getName().getString()
                            + "§7 потерял тотем бессмертия, зачарован: " + (isEnchanted ? "§a" : "§c") + "⬤§R";
                    ChatMessage.brandmessage(message);
                }
            }
        }
        
        if (e.getPacket() instanceof EntityStatusEffectS2CPacket effectPacket && allowPotions()) {
            net.minecraft.entity.Entity entity = mc.world.getEntityById(effectPacket.getEntityId());
            if (!(entity instanceof PlayerEntity player)) return;
            if (mc.player != null && player.getUuid().equals(mc.player.getUuid())) return;
            

            UUID playerId = player.getUuid();
            Map<String, StatusEffectInstance> currentEffectsMap = playerEffects.getOrDefault(playerId, new HashMap<>());
            

            List<StatusEffectInstance> currentEffectsCopy = new ArrayList<>(player.getStatusEffects());
            Map<String, StatusEffectInstance> updatedEffectsMap = new HashMap<>();
            for (StatusEffectInstance effect : currentEffectsCopy) {
                String key = effect.getEffectType().value().getTranslationKey() + ":" + effect.getAmplifier();
                updatedEffectsMap.put(key, effect);
            }
            

            String newEffectKey = effectPacket.getEffectId().value().getTranslationKey() + ":" + effectPacket.getAmplifier();
            StatusEffectInstance newEffect = new StatusEffectInstance(
                effectPacket.getEffectId(), 
                effectPacket.getDuration(), 
                effectPacket.getAmplifier(), 
                effectPacket.isAmbient(), 
                effectPacket.shouldShowParticles(), 
                effectPacket.shouldShowIcon()
            );
            updatedEffectsMap.put(newEffectKey, newEffect);
            

            List<SpecialPotionType> detectedPotions = new ArrayList<>();
            Set<String> newEffectKeys = Set.of(newEffectKey);
            
            create(updatedEffectsMap, newEffectKeys, detectedPotions, SpecialPotionType.KILLER,
                    "effect.minecraft.strength:3", "effect.minecraft.resistance:0");
            create(updatedEffectsMap, newEffectKeys, detectedPotions, SpecialPotionType.URINE,
                    "effect.minecraft.jump_boost:0", "effect.minecraft.speed:2");
            create(updatedEffectsMap, newEffectKeys, detectedPotions, SpecialPotionType.MEDIC,
                    "effect.minecraft.health_boost:2", "effect.minecraft.regeneration:2");
            create(updatedEffectsMap, newEffectKeys, detectedPotions, SpecialPotionType.BURP,
                    "effect.minecraft.blindness:0", "effect.minecraft.glowing:0", "effect.minecraft.hunger:9", 
                    "effect.minecraft.slowness:2", "effect.minecraft.wither:4");
            create(updatedEffectsMap, newEffectKeys, detectedPotions, SpecialPotionType.FLASH,
                    "effect.minecraft.blindness:0", "effect.minecraft.glowing:0");
            create(updatedEffectsMap, newEffectKeys, detectedPotions, SpecialPotionType.SULFURIC_ACID,
                    "effect.minecraft.poison:1", "effect.minecraft.slowness:3", "effect.minecraft.weakness:2", 
                    "effect.minecraft.wither:4");
            create(updatedEffectsMap, newEffectKeys, detectedPotions, SpecialPotionType.WINNER,
                    "effect.minecraft.health_boost:1", "effect.minecraft.invisibility:0", "effect.minecraft.regeneration:1", 
                    "effect.minecraft.resistance:0");
            

            if (!detectedPotions.isEmpty()) {
                for (SpecialPotionType type : detectedPotions) {
                    ItemStack potionStack = new ItemStack(Items.POTION);
                    potionStack.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal(type.getDisplayName()));

                    displayPotionNotificationFromEffect(player, potionStack, type.getEffects().stream()
                            .map(d -> new StatusEffectInstance(d.getEffect(), d.getDurationTicks(), d.getAmplifier(), false, false, true))
                            .collect(java.util.stream.Collectors.toList()));
                }
            } else {
                String localizedName = Text.translatable(effectPacket.getEffectId().value().getTranslationKey()).getString().replaceAll("§.", "");
                String duration = getPotionDurationString(newEffect, 1);
                int amplifier = effectPacket.getAmplifier();
                
                int level = Math.max(0, amplifier) + 1;

                ChatMessage.brandmessage("§R" + player.getName().getString() + "§7 получил §f" + localizedName + " " + level + "§R на §7" + duration + "§R");
            }
            
            playerEffects.put(playerId, updatedEffectsMap);
        }
    }

    private boolean create(Map<String, StatusEffectInstance> currentEffectsMap,
                           Set<String> newOrRefreshedKeys,
                           List<SpecialPotionType> detectedPotions,
                           SpecialPotionType type, String... comboKeys) {
        Set<String> comboSet = new HashSet<>(Arrays.asList(comboKeys));
        boolean allPresent = comboSet.stream().allMatch(currentEffectsMap::containsKey);
        boolean atLeastOneNew = comboSet.stream().anyMatch(newOrRefreshedKeys::contains);

        if (allPresent && atLeastOneNew) {
            detectedPotions.add(type);
            newOrRefreshedKeys.removeAll(comboSet);
            return true;
        }
        return false;
    }

    private void displayPotionNotificationFromEffect(PlayerEntity player, ItemStack itemStack,
                                                     List<StatusEffectInstance> appliedEffects) {

        ChatMessage.brandmessage("§R" + player.getName().getString() + "§7 получил " + "§R" + itemStack.getName().getString().replaceAll("§.", ""));
    }

    private String getPotionDurationString(StatusEffectInstance effect, int multiplier) {
        if (effect.isInfinite()) return "∞";
        int duration = effect.getDuration() / multiplier;
        int seconds = duration / 20;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return minutes > 0 ? minutes + " мин " + seconds + " сек" : seconds + " сек";
    }

    private String getPotionEffectsString(ItemStack stack) {
        PotionContentsComponent component = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (component == null) return "";
        
        StringBuilder sb = new StringBuilder();
        for (StatusEffectInstance eff : component.getEffects()) {
            if (!sb.isEmpty()) sb.append(", ");
            String name = Text.translatable(eff.getEffectType().value().getTranslationKey()).getString();
            int level = eff.getAmplifier() + 1;
            sb.append(name).append(" ").append(level);
        }
        return sb.toString();
    }

    private String getPotionEffectsStringWithDuration(ItemStack stack) {
        PotionContentsComponent component = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (component == null) return "";
        
        StringBuilder sb = new StringBuilder();
        for (StatusEffectInstance eff : component.getEffects()) {
            if (!sb.isEmpty()) sb.append("§8, §7");
            String name = Text.translatable(eff.getEffectType().value().getTranslationKey()).getString().replaceAll("§.", "");
            int amplifier = eff.getAmplifier();
            int level = Math.max(0, amplifier) + 1;
            String duration = getPotionDurationString(eff, 1);
            sb.append(name).append(" ").append(level).append("§R на §7").append(duration);
        }
        return sb.toString();
    }

    @Getter
    @RequiredArgsConstructor
    public enum SpecialPotionType {
        FLASH(16777215, "§6[★] §eВспышка", Arrays.asList(
                new PotionEffectData(StatusEffects.BLINDNESS, 20, 0),
                new PotionEffectData(StatusEffects.GLOWING, 4 * 60, 0)
        ), Arrays.asList(16580598, 16777205, 16777212, 16775167, 16318453, 16776959, 16252920, 16514303, 16775679)),

        KILLER(13369344, "§4[★] §cЗелье Киллера", Arrays.asList(
                new PotionEffectData(StatusEffects.RESISTANCE, 3 * 60, 0),
                new PotionEffectData(StatusEffects.STRENGTH, 90, 3)
        ), Arrays.asList(12582912, 15007744, 14024704)),

        BURP(16737792, "§c[★] §6Зелье Отрыжки", Arrays.asList(
                new PotionEffectData(StatusEffects.BLINDNESS, 10, 0),
                new PotionEffectData(StatusEffects.GLOWING, 3 * 60, 0),
                new PotionEffectData(StatusEffects.HUNGER, 90, 9),
                new PotionEffectData(StatusEffects.SLOWNESS, 3 * 60, 2),
                new PotionEffectData(StatusEffects.WITHER, 30, 4)
        ), Arrays.asList(16727040, 16733184, 16739072)),

        SULFURIC_ACID(10092339, "§2[★] §aСерная кислота", Arrays.asList(
                new PotionEffectData(StatusEffects.POISON, 50, 1),
                new PotionEffectData(StatusEffects.SLOWNESS, 90, 3),
                new PotionEffectData(StatusEffects.WEAKNESS, 90, 2),
                new PotionEffectData(StatusEffects.WITHER, 30, 4)
        ), Arrays.asList(9961472, 10223411, 10027007)),

        MEDIC(16711935, "§5[★] §dЗелье Медика", Arrays.asList(
                new PotionEffectData(StatusEffects.HEALTH_BOOST, 45, 2),
                new PotionEffectData(StatusEffects.REGENERATION, 45, 2)
        ), Arrays.asList(16716287, 16707583, 14680063)),

        WINNER(65280, "§2[★] §aЗелье Победителя", Arrays.asList(
                new PotionEffectData(StatusEffects.HEALTH_BOOST, 3 * 60, 1),
                new PotionEffectData(StatusEffects.INVISIBILITY, 15 * 60, 0),
                new PotionEffectData(StatusEffects.REGENERATION, 60, 1),
                new PotionEffectData(StatusEffects.RESISTANCE, 60, 0)
        ), Arrays.asList(59136, 57088, 63232, 65310, 65350)),

        URINE(65280, "§3[★] §bМоча Флеша", Arrays.asList(
                new PotionEffectData(StatusEffects.JUMP_BOOST, 2 * 60, 1),
                new PotionEffectData(StatusEffects.SPEED, 2 * 60, 2)
        ), Arrays.asList(65535));

        private final Integer baseColor;
        private final String displayName;
        private final List<PotionEffectData> effects;
        private final List<Integer> colorVariations;

        @Getter
        @RequiredArgsConstructor
        public static class PotionEffectData {
            private final RegistryEntry<StatusEffect> effect;
            private final Integer durationSeconds;
            private final Integer amplifier;

            public Integer getDurationTicks() {
                return durationSeconds * 20;
            }
        }
    }
}

