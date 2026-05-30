package code.essence.features.impl.misc;

import code.essence.events.packet.PacketEvent;
import code.essence.events.player.TickEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.MultiSelectSetting;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.client.logs.Logger;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;

import static code.essence.utils.display.interfaces.QuickImports.mc;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DebugModule extends Module {
    MultiSelectSetting logs = new MultiSelectSetting("Логи", "Что логировать в консоль")
            .value("Взрывные зелья", "Эффекты сущностей")
            .selected("Взрывные зелья", "Эффекты сущностей");

    Set<Integer> seenPotionEntities = new HashSet<>();
    Set<Integer> seenAreaClouds = new HashSet<>();

    public DebugModule() {
        super("DebugModule", ModuleCategory.MISC);
        setup(logs);
    }

    private boolean logPotions() {
        return logs.isSelected("Взрывные зелья");
    }

    private boolean logEffects() {
        return logs.isSelected("Эффекты сущностей");
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.world == null) return;
        if (!logPotions()) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PotionEntity potion) {
                int id = potion.getId();
                if (!seenPotionEntities.add(id)) continue;

                ItemStack stack = potion.getStack();
                if (stack == null || stack.isEmpty()) continue;
                
                if (!stack.isOf(Items.SPLASH_POTION) && !stack.isOf(Items.LINGERING_POTION)) continue;

                String name = stack.getName().getString();
                String effects = describePotionEffects(stack);
                Logger.info("[DebugModule] PotionEntity #" + id + " @(" +
                        String.format("%.2f", entity.getX()) + ", " +
                        String.format("%.2f", entity.getY()) + ", " +
                        String.format("%.2f", entity.getZ()) + ") " +
                        name + (effects.isEmpty() ? "" : " | " + effects));
            } else if (entity instanceof AreaEffectCloudEntity cloud) {
                int id = cloud.getId();
                if (!seenAreaClouds.add(id)) continue;

                String effects = describeCloudEffects(cloud);
                Logger.info("[DebugModule] AreaEffectCloud #" + id + " @(" +
                        String.format("%.2f", entity.getX()) + ", " +
                        String.format("%.2f", entity.getY()) + ", " +
                        String.format("%.2f", entity.getZ()) + ") " +
                        (effects.isEmpty() ? "" : "| " + effects));
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (mc.world == null) return;
        if (!logEffects()) return;

        switch (e.getPacket()) {
            case EntityStatusEffectS2CPacket p -> {
                Entity entity = mc.world.getEntityById(p.getEntityId());
                String entityName = describeEntity(entity);
                String effectName = Text.translatable(p.getEffectId().value().getTranslationKey()).getString();
                int level = p.getAmplifier() + 1;
                int durationTicks = p.getDuration();

                Logger.info("[DebugModule] EFFECT_ADD " + entityName +
                        " -> " + effectName + " " + level +
                        " (" + formatDuration(durationTicks) + ")" +
                        " ambient=" + p.isAmbient() +
                        " particles=" + p.shouldShowParticles() +
                        " icon=" + p.shouldShowIcon());
            }
            case RemoveEntityStatusEffectS2CPacket p -> {
                Entity entity = mc.world.getEntityById(p.entityId());
                String entityName = describeEntity(entity);
                String effectName = Text.translatable(p.effect().value().getTranslationKey()).getString();
                Logger.info("[DebugModule] EFFECT_REMOVE " + entityName + " -> " + effectName);
            }
            default -> {
            }
        }
    }

    private String describePotionEffects(ItemStack stack) {
        PotionContentsComponent component = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (component == null) return "";

        StringBuilder sb = new StringBuilder();
        for (StatusEffectInstance eff : component.getEffects()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(describeEffect(eff));
        }
        return sb.toString();
    }

    private String describeCloudEffects(AreaEffectCloudEntity cloud) {
        
        
        PotionContentsComponent contents = getCloudPotionContents(cloud);
        if (contents == null) return "";

        StringBuilder sb = new StringBuilder();
        for (StatusEffectInstance eff : contents.getEffects()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(describeEffect(eff));
        }
        return sb.toString();
    }

    private PotionContentsComponent getCloudPotionContents(AreaEffectCloudEntity cloud) {
        Object value = tryInvokeNoArg(cloud, "getPotionContents");
        if (value == null) value = tryInvokeNoArg(cloud, "getPotionContentsComponent");
        if (value == null) value = tryInvokeNoArg(cloud, "getPotionContentsComponent"); 
        return value instanceof PotionContentsComponent pc ? pc : null;
    }

    private Object tryInvokeNoArg(Object target, String methodName) {
        try {
            var m = target.getClass().getMethod(methodName);
            return m.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String describeEffect(StatusEffectInstance eff) {
        String name = Text.translatable(eff.getEffectType().value().getTranslationKey()).getString();
        int lvl = eff.getAmplifier() + 1;
        return name + " " + lvl + " (" + formatDuration(eff.getDuration()) + ")";
    }

    private String describeEntity(Entity entity) {
        if (entity == null) return "unknown_entity";
        if (entity instanceof LivingEntity living) {
            return living.getName().getString() + "(id=" + living.getId() + ")";
        }
        return entity.getType().toString() + "(id=" + entity.getId() + ")";
    }

    private String formatDuration(int ticks) {
        if (ticks < 0) return "∞";
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds %= 60;
        return minutes > 0 ? (minutes + "m " + seconds + "s") : (seconds + "s");
    }
}


