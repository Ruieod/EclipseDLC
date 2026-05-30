package code.essence.features.impl.render;

import code.essence.features.module.setting.implement.*;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.client.Instance;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.*;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.common.repository.friend.FriendUtils;
import code.essence.utils.display.render.geometry.Render2D;
import code.essence.events.render.WorldRenderEvent;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.display.render.geometry.Render3D;
import code.essence.utils.math.projection.Projection;
import code.essence.events.player.TickEvent;
import code.essence.events.render.DrawEvent;
import code.essence.events.render.WorldLoadEvent;
import com.mojang.blaze3d.systems.RenderSystem;

import java.awt.*;
import java.util.*;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Esp extends Module {
    public static Esp getInstance() {
        return Instance.get(Esp.class);
    }
    List<PlayerEntity> players = new ArrayList<>();

    public static MultiSelectSetting entityType = new MultiSelectSetting("Показывать", "Сущности, которые будут отображаться")
            .value("Игроков", "Мобов", "Животных", "Предметы", "Подставка для брони", "Друзья").selected("Игроков");
    public static RadioSetting boxType = new RadioSetting("Режим бокса", "Тип бокса",
            new String[]{"Обычный", "Корнер",  "3д"}, "3д");
    public BooleanSetting flatBoxOutline = new BooleanSetting("Контур", "Контур для плоских боксов").visible(() -> (boxType.get().equals("Корнер") || boxType.get().equals("Обычный")));

    static ColorSetting color = new ColorSetting("Цвет боксов", "Цвет кристаллов").value(new Color(255, 255, 255, 55).getRGB())
            .presets(new Color(0, 246, 255,255).getRGB(),
                    new Color(183, 1, 195,255).getRGB()
                    ,new Color(255, 60, 0,255).getRGB()
                    ,new Color(171, 253, 0,255).getRGB());

    public Esp() {
        super("Esp", "Esp", ModuleCategory.RENDER);
        setup(entityType, boxType, flatBoxOutline,  color);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        players.clear();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        players.clear();
        if (mc.world != null) {
            mc.world.getPlayers().stream()
                    .filter(player -> player != mc.player)
                    .filter(player -> player.getCustomName() == null || !player.getCustomName().getString().startsWith("Ghost_"))
                    .forEach(players::add);
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (mc.getEntityRenderDispatcher() == null || mc.getEntityRenderDispatcher().camera == null) return;
        if (!entityType.isSelected("Игроков") && !entityType.isSelected("Мобов") && !entityType.isSelected("Животных") && !entityType.isSelected("Предметы") && !entityType.isSelected("Подставка для брони") && !entityType.isSelected("Друзья")) return;
        if (!boxType.get().equals("3д")) return;
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
        
        for (PlayerEntity player : players) {
            if (player == null) continue;
            if (player.getCustomName() != null && player.getCustomName().getString().startsWith("Ghost_")) continue;
            boolean friend = FriendUtils.isFriend(player);
            if (!entityType.isSelected("Игроков") && !(entityType.isSelected("Друзья") && friend)) continue;
            double interpX = MathHelper.lerp(tickDelta, player.prevX, player.getX());
            double interpY = MathHelper.lerp(tickDelta, player.prevY, player.getY());
            double interpZ = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());
            Vec3d interpCenter = new Vec3d(interpX, interpY, interpZ);
            float distance = (float) mc.getEntityRenderDispatcher().camera.getPos().distanceTo(interpCenter);
            if (distance < 1) continue;
            int baseColor = friend ? ColorAssist.getFriendColor() : color.getColorWithAlpha();
            int alpha = (int) (color.getAlpha() * 255);
            int fillColor = (baseColor & 0x00FFFFFF) | (alpha << 24);
            int outlineColor = (baseColor & 0x00FFFFFF) | 0xFF000000;

            Box interpBox = player.getDimensions(player.getPose()).getBoxAt(interpX, interpY, interpZ);
            Render3D.drawBox(interpBox, fillColor, 2, true, true, false);
            Render3D.drawBox(interpBox, outlineColor, 2, true, true, false);
        }
        
        List<Entity> entities = PlayerInteractionHelper.streamEntities().toList();
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living && !(living instanceof PlayerEntity) && !(living instanceof ArmorStandEntity)) {
                boolean isMob = living instanceof MobEntity && !(living instanceof AnimalEntity);
                boolean isAnimal = living instanceof AnimalEntity;
                if ((isMob && !entityType.isSelected("Мобов")) || (isAnimal && !entityType.isSelected("Животных"))) continue;
                
                double interpX = MathHelper.lerp(tickDelta, living.prevX, living.getX());
                double interpY = MathHelper.lerp(tickDelta, living.prevY, living.getY());
                double interpZ = MathHelper.lerp(tickDelta, living.prevZ, living.getZ());
                Vec3d interpCenter = new Vec3d(interpX, interpY, interpZ);
                float distance = (float) mc.getEntityRenderDispatcher().camera.getPos().distanceTo(interpCenter);
                if (distance < 1) continue;
                
                int baseColor = color.getColorWithAlpha();
                int alpha = (int) (color.getAlpha() * 255);
                int fillColor = (baseColor & 0x00FFFFFF) | (alpha << 24);
                int outlineColor = (baseColor & 0x00FFFFFF) | 0xFF000000;
                
                Box interpBox = living.getDimensions(living.getPose()).getBoxAt(interpX, interpY, interpZ);
                Render3D.drawBox(interpBox, fillColor, 2, true, true, false);
                Render3D.drawBox(interpBox, outlineColor, 2, true, true, false);
            } else if (entity instanceof ItemEntity item && entityType.isSelected("Предметы")) {
                double interpX = MathHelper.lerp(tickDelta, item.prevX, item.getX());
                double interpY = MathHelper.lerp(tickDelta, item.prevY, item.getY());
                double interpZ = MathHelper.lerp(tickDelta, item.prevZ, item.getZ());
                Vec3d interpCenter = new Vec3d(interpX, interpY, interpZ);
                float distance = (float) mc.getEntityRenderDispatcher().camera.getPos().distanceTo(interpCenter);
                if (distance < 1) continue;
                
                int baseColor = color.getColorWithAlpha();
                int alpha = (int) (color.getAlpha() * 255);
                int fillColor = (baseColor & 0x00FFFFFF) | (alpha << 24);
                int outlineColor = (baseColor & 0x00FFFFFF) | 0xFF000000;
                
                Box interpBox = item.getBoundingBox();
                Render3D.drawBox(interpBox, fillColor, 2, true, true, false);
                Render3D.drawBox(interpBox, outlineColor, 2, true, true, false);
            } else if (entity instanceof ArmorStandEntity armorStand) {
                if (!entityType.isSelected("Подставка для брони")) continue;
                
                double interpX = MathHelper.lerp(tickDelta, armorStand.prevX, armorStand.getX());
                double interpY = MathHelper.lerp(tickDelta, armorStand.prevY, armorStand.getY());
                double interpZ = MathHelper.lerp(tickDelta, armorStand.prevZ, armorStand.getZ());
                Vec3d interpCenter = new Vec3d(interpX, interpY, interpZ);
                float distance = (float) mc.getEntityRenderDispatcher().camera.getPos().distanceTo(interpCenter);
                if (distance < 1) continue;
                
                int baseColor = color.getColorWithAlpha();
                int alpha = (int) (color.getAlpha() * 255);
                int fillColor = (baseColor & 0x00FFFFFF) | (alpha << 24);
                int outlineColor = (baseColor & 0x00FFFFFF) | 0xFF000000;
                
                Box interpBox = armorStand.getDimensions(armorStand.getPose()).getBoxAt(interpX, interpY, interpZ);
                Render3D.drawBox(interpBox, fillColor, 2, true, true, false);
                Render3D.drawBox(interpBox, outlineColor, 2, true, true, false);
            }
        }
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        MatrixStack matrix = e.getDrawContext().getMatrices();
        RenderSystem.disableDepthTest();
        matrix.push();
        matrix.translate(0, 0, -1000);
        
        if (entityType.isSelected("Игроков") || entityType.isSelected("Друзья")) {
            for (PlayerEntity player : players) {
                if (player == null) continue;
                if (player.getCustomName() != null && player.getCustomName().getString().startsWith("Ghost_")) continue;
                boolean friend = FriendUtils.isFriend(player);
                if (!entityType.isSelected("Игроков") && !(entityType.isSelected("Друзья") && friend)) continue;
                Vector4d vec4d = Projection.getVector4D(player);
                float distance = (float) mc.getEntityRenderDispatcher().camera.getPos().distanceTo(player.getBoundingBox().getCenter());
                if (distance < 1) continue;
                drawBox(friend, vec4d, player);
            }
        }
        
        List<Entity> entities = PlayerInteractionHelper.streamEntities().toList();
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living && !(living instanceof PlayerEntity) && !(living instanceof ArmorStandEntity)) {
                boolean isMob = living instanceof MobEntity && !(living instanceof AnimalEntity);
                boolean isAnimal = living instanceof AnimalEntity;
                
                if ((isMob && entityType.isSelected("Мобов")) || (isAnimal && entityType.isSelected("Животных"))) {
                    Vector4d vec4d = Projection.getVector4D(entity);
                    float distance = (float) mc.getEntityRenderDispatcher().camera.getPos().distanceTo(entity.getBoundingBox().getCenter());
                    if (distance < 1) continue;
                    drawBox(false, vec4d, living);
                }
            } else if (entity instanceof ItemEntity item && entityType.isSelected("Предметы")) {
                Vector4d vec4d = Projection.getVector4D(item);
                float distance = (float) mc.getEntityRenderDispatcher().camera.getPos().distanceTo(item.getBoundingBox().getCenter());
                if (distance < 1) continue;
                drawBox(false, vec4d, item);
            } else if (entity instanceof ArmorStandEntity armorStand) {
                if (!entityType.isSelected("Подставка для брони")) continue;
                
                Vector4d vec4d = Projection.getVector4D(armorStand);
                float distance = (float) mc.getEntityRenderDispatcher().camera.getPos().distanceTo(armorStand.getBoundingBox().getCenter());
                if (distance < 1) continue;
                drawBox(false, vec4d, armorStand);
            }
        }
        
        matrix.pop();
    }

    private void drawBox(boolean friend, Vector4d vec, Entity entity) {
        if (boxType.get().equals("3д")) {
            return;
        }
        int baseColor = friend ? ColorAssist.getFriendColor() : color.getColorWithAlpha();
        int alpha = (int) (color.getAlpha() * 255);
        int client = (baseColor & 0x00FFFFFF) | (alpha << 24);
        int black = ColorAssist.HALF_BLACK;
        float posX = (float) vec.x;
        float posY = (float) vec.y;
        float endPosX = (float) vec.z;
        float endPosY = (float) vec.w;
        float size = (endPosX - posX) / 3;
        if (boxType.get().equals("Корнер")) {
            Render2D.drawQuad(posX - 0.5F, posY - 0.5F, size, 0.5F, client);
            Render2D.drawQuad(posX - 0.5F, posY, 0.5F, size + 0.5F, client);
            Render2D.drawQuad(posX - 0.5F, endPosY - size - 0.5F, 0.5F, size, client);
            Render2D.drawQuad(posX - 0.5F, endPosY - 0.5F, size, 0.5F, client);
            Render2D.drawQuad(endPosX - size + 1, posY - 0.5F, size, 0.5F, client);
            Render2D.drawQuad(endPosX + 0.5F, posY, 0.5F, size + 0.5F, client);
            Render2D.drawQuad(endPosX + 0.5F, endPosY - size - 0.5F, 0.5F, size, client);
            Render2D.drawQuad(endPosX - size + 1, endPosY - 0.5F, size, 0.5F, client);
            if (flatBoxOutline.isValue()) {
                Render2D.drawQuad(posX - 1F, posY - 1, size + 1, 1.5F, black);
                Render2D.drawQuad(posX - 1F, posY + 0.5F, 1.5F, size + 0.5F, black);
                Render2D.drawQuad(posX - 1F, endPosY - size - 1, 1.5F, size, black);
                Render2D.drawQuad(posX - 1F, endPosY - 1, size + 1, 1.5F, black);
                Render2D.drawQuad(endPosX - size + 0.5F, posY - 1, size + 1, 1.5F, black);
                Render2D.drawQuad(endPosX, posY + 0.5F, 1.5F, size + 0.5F, black);
                Render2D.drawQuad(endPosX, endPosY - size - 1, 1.5F, size, black);
                Render2D.drawQuad(endPosX - size + 0.5F, endPosY - 1, size + 1, 1.5F, black);
            }
        } else if (boxType.get().equals("Обычный")) {
            if (flatBoxOutline.isValue()) {
                Render2D.drawQuad(posX - 1F, posY - 1F, endPosX - posX + 2F, 1.5F, black);
                Render2D.drawQuad(posX - 1F, posY - 1F, 1.5F, endPosY - posY + 2F, black);
                Render2D.drawQuad(posX - 1F, endPosY - 1F, endPosX - posX + 2F, 1.5F, black);
                Render2D.drawQuad(endPosX - 0.5F, posY - 1F, 1.5F, endPosY - posY + 2F, black);
            }
            Render2D.drawQuad(posX - 0.5F, posY - 0.5F, endPosX - posX + 1F, 0.5F, client);
            Render2D.drawQuad(posX - 0.5F, posY - 0.5F, 0.5F, endPosY - posY + 1F, client);
            Render2D.drawQuad(posX - 0.5F, endPosY - 0.5F, endPosX - posX + 1F, 0.5F, client);
            Render2D.drawQuad(endPosX, posY - 0.5F, 0.5F, endPosY - posY + 1F, client);
        }
    }

}