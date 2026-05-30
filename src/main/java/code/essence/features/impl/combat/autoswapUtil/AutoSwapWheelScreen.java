package code.essence.features.impl.combat.autoswapUtil;

import code.essence.features.impl.combat.AutoSwap;
import code.essence.utils.client.logs.Logger;
import code.essence.utils.display.interfaces.QuickImports;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;


import java.awt.*;

public class AutoSwapWheelScreen extends Screen implements QuickImports {

    private final AutoSwap autoSwap;


    private int hoveredSector = -1;


    private static class IconRect {
        int x, y, w, h;
        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }
    private final IconRect[] iconRects = new IconRect[3];


    private final ItemStack[] lastStacks = new ItemStack[3];


    public AutoSwapWheelScreen(AutoSwap autoSwap) {
        super(Text.empty());
        this.autoSwap = autoSwap;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(null);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Logger.info("[AutoSwapWheel] mouseClicked: button=" + button + ", mouseX=" + mouseX + ", mouseY=" + mouseY + ", hoveredSector=" + hoveredSector);

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            for (int i = 0; i < 3; i++) {
                IconRect r = iconRects[i];
                if (r != null && r.contains(mouseX, mouseY)) {
                    Logger.info("[AutoSwapWheel] ПКМ по иконке слота " + i);
                    if (client != null) {
                        close();
                        autoSwap.startSelectingItem(i);
                    }
                    return true;
                }
            }
            if (hoveredSector >= 0 && hoveredSector < 3) {
                Logger.info("[AutoSwapWheel] ПКМ по сектору " + hoveredSector);
                if (client != null) {
                    close();
                    autoSwap.startSelectingItem(hoveredSector);
                }
                return true;
            }
        }


        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            Logger.info("[AutoSwapWheel] ЛКМ нажата");

            for (int i = 0; i < 3; i++) {
                IconRect r = iconRects[i];
                if (r != null && r.contains(mouseX, mouseY)) {
                    ItemStack st = lastStacks[i];
                    Logger.info("[AutoSwapWheel] ЛКМ по иконке слота " + i + ", stack=" + (st != null && !st.isEmpty() ? st.getItem().toString() : "пусто"));
                    if (st != null && !st.isEmpty()) {
                        close();
                        autoSwap.startSwapToItemStack(st);
                    } else {
                        Logger.warn("[AutoSwapWheel] Стек пуст для слота " + i);
                        close();
                    }
                    return true;
                }
            }


