package code.essence.features.impl.render;

import code.essence.features.module.setting.implement.ColorSetting;
import code.essence.features.module.setting.implement.RadioSetting;
import code.essence.utils.theme.ThemeManager;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.client.managers.event.types.EventType;
import code.essence.features.impl.combat.Aura;
import code.essence.features.impl.combat.TriggerBot;
import code.essence.utils.features.aura.striking.StrikeManager;
import code.essence.common.animation.Animation;
import code.essence.common.animation.Direction;
import code.essence.common.animation.implement.Decelerate;
import code.essence.common.animation.implement.OutBack;
import code.essence.events.player.RotationUpdateEvent;
import code.essence.events.render.WorldRenderEvent;
import code.essence.Essence;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.math.calc.CalcVector;
import code.essence.utils.client.Instance;
import code.essence.utils.features.aura.utils.MathAngle;
import code.essence.utils.features.aura.warp.Turns;
import code.essence.utils.math.time.StopWatch;
import code.essence.utils.display.render.geometry.Render3D;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector3f;
import org.joml.Vector4i;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import code.essence.features.module.setting.implement.SelectSetting;
import code.essence.features.module.setting.implement.BooleanSetting;
import code.essence.features.module.Module;
import code.essence.features.module.ModuleCategory;
import net.minecraft.util.Identifier;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetESP extends Module {

    public static TargetESP getInstance() {
        return Instance.get(TargetESP.class);
    }

    Animation esp_anim = new Decelerate().setMs(400).setValue(1);
    Animation crystal2FadeAnim = new OutBack().setMs(500).setValue(1f);

    public Animation getCrystal2FadeAnim() {
        return crystal2FadeAnim;
    }



    public BooleanSetting getCrystalFill() {
        return crystalFill;
    }

    static SelectSetting targetEspType = new SelectSetting("Режим", "Выбирает тип цели esp")
            .value("Квадрат", "Круг", "Призраки", "Кристаллы")
            .selected("Круг");

    SelectSetting cubeType = new SelectSetting("Режим квадрата", "Выбирает тип куба")
            .value("1", "2")
            .visible(() -> targetEspType.isSelected("Квадрат"));

    static SelectSetting ghostType = new SelectSetting("Режим призраков", "Выбирает тип призраков")
            .value("1", "2")
            .visible(() -> targetEspType.isSelected("Призраки"));

    static SelectSetting crystalType = new SelectSetting("Режим кристаллов", "Выбирает тип кристаллов")
            .value("1", "2", "3")
            .visible(() -> targetEspType.isSelected("Кристаллы"));

    static BooleanSetting crystalFill = new BooleanSetting("Заливка кристаллов", "Заливать кристаллы цветом (режим 2)")
            .visible(() -> targetEspType.isSelected("Кристаллы") && crystalType.isSelected("2"));

    public static BooleanSetting ghostMovement = new BooleanSetting("Движение призраков", "Показывать след движения призраков")
            .setValue(true)
            .visible(() -> targetEspType.isSelected("Призраки") && ghostType.isSelected("1"));

   public static RadioSetting colorMode = new RadioSetting("Режим цвета", "Выбор источника цвета",
            new String[]{"Клиентский", "Кастом"}, "Клиентский");
   
   public static ColorSetting maincolor = new ColorSetting("Цвет", "Цвет таргетесп")
            .value(new Color(255, 255, 255, 55).getRGB())
            .presets(new Color(0, 246, 255,255).getRGB(),
                    new Color(183, 1, 195,255).getRGB()
                    ,new Color(255, 60, 0,255).getRGB()
                    ,new Color(171, 253, 0,255).getRGB())
            .visible(() -> colorMode.get().equals("Кастом"));
   
   public static BooleanSetting redOnHit = new BooleanSetting("Краснеть при ударе", "ESP становится красным при ударе")
            .setValue(true);
   
   public static int getColor() {
       return colorMode.get().equals("Клиентский") ? ThemeManager.primaryColor.getColor() : maincolor.getColor();
   }

    public TargetESP() {
        super("TargetEsp", "TargetEsp", ModuleCategory.RENDER);
        setup(targetEspType, ghostType, crystalType, crystalFill, cubeType, colorMode, maincolor, redOnHit, ghostMovement);
    }

    @Override
    public void activate() {
        crystal2FadeAnim.setDirection(Direction.FORWARDS);
        super.activate();
    }

    @Override
    public void deactivate() {
        crystal2FadeAnim.setDirection(Direction.BACKWARDS);
        super.deactivate();
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (e.getType() == EventType.POST) {
            Render3D.updateTargetEsp();
        }
    }

    private static LivingEntity getCombatTarget() {
        if (Aura.getInstance().isState() && Aura.getInstance().getTarget() != null) {
            return Aura.getInstance().getTarget();
        }
        TriggerBot tb = TriggerBot.getInstance();
        if (tb != null && tb.isState() && tb.getTargetEsp().isValue() && tb.getTarget() != null) {
            return tb.getTarget();
        }
        return null;
    }

    private static LivingEntity getCombatLastTarget() {
        if (Aura.getInstance().isState() && Aura.getInstance().getLastTarget() != null) {
            return Aura.getInstance().getLastTarget();
        }
        TriggerBot tb = TriggerBot.getInstance();
        if (tb != null && tb.isState() && tb.getTargetEsp().isValue() && tb.getLastTarget() != null) {
            return tb.getLastTarget();
        }
        return Aura.getInstance().getLastTarget();
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        float tickDelta = e.getPartialTicks();

        LivingEntity target = getCombatTarget();
        if (target != null) {
            Vec3d currentPos = new Vec3d(
                MathHelper.lerp(tickDelta, target.prevX, target.getX()),
                MathHelper.lerp(tickDelta, target.prevY, target.getY()),
                MathHelper.lerp(tickDelta, target.prevZ, target.getZ())
            );
            long currentTime = System.currentTimeMillis();
            if (targetPositionHistory.isEmpty() || currentPos.distanceTo(targetPositionHistory.get(0).position) > 0.05) {
                targetPositionHistory.add(0, new PositionEntry(currentPos));
            }
            targetPositionHistory.removeIf(entry -> currentTime - entry.timestamp > 1000);
            while (targetPositionHistory.size() > 200) {
                targetPositionHistory.remove(targetPositionHistory.size() - 1);
            }
        } else {
            if (!targetPositionHistory.isEmpty()) {
                targetPositionHistory.clear();
            }
        }

        StrikeManager attackHandler = Essence.getInstance().getAttackPerpetrator().getAttackHandler();
        StopWatch attackTimer = attackHandler.getAttackTimer();

        LivingEntity lastTarget = getCombatLastTarget();
        esp_anim.setDirection(target != null ? Direction.FORWARDS : Direction.BACKWARDS);
        boolean combatActive = lastTarget != null && !esp_anim.isFinished(Direction.BACKWARDS);

        if (combatActive) {
            float anim = esp_anim.getOutput().floatValue();
            float red = MathHelper.clamp((lastTarget.hurtTime - tickCounter.getTickDelta(false)) / 20f, 0f, 1f);

            switch (targetEspType.getSelected()) {
                case "Квадрат" -> Render3D.drawCube(lastTarget, anim, red, cubeType.getSelected());
                case "Круг" -> Render3D.drawCircle(e.getStack(), lastTarget, anim, red);
                case "Призраки" -> {
                    if (ghostType.isSelected("1")) {
                        Render3D.drawGhosts(lastTarget, anim, red, 0.62F);
                    } else if (ghostType.isSelected("2")) {
                        Render3D.renderNurik(lastTarget, anim, red);
                    }
                }
                case "Кристаллы" -> {
                    String currentCrystalType = crystalType.getSelected();
                    if (crystalType.isSelected("2")) {
                        renderCrystalsBlumeStyle(e.getStack(), lastTarget, anim, red, tickDelta);
                    } else {
                        if (crystalList.isEmpty() || 
                            lastTarget != lastRenderedTarget || 
                            !currentCrystalType.equals(lastCrystalType)) {
                            if (crystalType.isSelected("1")) {
                                Render3D.renderCrystals(lastTarget, anim, red);
                            } else if (crystalType.isSelected("3")) {
                                createCrystalsHorizontal(lastTarget);
                            }
                            lastRenderedTarget = lastTarget;
                            lastCrystalType = currentCrystalType;
                        }
                        renderCrystals(e.getStack(), lastTarget, anim, red);
                    }
                }
            }
        }
    }

    private Entity lastRenderedTarget = null;
    private String lastCrystalType = null;
    private final List<Crystal> crystalList = new ArrayList<>();
    private float rotationAngle = 0;
    
    public static final CopyOnWriteArrayList<PositionEntry> targetPositionHistory = new CopyOnWriteArrayList<>();
    
    public static class PositionEntry {
        public final Vec3d position;
        public final long timestamp;
        
        public PositionEntry(Vec3d position) {
            this.position = position;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private static class Crystal {
        private final Entity entity;
        private final Vec3d position;
        private final Vec3d rotation;
        private final float size;
        private final float rotationSpeed;

        public Crystal(Entity entity, Vec3d position, Vec3d rotation) {
            this.entity = entity;
            this.position = position;
            this.rotation = rotation;
            this.size = 0.05f;
            this.rotationSpeed = 0.5f + (float)(Math.random() * 1.5f);
        }

        public void render(MatrixStack ms, float anim, float red, Camera camera) {
            ms.push();
            ms.translate(position.x, position.y, position.z);
            float pulsation = 1.0f + (float) (Math.sin(System.currentTimeMillis() / 500.0) * 0.1f);
            ms.scale(pulsation, pulsation, pulsation);
            float selfRotation = (System.currentTimeMillis() % 36000) / 100.0f * rotationSpeed;
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rotation.x));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) rotation.y + selfRotation));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) rotation.z));
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            int baseColor = (redOnHit.isValue() && red > 0) ? ColorAssist.red : getColor();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            drawCrystal(ms, baseColor, 0.2f, true, anim);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            drawCrystal(ms, baseColor, 0.3f, true, anim);
            drawCrystal(ms, baseColor, 0.8f, false, anim);
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            ms.push();
            ms.scale(1.2f, 1.2f, 1.2f);
            drawCrystal(ms, baseColor, 0.3f, true, anim);
            ms.pop();
            drawBloomSphere(ms, baseColor, anim, camera);
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            ms.pop();
        }

        private void drawBloomSphere(MatrixStack ms, int baseColor, float anim, Camera camera) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, Identifier.of("textures/bloom.png"));
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.depthMask(false);
            int bloomColor = ColorAssist.setAlpha(baseColor, (int) (0.4f * 25 * anim));
            float bloomSize = size * 13.0f;
            float pitch = camera.getPitch();
            float yaw = camera.getYaw();
            int segments = 4; 
            for (int i = 0; i < segments; i++) {
                ms.push();
                float angle = (360.0f / segments) * i;
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                Matrix4f matrix = ms.peek().getPositionMatrix();
                BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                bufferBuilder.vertex(matrix, -bloomSize / 2, -bloomSize / 2, 0).texture(0, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, -bloomSize / 2, 0).texture(1, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, bloomSize / 2, 0).texture(1, 0).color(bloomColor);
                bufferBuilder.vertex(matrix, -bloomSize / 2, bloomSize / 2, 0).texture(0, 0).color(bloomColor);
                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
                ms.pop();
            }
            for (int i = 0; i < segments; i++) {
                ms.push();
                float angle = (360.0f / segments) * i;
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                Matrix4f matrix = ms.peek().getPositionMatrix();
                BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                bufferBuilder.vertex(matrix, -bloomSize / 2, -bloomSize / 2, 0).texture(0, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, -bloomSize / 2, 0).texture(1, 1).color(bloomColor);
                bufferBuilder.vertex(matrix, bloomSize / 2, bloomSize / 2, 0).texture(1, 0).color(bloomColor);
                bufferBuilder.vertex(matrix, -bloomSize / 2, bloomSize / 2, 0).texture(0, 0).color(bloomColor);
                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
                ms.pop();
            }
            RenderSystem.depthMask(true);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        }

        private void drawCrystal(MatrixStack ms, int baseColor,  float alpha, boolean filled, float anim) {
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(
                    filled ? VertexFormat.DrawMode.TRIANGLES : VertexFormat.DrawMode.DEBUG_LINES,
                    VertexFormats.POSITION_COLOR
            );
            float s = size;
            float h_prism = size * 1f;
            float h_pyramid = size * 1.5f;
            int numSides = 8;
            List<Vec3d> topVertices = new ArrayList<>();
            List<Vec3d> bottomVertices = new ArrayList<>();
            for (int i = 0; i < numSides; i++) {
                float angle = (float) (2 * Math.PI * i / numSides);
                float x = (float) (s * Math.cos(angle));
                float z = (float) (s * Math.sin(angle));
                topVertices.add(new Vec3d(x, h_prism / 2, z));
                bottomVertices.add(new Vec3d(x, -h_prism / 2, z));
            }
            Vec3d vTop = new Vec3d(0, h_prism / 2 + h_pyramid, 0);
            Vec3d vBottom = new Vec3d(0, -h_prism / 2 - h_pyramid, 0);
            int finalColor = ColorAssist.setAlpha(baseColor, (int) (alpha * 255 * anim));
            for (int i = 0; i < numSides; i++) {
                Vec3d v1 = bottomVertices.get(i);
                Vec3d v2 = bottomVertices.get((i + 1) % numSides);
                Vec3d v3 = topVertices.get((i + 1) % numSides);
                Vec3d v4 = topVertices.get(i);
                drawQuad(ms, bufferBuilder, v1, v2, v3, v4, finalColor, filled);
            }
            for (int i = 0; i < numSides; i++) {
                Vec3d v1 = topVertices.get(i);
                Vec3d v2 = topVertices.get((i + 1) % numSides);
                drawTriangle(ms, bufferBuilder, vTop, v1, v2, finalColor, filled);
            }
            for (int i = 0; i < numSides; i++) {
                Vec3d v1 = bottomVertices.get(i);
                Vec3d v2 = bottomVertices.get((i + 1) % numSides);
                drawTriangle(ms, bufferBuilder, vBottom, v2, v1, finalColor, filled);
            }
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        }

        private void drawTriangle(MatrixStack ms, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, int color, boolean filled) {
            if (filled) {
                bb.vertex(ms.peek().getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z).color(color);
                bb.vertex(ms.peek().getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z).color(color);
            }
        }

        private void drawQuad(MatrixStack ms, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, Vec3d v4, int color, boolean filled) {
            if (filled) {
                drawTriangle(ms, bb, v1, v2, v3, color, true);
                drawTriangle(ms, bb, v1, v3, v4, color, true);
            } else {
                bb.vertex(ms.peek().getPositionMatrix(), 100, 100, (float)v1.z).color(color);
            }
        }
    }

    private void createCrystals(Entity target) {
        crystalList.clear();
        crystalList.add(new Crystal(target, new Vec3d(0, 0.85, 0.8), new Vec3d(-49, 0, 40)));
        crystalList.add(new Crystal(target, new Vec3d(0.2, 0.85, -0.675), new Vec3d(35, 0, -30)));
        crystalList.add(new Crystal(target, new Vec3d(0.6, 1.35, 0.6), new Vec3d(-30, 0, 35)));
        crystalList.add(new Crystal(target, new Vec3d(-0.74, 1.05, 0.4), new Vec3d(-25, 0, -30)));
        crystalList.add(new Crystal(target, new Vec3d(0.74, 0.95, -0.4), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.475, 0.85, -0.375), new Vec3d(30, 0, -25)));
        crystalList.add(new Crystal(target, new Vec3d(0, 1.35, -0.6), new Vec3d(45, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(0.85, 0.7, 0.1), new Vec3d(-30, 0, 30)));
        crystalList.add(new Crystal(target, new Vec3d(-0.7, 1.35, -0.3), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.3, 1.35, 0.55), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.5, 0.7, 0.7), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(0.5, 0.7, 0.7), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.7, 0.75, 0), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.2, 0.65, -0.7), new Vec3d(0, 0, 0)));
    }

    private void createCrystalsHorizontal(Entity target) {
        crystalList.clear();
        
        crystalList.add(new Crystal(target, new Vec3d(0, 0.85, 0.8), new Vec3d(90, 0, 40)));
        crystalList.add(new Crystal(target, new Vec3d(0.2, 0.85, -0.675), new Vec3d(90, 0, -30)));
        crystalList.add(new Crystal(target, new Vec3d(0.6, 1.35, 0.6), new Vec3d(90, 0, 35)));
        crystalList.add(new Crystal(target, new Vec3d(-0.74, 1.05, 0.4), new Vec3d(90, 0, -30)));
        crystalList.add(new Crystal(target, new Vec3d(0.74, 0.95, -0.4), new Vec3d(90, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.475, 0.85, -0.375), new Vec3d(90, 0, -25)));
        crystalList.add(new Crystal(target, new Vec3d(0, 1.35, -0.6), new Vec3d(90, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(0.85, 0.7, 0.1), new Vec3d(90, 0, 30)));
        crystalList.add(new Crystal(target, new Vec3d(-0.7, 1.35, -0.3), new Vec3d(90, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.3, 1.35, 0.55), new Vec3d(90, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.5, 0.7, 0.7), new Vec3d(90, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(0.5, 0.7, 0.7), new Vec3d(90, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.7, 0.75, 0), new Vec3d(90, 0, 0)));
        crystalList.add(new Crystal(target, new Vec3d(-0.2, 0.65, -0.7), new Vec3d(90, 0, 0)));
        
        
        
        crystalList.add(new Crystal(target, new Vec3d(0.6, 0.0, 0.5), new Vec3d(90, 0, 45)));
        crystalList.add(new Crystal(target, new Vec3d(-0.6, 0.0, 0.5), new Vec3d(90, 0, -45)));
        crystalList.add(new Crystal(target, new Vec3d(0.0, 0.25, 0.7), new Vec3d(90, 0, 0)));
        
        
        crystalList.add(new Crystal(target, new Vec3d(0.7, 0.4, 0.3), new Vec3d(90, 0, 60)));
        crystalList.add(new Crystal(target, new Vec3d(-0.7, 0.4, 0.3), new Vec3d(90, 0, -60)));
        crystalList.add(new Crystal(target, new Vec3d(0.0, 0.6, 0.8), new Vec3d(90, 0, 0)));
        
        
        crystalList.add(new Crystal(target, new Vec3d(0.8, 0.8, 0.2), new Vec3d(90, 0, 30)));
        crystalList.add(new Crystal(target, new Vec3d(-0.8, 0.8, 0.2), new Vec3d(90, 0, -30)));
        crystalList.add(new Crystal(target, new Vec3d(0.0, 1.0, 0.9), new Vec3d(90, 0, 0)));
        
        
        crystalList.add(new Crystal(target, new Vec3d(0.75, 1.25, 0.4), new Vec3d(90, 0, 50)));
        crystalList.add(new Crystal(target, new Vec3d(-0.75, 1.25, 0.4), new Vec3d(90, 0, -50)));
        crystalList.add(new Crystal(target, new Vec3d(0.0, 1.4, 0.85), new Vec3d(90, 0, 0)));
        
        
        crystalList.add(new Crystal(target, new Vec3d(0.6, 1.6, 0.5), new Vec3d(90, 0, 40)));
        crystalList.add(new Crystal(target, new Vec3d(-0.6, 1.6, 0.5), new Vec3d(90, 0, -40)));
        crystalList.add(new Crystal(target, new Vec3d(0.0, 1.7, 0.7), new Vec3d(90, 0, 0)));
    }

    private void renderCrystals(MatrixStack ms, Entity target, float anim, float red) {
        if (target == null || crystalList.isEmpty()) {
            return;
        }
        RenderSystem.enableDepthTest();
        Vec3d targetPos = CalcVector.lerpPosition(target);
        rotationAngle = (rotationAngle + 0.5f) % 360;
        ms.push();
        ms.translate(targetPos.x, targetPos.y, targetPos.z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationAngle));
        Camera camera = mc.gameRenderer.getCamera();
        for (Crystal crystal : crystalList) {
            crystal.render(ms, anim, red, camera);
        }
        ms.pop();
        RenderSystem.enableDepthTest();
    }

    // Кристаллы из TargetEspModule (режим "2" — орбитальные с заливкой)
    private static final Vector3f[] CRYSTAL_VERTICES = new Vector3f[]{
            new Vector3f(0.0f, 1.5f, 0.0f), new Vector3f(0.0f, -1.5f, 0.0f),
            new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f(-1.0f, 0.0f, 0.0f),
            new Vector3f(0.0f, 0.0f, 1.0f), new Vector3f(0.0f, 0.0f, -1.0f)
    };
    private static final int[][] CRYSTAL_FACES = new int[][]{
            {0, 2, 4}, {0, 4, 3}, {0, 3, 5}, {0, 5, 2},
            {1, 4, 2}, {1, 3, 4}, {1, 5, 3}, {1, 2, 5}
    };

    private void renderCrystalsBlumeStyle(MatrixStack matrixStack, LivingEntity living, float anim, float red, float tickDelta) {
        if (living == null) return;
        Vec3d vec3d = living.getLerpedPos(tickDelta);
        double time = System.nanoTime() / 1_000_000_000.0;
        double lengthX = living.getBoundingBox().getLengthX();
        double lengthZ = living.getBoundingBox().getLengthZ();
        int baseColor = (redOnHit.isValue() && red > 0) ? ColorAssist.red
                : (crystalFill.isValue() ? ColorAssist.clampSaturation(getColor(), 1f, 1.1f) : -1);
        baseColor = ColorAssist.multAlpha(baseColor, anim);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Camera camera = mc.gameRenderer.getCamera();

        for (int i = 0; i < 32; i++) {
            double cos = Math.cos(time + i) * (lengthX + 0.2f);
            double sin = Math.sin(time + i) * (lengthZ + 0.2f);
            Vec3d crystalPos = vec3d.add(cos, (i / 8f) % living.getHeight(), sin);
            Vec3d locationDelta = new Vec3d(vec3d.x - crystalPos.x, 0f, vec3d.z - crystalPos.z);
            Turns rotation = MathAngle.fromVec3d(locationDelta);
            Vec3d targetPos = crystalPos.subtract(rotation.toVector().multiply(1.0f - anim));

            matrixStack.push();
            matrixStack.translate(targetPos.x, targetPos.y, targetPos.z);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rotation.getYaw()));
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            matrixStack.scale(0.1f, 0.1f, 0.1f);
            org.joml.Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
            for (int[] face : CRYSTAL_FACES) {
                Vector3f v1 = CRYSTAL_VERTICES[face[0]];
                Vector3f v2 = CRYSTAL_VERTICES[face[1]];
                Vector3f v3 = CRYSTAL_VERTICES[face[2]];
                builder.vertex(matrix4f, v1.x, v1.y, v1.z).color(baseColor);
                builder.vertex(matrix4f, v2.x, v2.y, v2.z).color(baseColor);
                builder.vertex(matrix4f, v3.x, v3.y, v3.z).color(baseColor);
            }
            matrixStack.pop();

            MatrixStack glowMat = new MatrixStack();
            glowMat.push();
            glowMat.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            glowMat.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
            glowMat.translate(targetPos.x - camera.getPos().x, targetPos.y - camera.getPos().y, targetPos.z - camera.getPos().z);
            glowMat.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            glowMat.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            float glowSize = 0.2f;
            int glowColor = ColorAssist.multAlpha(getColor(), 0.3f * anim);
            Render3D.drawTexture(glowMat.peek(), Particles.ParticleType.shariki.texture(), -glowSize * 2, -glowSize * 2, glowSize * 4, glowSize * 4, new Vector4i(glowColor, glowColor, glowColor, glowColor), true);
            glowMat.pop();
        }

        BuiltBuffer builtBuffer = builder.endNullable();
        if (builtBuffer != null && builtBuffer.getDrawParameters().vertexCount() > 0) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
        }

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}