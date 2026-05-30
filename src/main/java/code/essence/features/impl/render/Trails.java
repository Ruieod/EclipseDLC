package code.essence.features.impl.render;

import code.essence.common.repository.friend.FriendUtils;
import code.essence.events.render.WorldRenderEvent;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import code.essence.features.module.setting.implement.ColorSetting;
import code.essence.features.module.setting.implement.MultiSelectSetting;
import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.features.module.setting.implement.SliderSettings;
import code.essence.utils.client.Instance;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.display.color.ColorAssist;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class Trails extends Module {

    private final MultiSelectSetting targetsSetting = new MultiSelectSetting("Отрисовывать на", "trails.targets.desc")
            .value("Себе", "Друзьях", "Игроках")
            .selected("Себе", "Друзьях");

    private final SliderSettings lengthSetting = new SliderSettings("Длина", "trails.length.desc")
            .range(0.1f, 5.0f)
            .setValue(2.5f)
            .step(0.1f);

    private final SelectSetting colorMode = new SelectSetting("Режим цвета", "trails.color_mode.desc")
            .value("Клиентский", "Радужный", "Кастом")
            .selected("Клиентский");
    private final ColorSetting customColor = new ColorSetting("trails.custom_color", "trails.custom_color.desc")
            .value(ColorAssist.getClientColor())
            .visible(() -> colorMode.isSelected("Кастом"));

    private final Map<Integer, List<Trail>> trailsByEntity = new HashMap<>();

    public Trails() {
        super("Trails", "Trails", ModuleCategory.RENDER);
        setup(targetsSetting, lengthSetting, colorMode, customColor);
    }

    @Override
    public void deactivate() {
        trailsByEntity.clear();

        super.deactivate();
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (mc.world == null || mc.player == null) {
            return;
        }

        float partialTicks = event.getPartialTicks();
        float lengthValue = lengthSetting.getValue() * 1000;

        int themeColor = getTrailColor();

        List<TrailQuad> quadsToRender = new ArrayList<>();

        List<PlayerEntity> players = new ArrayList<>(mc.world.getPlayers());
        if (!players.contains(mc.player)) {
            players.add(mc.player);
        }

        for (Entity entity : players) {
            if (!isValidTarget(entity)) continue;
            if (entity == mc.player && mc.options.getPerspective().isFirstPerson()) continue;

            if (entity instanceof LivingEntity living) {
                double posX = living.prevX + (living.getX() - living.prevX) * partialTicks;
                double posY = living.prevY + (living.getY() - living.prevY) * partialTicks + 0.05 - (living.isGliding() ? 0.15 : 0);
                double posZ = living.prevZ + (living.getZ() - living.prevZ) * partialTicks;

                List<Trail> list = trailsByEntity.computeIfAbsent(living.getId(), id -> new ArrayList<>());
                list.add(new Trail(posX, posY, posZ));

                float height = living.getHeight() * (living.isInSneakingPose() ? 0.8f : 1.0f);

                Trail prevTrail = null;
                boolean isFirst = true;
                int prevColor = 0;

                Iterator<Trail> it = list.iterator();
                while (it.hasNext()) {
                    Trail trail = it.next();

                    if (System.currentTimeMillis() - trail.time > lengthValue) {
                        it.remove();
                        continue;
                    }

                    trail.alpha = Math.clamp((float) (System.currentTimeMillis() - trail.time) / lengthValue, 0.0f, 1.0f) * 255.0f;
                    int color = applyOpacity(themeColor, (int) ((255 * (255 - Math.round(trail.alpha))) / 220.0F));

                    if (!isFirst && prevTrail != null) {
                        quadsToRender.add(new TrailQuad(
                                prevTrail.x, prevTrail.y, prevTrail.z,
                                prevTrail.x, prevTrail.y + height, prevTrail.z,
                                trail.x, trail.y + height, trail.z,
                                trail.x, trail.y, trail.z,
                                prevColor, color
                        ));
                    }

                    isFirst = false;
                    prevTrail = trail;
                    prevColor = color;
                }

                if (list.isEmpty()) {
                    trailsByEntity.remove(living.getId());
                }
            }
        }

        if (!quadsToRender.isEmpty()) {
            var entry = event.getStack().peek();
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.disableCull();
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);

            var tessellator = net.minecraft.client.render.Tessellator.getInstance();
            var buffer = tessellator.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_COLOR);

            for (TrailQuad q : quadsToRender) {
                buffer.vertex(entry.getPositionMatrix(), (float) q.x0, (float) q.y0, (float) q.z0).color(q.color1);
                buffer.vertex(entry.getPositionMatrix(), (float) q.x1, (float) q.y1, (float) q.z1).color(q.color1);
                buffer.vertex(entry.getPositionMatrix(), (float) q.x2, (float) q.y2, (float) q.z2).color(q.color2);
                buffer.vertex(entry.getPositionMatrix(), (float) q.x3, (float) q.y3, (float) q.z3).color(q.color2);
            }

            net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buffer.end());

            com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            com.mojang.blaze3d.systems.RenderSystem.enableCull();
        }
    }

    private record TrailQuad(double x0, double y0, double z0, double x1, double y1, double z1,
                             double x2, double y2, double z2, double x3, double y3, double z3,
                             int color1, int color2) {}

    private boolean isValidTarget(Entity entity) {
        if (!entity.isAlive()) return false;
        if (entity.isInvisible()) return false;

        if (entity instanceof PlayerEntity player) {
            if (entity == mc.player) {
                return targetsSetting.isSelected("Себе");
            }

            if (FriendUtils.isFriend(player)) {
                return targetsSetting.isSelected("Друзьях");
            }

            return targetsSetting.isSelected("Игроках");
        }

        return false;
    }

    private int getTrailColor() {
        return switch (colorMode.getSelected()) {
            case "Клиентский" -> ColorAssist.getClientColor();
            case "Радужный" -> ColorAssist.rainbow(10, (int) (System.currentTimeMillis() / 50), 0.5f, 1f, 1f);
            default -> customColor.getColor();
        };
    }

    private int applyOpacity(int color, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    public static Trails getInstance() {
        return Instance.get(Trails.class);
    }

    private static final class Trail {
        final double x, y, z;
        float alpha;
        final long time;

        public Trail(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = System.currentTimeMillis();
        }
    }
}
