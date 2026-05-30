
        package code.essence.display.hud;
import code.essence.common.animation.Animation;
import code.essence.common.animation.implement.Decelerate;
import code.essence.utils.display.render.post.KawaseBlur;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import code.essence.utils.math.time.StopWatch;
import code.essence.utils.theme.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import code.essence.utils.client.managers.api.draggable.AbstractDraggable;
import code.essence.features.impl.combat.Aura;
import code.essence.features.impl.render.Hud;
import code.essence.common.animation.Direction;
import code.essence.common.animation.implement.EaseOut;
import code.essence.utils.display.render.font.FontRenderer;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.Essence;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.display.render.geometry.Render2D;
import code.essence.utils.display.scissor.ScissorAssist;
import code.essence.utils.client.packet.network.Network;
import com.mojang.blaze3d.systems.RenderSystem;
import com.google.common.base.Suppliers;
import code.essence.utils.display.atlasfont.msdf.MsdfFont;
import code.essence.utils.display.render.systemrender.builders.Builder;
import org.joml.Matrix4f;
import java.awt.*;
        import java.util.function.Supplier;

public class TargetHud2 extends AbstractDraggable {
    private static final Supplier<MsdfFont> MEDIUM_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("suisseintlmedium").data("suisseintlmedium").build());

    private final Animation faceAlphaAnimation = new Decelerate().setMs(125).setValue(1);
    private final StopWatch stopWatch = new StopWatch();
    private LivingEntity lastTarget;
    private float health;
    private float absorption;
    private float previousWidth = 100f;
    private boolean wasActive = false;
    private boolean initialized = false;

    public TargetHud2() {
        super("Target Hud", 10, 80, 100, 36, true);
        this.scaleAnimation = new EaseOut().setValue(1).setMs(150);
        if (getX() < 0) {
            setX(10);
        }
    }

    @Override
    public boolean visible() {
        if (Hud.getInstance() == null) return false;
        boolean isTargetHudSelected = Hud.getInstance().interfaceSettings.isSelected("Target Hud");
        boolean isHudEnabled = Hud.getInstance().state;
        boolean isVariant2 = Hud.getInstance().targetHudMode.isSelected("2");
        boolean isActive = isTargetHudSelected && isHudEnabled && isVariant2;
        return isActive && scaleAnimation.isDirection(Direction.FORWARDS);
    }

    @Override
    public void tick() {
        if (!Hud.getInstance().interfaceSettings.isSelected("Target Hud") || !Hud.getInstance().state
                || !Hud.getInstance().targetHudMode.isSelected("2")) {
            return;
        }

        LivingEntity auraTarget = Aura.getInstance().getTarget();
        if (auraTarget != null) {
            lastTarget = auraTarget;
            startAnimation();
            faceAlphaAnimation.setDirection(Direction.FORWARDS);
        } else if (PlayerInteractionHelper.isChat(mc.currentScreen) && mc.player != null) {
            lastTarget = mc.player;
            startAnimation();
            faceAlphaAnimation.setDirection(Direction.FORWARDS);
        } else if (stopWatch.finished(500)) {
            stopAnimation();
            faceAlphaAnimation.setDirection(Direction.BACKWARDS);
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        boolean isActive = Hud.getInstance().interfaceSettings.isSelected("Target Hud") && Hud.getInstance().state
                && Hud.getInstance().targetHudMode.isSelected("2");

        if (!initialized) {
            previousWidth = getWidth();
            initialized = true;
        }

        if (isActive && !wasActive) {
            previousWidth = getWidth();
        }
        wasActive = isActive;

        if (isActive && lastTarget != null) {
            MatrixStack matrix = context.getMatrices();
            drawMain(context, matrix, tickCounter.getTickDelta(false));
            drawFace(context);
        }
    }

    private void drawMain(DrawContext context, MatrixStack matrix, float delta) {
        RenderSystem.disableDepthTest();
        matrix.push();
        matrix.translate(0, 0, -1000);

        FontRenderer nameFont = Fonts.getSize(17, Fonts.Type.SuisseIntlMedium);
        float hp = PlayerInteractionHelper.getHealth(lastTarget);
        String stringHp = (lastTarget.isInvisible() && !Network.isSpookyTime() && !Network.isCopyTime()) ? " ??" : PlayerInteractionHelper.getHealthString(lastTarget);
        health = MathHelper.clamp(Calculate.interpolateSmooth(1, health, hp / lastTarget.getMaxHealth() * 360), 0, 360);
        float absorptionAmount = lastTarget.getAbsorptionAmount();
        absorption = MathHelper.clamp(Calculate.interpolateSmooth(1, absorption, absorptionAmount / 20.0F * 360), 0, 360);

        float nameWidth = nameFont.getStringWidth(lastTarget.getName().getString());
        float hpTextSize = 7f;
        float hpTextWidth = MEDIUM_FONT.get().getWidth(stringHp + "hp", hpTextSize);

        float scrollStartX = getX() + 39;
        float nameEndX = scrollStartX + nameWidth;
        float paddingAfterName = 5f;
        float baseHpBarWidth = 64.5f;
        float rightPadding = 1;
        float hpBarStartX = getX() + 36f;

        float itemsEndX = getX() + 36 + 6 * 11f;
        float itemsEndRelative = itemsEndX - getX();
        float minWidth = Math.max(itemsEndRelative, 36 + baseHpBarWidth) + rightPadding + 1;

        float nameEndRelative = nameEndX - getX();
        float hpBarEndRelative = 36 + baseHpBarWidth;
        float requiredWidth;
        if (nameEndX > hpBarStartX + baseHpBarWidth) {
            float extendedHpBarEnd = nameEndRelative + paddingAfterName;
            requiredWidth = extendedHpBarEnd + rightPadding + 1;
        } else {
            requiredWidth = hpBarEndRelative + rightPadding + 1;
        }

        float baseWidth = Math.max(requiredWidth, minWidth);


        float widthDifference = baseWidth - previousWidth;
        if (Math.abs(widthDifference) > 0.1f) {
            int newX = (int) (getX() - widthDifference / 2f);
            if (newX < 0) {
                newX = 0;
            }
            setX(newX);
            previousWidth = baseWidth;
        }

        if (getX() < 0) {
            setX(10);
            previousWidth = baseWidth;
        }

        setWidth((int) baseWidth);
        setHeight((int) 41);

        float hpBarY = getY() + 12f;
        float hpBarHeight = 9f;
        float hpBarWidth = baseHpBarWidth;

        if (nameEndX > hpBarStartX + baseHpBarWidth) {
            float maxHpBarEndX = getX() + getWidth() - 1 - rightPadding;
            float calculatedWidth = Math.min(maxHpBarEndX - hpBarStartX, nameEndX + paddingAfterName - hpBarStartX);
            if (calculatedWidth > baseHpBarWidth) {
                hpBarWidth = calculatedWidth;
            }
        }

        float backgroundWidth;
        if (hpBarWidth > baseHpBarWidth) {
            float hpBarEndX = hpBarStartX + hpBarWidth;
            backgroundWidth = hpBarEndX - getX() + rightPadding + 1.5f;
        } else {
            backgroundWidth = baseWidth - 1;
        }

        if (Hud.blur.isValue()) {
            Render2D.rectangleWithMask(matrix.peek().getPositionMatrix(), getX(), getY(), backgroundWidth, 35, 5f,0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment());
        }
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), backgroundWidth, 35).quality(5)
                .round(5f)
                .softness(1)
                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                .color(ThemeManager.BackgroundGui.getColor())
                .build());

        if (mc.player != null && lastTarget != mc.player) {
            float ourHp = PlayerInteractionHelper.getHealth(mc.player);
            float theirHp = PlayerInteractionHelper.getHealth(lastTarget);
            boolean weWinning = ourHp > theirHp;
            String label = weWinning ? "WIN" : "LOSE";
            int color = weWinning ? new Color(10, 255, 10).getRGB() : new Color(255, 0, 0).getRGB();
            float labelWidth = nameFont.getStringWidth(label);
            float topY = getY() - 10f;
            nameFont.drawString(matrix, label, getX() + (backgroundWidth - labelWidth) / 2f-12, topY+2, color);
        }

        nameFont.drawString(matrix, lastTarget.getName().getString(), scrollStartX-2, getY() + 5f, ThemeManager.textColor.getColor());

        rectangle.render(ShapeProperties.create(matrix, hpBarStartX, hpBarY, hpBarWidth, hpBarHeight)
                .round(2f)
                .thickness(2)
                .outlineColor(new Color(255,255,255,25).getRGB())
                .color(new Color(0,0,0,50).getRGB())
                .build());

        float hpWidth = (health / 360f) * hpBarWidth;
        if (hpWidth > 0) {
            rectangle.render(ShapeProperties.create(matrix, hpBarStartX, hpBarY, hpWidth, hpBarHeight)
                    .round(2f)
                    .color(ColorAssist.getClientColor(), ColorAssist.getClientColor(),ColorAssist.getClientColor2(),ColorAssist.getClientColor2())
                    .build());
        }

        if (absorption > 0 && !Network.isFunTime()) {
            float absorptionWidth = (absorption / 360f) * hpBarWidth;
            rectangle.render(ShapeProperties.create(matrix, hpBarStartX, hpBarY, absorptionWidth, hpBarHeight)
                    .round(2f)
                    .color(new Color(255, 215, 0, 255).getRGB(), new Color(255, 215, 0, 255).getRGB(), new Color(255, 128, 0, 255).getRGB(), new Color(255, 128, 0, 255).getRGB())
                    .build());
        }

        drawItemsInMain(context, matrix);

        Matrix4f matrix4f = matrix.peek().getPositionMatrix();
        matrix.pop();

        double animationValue = scaleAnimation.getOutput().doubleValue();
        if (animationValue > 0.6) {
            float hpTextX = hpBarStartX + (hpBarWidth - hpTextWidth) / 2f;
            float hpTextY = hpBarY + (hpBarHeight - MEDIUM_FONT.get().getMetrics().baselineHeight() * hpTextSize) / 2f;
            Builder.text()
                    .font(MEDIUM_FONT.get())
                    .text(stringHp + "hp")
                    .size(hpTextSize)
                    .color(ThemeManager.textColor.getColor())
                    .build()
                    .render(matrix4f, hpTextX, hpTextY-0.75, 0);
        }

        RenderSystem.enableDepthTest();
    }

    private void drawItemsInMain(DrawContext context, MatrixStack matrix) {
        ItemStack[] slots = new ItemStack[] {
                lastTarget.getOffHandStack(),
                lastTarget.getMainHandStack(),
                lastTarget.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD),
                lastTarget.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST),
                lastTarget.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS),
                lastTarget.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET)
        };
        float startX = getX() + 36;
        float y = getY() + 19f;
        float itemSpacing = 11f;
        float itemSize = 16 * 0.75F;
        float itemOffsetX = -1.5f;
        float itemOffsetY = 2f;

        RenderSystem.disableDepthTest();
        for (int i = 0; i < 6; i++) {
            float currentX = startX + i * itemSpacing;
            if (!slots[i].isEmpty()) {
                matrix.push();
                matrix.translate(0, 0, 100);
                Render2D.defaultDrawStack(context, slots[i], currentX + itemOffsetX, y + itemOffsetY, true, Aura.aimMode.get().equals("ХолиВорлд") ? false : true, 0.7F);
                matrix.pop();
            } else {
                String xText = "p";
                FontRenderer font = Fonts.getSize(12, Fonts.Type.ESSENCE);
                float textWidth = font.getStringWidth(xText);
                float textHeight = font.getStringHeight(xText);
                float textX = currentX + itemOffsetX + (itemSize - textWidth) / 2.0F;
                float textY = y + itemOffsetY + (itemSize - textHeight) / 2.0F;
                font.drawString(matrix, xText, textX, textY+6.5f, new Color(137, 137, 140, 255).getRGB());
            }
        }
    }

    private void drawFace(DrawContext context) {
        if (mc.getEntityRenderDispatcher() == null || lastTarget == null) {
            return;
        }
        EntityRenderer<? super LivingEntity, ?> baseRenderer = mc.getEntityRenderDispatcher().getRenderer(lastTarget);
        if (baseRenderer == null || !(baseRenderer instanceof LivingEntityRenderer<?, ?, ?>)) {
            return;
        }
        @SuppressWarnings("unchecked")
        LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?> renderer = (LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>) baseRenderer;
        LivingEntityRenderState state = renderer.getAndUpdateRenderState(lastTarget, tickCounter.getTickDelta(false));
        if (state == null) {
            return;
        }
        Identifier textureLocation = renderer.getTexture(state);
        float alpha = faceAlphaAnimation.getOutput().floatValue();
        MatrixStack matrix = context.getMatrices();
        Calculate.setAlpha(alpha, () -> {
            float hpRectX = getX() + 4f;
            float hpRectY = getY() + 4f;
            float hpRectWidth = 26f;
            float hpRectHeight = 24f;

            float padding = 3f;

            float faceWidth = hpRectWidth + 12 - (padding * 2f);
            float faceHeight = hpRectHeight +13 - (padding * 2f);

            float hpRectCenterX = hpRectX + hpRectWidth / 2f+1;
            float hpRectCenterY = hpRectY + hpRectHeight / 2f+1f;

            float faceX = hpRectCenterX - faceWidth / 2f;
            float faceY = hpRectCenterY - faceHeight / 2f;

            ScissorAssist scissorManager = Essence.getInstance().getScissorManager();
            scissorManager.push(matrix.peek().getPositionMatrix(), faceX, faceY, faceWidth, faceHeight);

            float originalFaceSize = 35.5f;
            float originalFaceX = hpRectCenterX - originalFaceSize / 2f;
            float originalFaceY = hpRectCenterY - originalFaceSize / 2f;

            Render2D.drawTexture(context, textureLocation, originalFaceX, originalFaceY, originalFaceSize, 10, 8, 8, 64, ColorAssist.getRect(1), ColorAssist.multRed(-1, 1 + lastTarget.hurtTime / 4F));

            scissorManager.pop();
        });
    }
}