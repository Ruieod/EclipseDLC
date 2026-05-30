package code.essence.display.hud;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import code.essence.utils.client.managers.api.draggable.AbstractDraggable;
import code.essence.features.impl.combat.Aura;
import code.essence.features.impl.render.Hud;
import code.essence.common.animation.Animation;
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
import java.awt.*;

public class TargetHud extends AbstractDraggable {
    private final Animation animation = new Decelerate().setMs(650).setValue(1);
    private final Animation faceAlphaAnimation = new Decelerate().setMs(125).setValue(1);
    private final StopWatch stopWatch = new StopWatch();
    private final StopWatch distanceUpdateTimer = new StopWatch();
    private LivingEntity lastTarget;
    private Item lastItem = Items.AIR;
    private float health;
    private float absorption;
    private float displayedDistance;
    private float scrollPosition = 0f;
    private float previousWidth = 100f;

    public TargetHud() {
        super("Target Hud", 10, 80, 100, 36, true);
        this.scaleAnimation = new EaseOut().setValue(1).setMs(100);
    }

    @Override
    public boolean visible() {
        
        boolean isActive = Hud.getInstance() != null && Hud.getInstance().interfaceSettings.isSelected("Target Hud") 
                && Hud.getInstance().state && Hud.getInstance().targetHudMode.isSelected("1");
        return isActive && scaleAnimation.isDirection(Direction.FORWARDS);
    }

    @Override
    public void tick() {
        
        if (!Hud.getInstance().interfaceSettings.isSelected("Target Hud") || !Hud.getInstance().state 
                || !Hud.getInstance().targetHudMode.isSelected("1")) {
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
        if (Hud.getInstance().interfaceSettings.isSelected("Target Hud") && Hud.getInstance().state 
                && Hud.getInstance().targetHudMode.isSelected("1")) {
            if (lastTarget != null) {
                MatrixStack matrix = context.getMatrices();
                drawMain(context, matrix, tickCounter.getTickDelta(false));
                drawFace(context);
            }
        }
    }

    private void drawMain(DrawContext context, MatrixStack matrix, float delta) {
        RenderSystem.disableDepthTest();
        matrix.push();
        matrix.translate(0, 0, -1000);
        
        FontRenderer font = Fonts.getSize(14, Fonts.Type.SuisseIntlMedium);
        FontRenderer distancefont = Fonts.getSize(12, Fonts.Type.SuisseIntlSemiBold);
        float hp = PlayerInteractionHelper.getHealth(lastTarget);
        String stringHp = (lastTarget.isInvisible() && !Network.isHolyWorld() && !Network.isSpookyTime() && !Network.isCopyTime()) ? " ??" : PlayerInteractionHelper.getHealthString(lastTarget);
        health = MathHelper.clamp(Calculate.interpolateSmooth(1, health, hp / lastTarget.getMaxHealth() * 360), 0, 360);
        float absorptionAmount = lastTarget.getAbsorptionAmount();
        absorption = MathHelper.clamp(Calculate.interpolateSmooth(1, absorption, absorptionAmount / 20.0F * 360), 0, 360);
        float actualDistance = mc.player.distanceTo(lastTarget);
        float roundedDistance = Math.round(actualDistance * 2) / 2.0f;
        if (distanceUpdateTimer.finished(10)) {
            displayedDistance = MathHelper.clamp(Calculate.interpolateSmooth(0.5f, displayedDistance, roundedDistance), 0, 100);
            distanceUpdateTimer.reset();
        }
        String distanceText = String.format("%.1f", displayedDistance);

        float nameWidth = font.getStringWidth(lastTarget.getName().getString());
        float hpTextWidth = Fonts.getSize(13, Fonts.Type.SuisseIntlMedium).getStringWidth(stringHp+"hp");

        float itemsEnd = 36 + 6 * 8f;
        float minWidthFromItems = itemsEnd + 17;
        
        float baseWidth;
        if (nameWidth > 40) {
            baseWidth = 124;
        } else {
            float scrollStartX = getX() + 39;
            float hpTextX = scrollStartX + nameWidth + 5;
            float hpEndX = hpTextX + 20;
            float calculatedWidth = hpEndX - getX() + 15;

            baseWidth = Math.max(calculatedWidth, minWidthFromItems);
        }
        

        float widthDifference = baseWidth - previousWidth;
        if (Math.abs(widthDifference) > 0.1f) {
            setX((int) (getX() - widthDifference / 2f));
            previousWidth = baseWidth;
        }
        
        setWidth((int) baseWidth);
        setHeight((int) 41);
        if (Hud.blur.isValue()) {
            Render2D.rectangleWithMask(matrix.peek().getPositionMatrix(), getX(), getY(), getWidth()-8, 31.8f, 8.5f,0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment());
        }

        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth()-8, 31.8).quality(5)
                .round(8.5f)
                .softness(1)
                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                .color(ThemeManager.BackgroundGui.getColor())
                .build());

        rectangle.render(ShapeProperties.create(matrix, getX()+34, getY()+4, getWidth() - 46, 24)
                .round(4f)
                .softness(2)
                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                .color(ThemeManager.BackgroundSettings.getColor())
                .build());

        rectangle.render(ShapeProperties.create(matrix, getX() + 4f, getY() + 4f, 26, 24)
                .round(4f)
                .thickness(2)
                .outlineColor(new Color(255,255,255,25).getRGB())
                .color(new Color(0,0,0,50).getRGB())
                .build());

