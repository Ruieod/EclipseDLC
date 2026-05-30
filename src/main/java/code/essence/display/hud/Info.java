package code.essence.display.hud;

import code.essence.features.impl.render.Hud;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import code.essence.utils.display.render.font.FontRenderer;
import code.essence.utils.client.packet.network.Network;
import code.essence.utils.math.calc.Calculate;
import code.essence.utils.interactions.simulate.Simulations;
import code.essence.utils.client.managers.api.draggable.AbstractDraggable;
import code.essence.utils.display.render.font.Fonts;

import java.awt.Color;
import java.util.Objects;

public class Info extends AbstractDraggable {
    private int fpsCount = 0;

    public Info() {
        super("Info", 0, 0, 60, 0, false);
    }

    @Override
    public void tick() {
        super.tick();
        fpsCount = mc.getCurrentFps();
    }

    @Override
    public void drawDraggable(DrawContext context) {
        float chatX = 2;
        float baseY = window.getScaledHeight() - 35;
        float chatY = PlayerInteractionHelper.isChat(mc.currentScreen) ? baseY - 15 : baseY;

        BlockPos blockPos = Objects.requireNonNull(mc.player).getBlockPos();
        FontRenderer font = Fonts.getSize(13, Fonts.Type.SuisseIntlMedium);

        int grayColor = new Color(157, 157, 160).getRGB();

        String tpsLabel = "TPS: ";
        String tpsValue = String.valueOf(Calculate.round(Network.TPS, 0.1F));

        String fpsLabel = "FPS: ";
        String fpsValue = String.valueOf(fpsCount);

        String bpsLabel = "BPS: ";
        String bpsValue = String.valueOf(Calculate.round(Simulations.getSpeedSqrt(mc.player) * 20.0F, 0.25F));

        String coordsLabel = "Coords: ";
        String coordsValue = blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ();

        float maxWidth = Math.max(
                Math.max(font.getStringWidth(tpsLabel + tpsValue), font.getStringWidth(fpsLabel + fpsValue)),
                Math.max(font.getStringWidth(bpsLabel + bpsValue), font.getStringWidth(coordsLabel + coordsValue))
        );

        setX((int) chatX);
        setY((int) chatY);
        setWidth((int) maxWidth + 4);

        float lineHeight = 9f;
        float yOffset = Hud.hidecoords.isValue() ? lineHeight : 0;
        float startX = chatX + 2;

        font.drawRainbowString(context.getMatrices(), tpsLabel, startX, chatY + yOffset, ColorAssist.getClientColor(), ColorAssist.getClientColor2());
        float tpsValueX = startX + font.getStringWidth(tpsLabel);
        font.drawString(context.getMatrices(), tpsValue, tpsValueX, chatY + yOffset, grayColor);
        yOffset += lineHeight;

        font.drawRainbowString(context.getMatrices(), fpsLabel, startX, chatY + yOffset, ColorAssist.getClientColor(), ColorAssist.getClientColor2());
        float fpsValueX = startX + font.getStringWidth(fpsLabel);
        font.drawString(context.getMatrices(), fpsValue, fpsValueX, chatY + yOffset, grayColor);
        yOffset += lineHeight;

        font.drawRainbowString(context.getMatrices(), bpsLabel, startX, chatY + yOffset, ColorAssist.getClientColor(), ColorAssist.getClientColor2());
        float bpsValueX = startX + font.getStringWidth(bpsLabel);
        font.drawString(context.getMatrices(), bpsValue, bpsValueX, chatY + yOffset, grayColor);
        yOffset += lineHeight;

        if (!Hud.hidecoords.isValue()) {
            font.drawRainbowString(context.getMatrices(), coordsLabel, startX, chatY + yOffset, ColorAssist.getClientColor(), ColorAssist.getClientColor2());
            float coordsValueX = startX + font.getStringWidth(coordsLabel);
            font.drawString(context.getMatrices(), coordsValue, coordsValueX, chatY + yOffset, grayColor);
            yOffset += lineHeight;
        }
        
        setHeight((int) yOffset);
    }
}