            if (hoveredSector >= 0 && hoveredSector < 3) {
                ItemStack st = lastStacks[hoveredSector];
                Logger.info("[AutoSwapWheel] ЛКМ по сектору " + hoveredSector + ", stack=" + (st != null && !st.isEmpty() ? st.getItem().toString() : "пусто"));
                if (st != null && !st.isEmpty()) {
                    close();
                    autoSwap.startSwapToItemStack(st);
                } else {
                    Logger.warn("[AutoSwapWheel] Стек пуст для сектора " + hoveredSector);
                    close();
                }
                return true;
            } else {
                Logger.warn("[AutoSwapWheel] hoveredSector невалиден: " + hoveredSector);
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {

        float cx = width / 2f;
        float cy = height / 2f;

        float base = Math.min(width, height);
        float outerR = base * 0.20f;
        float innerR = outerR - 24f;


        int baseSectorColor  = new Color(255, 255, 255, 80).getRGB();
        int hoverSectorColor = new Color(255, 100, 100, 110).getRGB();
        hoveredSector = computeHoveredSector(mouseX, mouseY, cx, cy, innerR, outerR);


        float sectorSize = 120f;
        float startAngle = -90f - sectorSize / 2f;

        for (int i = 0; i < 3; i++) {
            float sa = startAngle + i * sectorSize;
            float ea = sa + sectorSize;
            int col = (i == hoveredSector) ? hoverSectorColor : baseSectorColor;
            drawRingSector(ctx, cx, cy, innerR, outerR, sa, ea, col);
        }


        int lineColor = new Color(255, 255, 255, 255).getRGB();
        for (int i = 0; i < 3; i++) {
            float ang = startAngle + i * sectorSize;
            drawRadialLine(ctx, cx, cy, innerR, outerR, ang, lineColor);
        }


        for (int i = 0; i < 3; i++) {
            iconRects[i] = null;
            lastStacks[i] = ItemStack.EMPTY;
        }


        if (client != null && client.player != null) {
            float iconRadius = (innerR + outerR) / 2f;

            for (int i = 0; i < 3; i++) {
                ItemStack st = autoSwap.getWheelSlotStack(i);
                lastStacks[i] = st; 

                float sa = startAngle + i * sectorSize;
                float mid = sa + sectorSize / 2f;
                double rad = Math.toRadians(mid);

                float ix = (float) (cx + iconRadius * Math.cos(rad));
                float iy = (float) (cy + iconRadius * Math.sin(rad));

                IconRect r = new IconRect();
                r.x = (int) (ix - 8);
                r.y = (int) (iy - 8);
                r.w = 16;
                r.h = 16;
                iconRects[i] = r;

                if (!st.isEmpty()) {
                    ctx.drawItem(st, r.x, r.y);
                } else {
                    Logger.info("[AutoSwapWheel] Слот " + i + ": стек пуст, wheelSlot item=" + (autoSwap.getWheelSlotItem(i) != null ? autoSwap.getWheelSlotItem(i).toString() : "null"));
                }
            }
        }

        
        String hintText = "ПКМ - изменить предмет";
        int textColor = new Color(255, 255, 255, 200).getRGB();
        float textY = cy + outerR + 30f; 
        int textWidth = textRenderer.getWidth(hintText);
        ctx.drawTextWithShadow(textRenderer, Text.literal(hintText), (int)(cx - textWidth / 2f), (int)textY, textColor);

    }


    private int computeHoveredSector(double mouseX, double mouseY,
                                     float cx, float cy,
                                     float innerR, float outerR) {
        float dx = (float) mouseX - cx;
        float dy = (float) mouseY - cy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        
        float ang = (float) Math.toDegrees(Math.atan2(dy, dx));
        if (ang < 0) ang += 360f;

        float sectorSize = 120f;
        float startAngle = -90f - sectorSize / 2f;
        if (startAngle < 0) startAngle += 360f;

        float rel = ang - startAngle;
        if (rel < 0) rel += 360f;

        int idx = (int) (rel / sectorSize);
        if (idx < 0 || idx > 2) return -1;
        
        
        return idx;
    }

    private void drawRingSector(DrawContext ctx, float cx, float cy,
                                float innerR, float outerR,
                                float startDeg, float endDeg,
                                int color) {

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();

        boolean cullWasEnabled =
                org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        int segments = 220;
        float startRad = (float) Math.toRadians(startDeg);
        float endRad   = (float) Math.toRadians(endDeg);
        float step     = (endRad - startRad) / segments;

        for (int i = 0; i < segments; i++) {
            float a1 = startRad + step * i;
            float a2 = startRad + step * (i + 1);

            float cos1 = MathHelper.cos(a1);
            float sin1 = MathHelper.sin(a1);
            float cos2 = MathHelper.cos(a2);
            float sin2 = MathHelper.sin(a2);

            float ox1 = cx + outerR * cos1;
            float oy1 = cy + outerR * sin1;
            float ox2 = cx + outerR * cos2;
            float oy2 = cy + outerR * sin2;

            float ix1 = cx + innerR * cos1;
            float iy1 = cy + innerR * sin1;
            float ix2 = cx + innerR * cos2;
            float iy2 = cy + innerR * sin2;

            buf.vertex(matrix, ox1, oy1, 0).color(color);
            buf.vertex(matrix, ox2, oy2, 0).color(color);
            buf.vertex(matrix, ix2, iy2, 0).color(color);

            buf.vertex(matrix, ox1, oy1, 0).color(color);
            buf.vertex(matrix, ix2, iy2, 0).color(color);
            buf.vertex(matrix, ix1, iy1, 0).color(color);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());

        if (cullWasEnabled) {
            RenderSystem.enableCull();
        } else {
            RenderSystem.disableCull();
        }

        RenderSystem.disableBlend();
    }

    private void drawRadialLine(DrawContext ctx, float cx, float cy,
                                float innerR, float outerR,
                                float angleDeg, int color) {
        float rad = (float) Math.toRadians(angleDeg);
        float cos = MathHelper.cos(rad);
        float sin = MathHelper.sin(rad);

        float x1 = cx + innerR * cos;
        float y1 = cy + innerR * sin;
        float x2 = cx + outerR * cos;
        float y2 = cy + outerR * sin;

        
        float lineWidth = .5f;
        
        
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length > 0) {
            
            float perpX = -dy / length * lineWidth;
            float perpY = dx / length * lineWidth;
            
            
            float x1a = x1 + perpX;
            float y1a = y1 + perpY;
            float x1b = x1 - perpX;
            float y1b = y1 - perpY;
            float x2a = x2 + perpX;
            float y2a = y2 + perpY;
            float x2b = x2 - perpX;
            float y2b = y2 - perpY;

            Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

            
            buf.vertex(matrix, x1a, y1a, 0).color(color);
            buf.vertex(matrix, x2a, y2a, 0).color(color);
            buf.vertex(matrix, x1b, y1b, 0).color(color);

            
            buf.vertex(matrix, x1b, y1b, 0).color(color);
            buf.vertex(matrix, x2a, y2a, 0).color(color);
            buf.vertex(matrix, x2b, y2b, 0).color(color);

            BufferRenderer.drawWithGlobalProgram(buf.end());
            RenderSystem.disableBlend();
        }
    }

    private Item mapItem(String type) {
        if (autoSwap == null) return Items.AIR;
        return autoSwap.getItemByType(type);
    }

      }