        float hpWidth = (health / 360f) * 26f;
        if (hpWidth > 0) {
            int rainbowColor = ColorAssist.getTrueRainbowColor(ColorAssist.getClientColor(), ColorAssist.getClientColor2());
            rectangle.render(ShapeProperties.create(matrix, getX() + 4f, getY() + 4f, hpWidth, 24)
                    .round(4f)
                    .color(ColorAssist.getClientColor(), ColorAssist.getClientColor2(),ColorAssist.getClientColor(),ColorAssist.getClientColor2())
                    .build());
        }

        if (absorption > 0 && !Network.isFunTime()) {
            float absorptionWidth = (absorption / 360f) * 26f;
            rectangle.render(ShapeProperties.create(matrix, getX() + 4f, getY() + 4f, absorptionWidth, 24)
                    .round(4f)
                    .color(new Color(255, 215, 0, 255).getRGB(), new Color(255, 128, 0, 255).getRGB(), new Color(255, 215, 0, 255).getRGB(), new Color(255, 128, 0, 255).getRGB())
                    .build());
        }

        drawItemsInMain(context, matrix);

        float itemsEndX = getX() + 36 + 6 * 8f;
        float scrollStartX = getX() + 39;
        float scrollWidth = itemsEndX - scrollStartX;


        if (nameWidth > 40) {
            ScissorAssist scissorManager = Essence.getInstance().getScissorManager();
            scissorManager.push(matrix.peek().getPositionMatrix(), scrollStartX, getY() + 4, scrollWidth, 24);

            String name = lastTarget.getName().getString();
            String separation = " ";
            String scrollingText = name + separation + name;

            float scrollingTextWidth = font.getStringWidth(scrollingText);
            float separationWidth = font.getStringWidth(separation);
            float scrollCycle = nameWidth + separationWidth;

            float scrollSpeed = .25f;
            scrollPosition += scrollSpeed * delta;
            scrollPosition = scrollPosition % scrollCycle;

            font.drawString(matrix, scrollingText, scrollStartX - scrollPosition, getY() + 11f, ThemeManager.textColor.getColor());

            scissorManager.pop();
        } else {
            scrollPosition = 0f;
            Fonts.getSize(14, Fonts.Type.SuisseIntlMedium).drawString(matrix, lastTarget.getName().getString(), scrollStartX, getY() + 11f, ThemeManager.textColor.getColor());
        }

        float secondBgX = getX() + 34;
        float secondBgY = getY() + 4;
        float secondBgWidth = getWidth() - 46;

        float leftPaddingFromBg = scrollStartX - secondBgX;
        float bgRightEdge = secondBgX + secondBgWidth;
        float rightPaddingHpX = bgRightEdge - leftPaddingFromBg - hpTextWidth ;
        
        float hpTextX;
        if (nameWidth > 40) {
            float desiredHpX = itemsEndX + 35 - hpTextWidth;
            hpTextX = Math.min(desiredHpX, rightPaddingHpX);
        } else {
            float hpTextXu4et = scrollStartX + nameWidth + 5;
            hpTextX = hpTextXu4et;
        }
        float hpTextY = secondBgY + 6;
        Fonts.getSize(13, Fonts.Type.SuisseIntlMedium).drawRainbowString(matrix, stringHp+"hp", hpTextX-0.5, hpTextY+1.5, ColorAssist.getClientColor(),ColorAssist.getClientColor2());
        
        matrix.pop();
        RenderSystem.enableDepthTest();
    }

    private void drawItemsInMain(DrawContext context, MatrixStack matrix) {
        ItemStack[] slots = new ItemStack[] {
                lastTarget.getMainHandStack(),
                lastTarget.getOffHandStack(),
                lastTarget.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD),
                lastTarget.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST),
                lastTarget.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS),
                lastTarget.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET)
        };
        float startX = getX() + 36;
        float y = getY() + 19f;
        float slotSize = 16 * 0.5F + 2;

        RenderSystem.disableDepthTest();
       for (int i = 0; i < 6; i++) {
            float currentX = startX + i * 8f;
            if (!slots[i].isEmpty()) {
                matrix.push();
                matrix.translate(0, 0, 100);
                Render2D.defaultDrawStack(context, slots[i], currentX+1, y - 2.8f, true, false, 0.5F);
                matrix.pop();
            } else {
                String xText = "p";
                FontRenderer font = Fonts.getSize(12, Fonts.Type.ESSENCE);
                float textWidth = font.getStringWidth(xText);
                float textHeight = font.getStringHeight(xText);
                float textX = currentX + (slotSize - textWidth) / 2.0F;
                float textY = y +1 + 0.5F + (slotSize - textHeight) / 2.0F;
                font.drawString(matrix, xText, currentX+2.51, textY + 1.25f, new Color(137, 137, 140, 255).getRGB());
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
            
            float faceWidth = hpRectWidth - (padding * 2f);
            float faceHeight = hpRectHeight - (padding * 2f);
            
            float hpRectCenterX = hpRectX + hpRectWidth / 2f;
            float hpRectCenterY = hpRectY + hpRectHeight / 2f;
            
            float faceX = hpRectCenterX - faceWidth / 2f;
            float faceY = hpRectCenterY - faceHeight / 2f;
            
            ScissorAssist scissorManager = Essence.getInstance().getScissorManager();
            scissorManager.push(matrix.peek().getPositionMatrix(), faceX, faceY, faceWidth, faceHeight);
            
            float originalFaceSize = 20.5f;
            float originalFaceX = hpRectCenterX - originalFaceSize / 2f;
            float originalFaceY = hpRectCenterY - originalFaceSize / 2f;
            
            Render2D.drawTexture(context, textureLocation, originalFaceX, originalFaceY, originalFaceSize, 4, 8, 8, 64, ColorAssist.getRect(1), ColorAssist.multRed(-1, 1 + lastTarget.hurtTime / 4F));
            
            scissorManager.pop();
        });
    }
}