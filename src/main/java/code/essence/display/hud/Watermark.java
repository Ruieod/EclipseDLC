package code.essence.display.hud;

import code.essence.features.impl.render.Hud;
import code.essence.utils.display.render.geometry.Render2D;
import code.essence.utils.display.render.post.KawaseBlur;
import code.essence.utils.theme.ThemeManager;
import com.google.common.base.Suppliers;
import code.essence.utils.client.managers.api.draggable.AbstractDraggable;
import code.essence.utils.display.atlasfont.msdf.MsdfFont;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.display.render.systemrender.builders.Builder;
import code.essence.Essence;
import code.essence.utils.display.color.ColorAssist;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.function.Supplier;

public class    Watermark extends AbstractDraggable {
    private int fpsCount = 0;
    private static final Supplier<MsdfFont> ICONS_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("icons").data("icons").build());
    private static final Supplier<MsdfFont> ICONS_FONT_1 = Suppliers.memoize(() -> MsdfFont.builder().atlas("clienticon1").data("clienticon1").build());
    private static final Supplier<MsdfFont> BOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());
    private static final Supplier<MsdfFont> ICONS = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());
    private static final Supplier<MsdfFont> ESSENCE_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("essence").data("essence").build());
    private static final Supplier<MsdfFont> MEDIUM_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("suisseintlmedium").data("suisseintlmedium").build());
    private static final Supplier<MsdfFont> REGULAR_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("suisseintlregular").data("suisseintlregular").build());

    public Watermark() {
        super("Watermark", 10, 10, 92, 16, true);
    }

    @Override
    public void tick() {
        fpsCount = mc.getCurrentFps();
    }

    @Override
    public void drawDraggable(DrawContext e) {
        MatrixStack matrix = e.getMatrices();
        Matrix4f matrix4f = matrix.peek().getPositionMatrix();
        String offset = "";
        String name = Essence.getInstance().getClientInfoProvider().clientName() + offset;
        String icon = "A ";
        String point = " • ";
        String username = Essence.getInstance().getNativeUsername();
        String serverIp = "Singleplayer";
        try {
            if (mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null) {
                serverIp = mc.getCurrentServerEntry().address;
            }
        } catch (Exception ex1) {
            serverIp = "Singleplayer";
        }
        String pingNumber = "0";
        try {
            if (mc.getNetworkHandler() != null && mc.player != null && mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null) {
                pingNumber = String.valueOf(mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency());
            }
        } catch (Exception ex2) {
            pingNumber = "0";
        }
        String pingMs = "ms";
        String title = ("admin");;

        float iconWidth = ICONS_FONT.get().getWidth(icon, 12);
        float titleWidth = Fonts.getSize(12, Fonts.Type.DEFAULT).getStringWidth(title);
        
        float defaultThickness = 0.05f;
        float defaultOutlineThickness = 0.0f;
        float defaultSpacing = 0.0f;
        float effectiveThickness62 = (defaultThickness + defaultOutlineThickness * 0.5f) * 0.5f * 6.2f;
        float effectiveThickness6 = (defaultThickness + defaultOutlineThickness * 0.5f) * 0.5f * 6f;
        float effectiveThickness5 = (defaultThickness + defaultOutlineThickness * 0.5f) * 0.5f * 5f;
        
        float usernameVisualWidth = MEDIUM_FONT.get().getVisualRightEdge(username, 6.2f, effectiveThickness62, defaultSpacing);
        float serverIpVisualWidth = MEDIUM_FONT.get().getVisualRightEdge(serverIp, 6f, effectiveThickness6, defaultSpacing);
        float pingNumberVisualWidth = MEDIUM_FONT.get().getVisualRightEdge(pingNumber, 6f, effectiveThickness6, defaultSpacing);
        float pingMsVisualWidth = REGULAR_FONT.get().getVisualRightEdge(pingMs, 5f, effectiveThickness5, defaultSpacing);
        
        float usernameWidth = MEDIUM_FONT.get().getWidth(username, 6.2f);
        float serverIpWidth = MEDIUM_FONT.get().getWidth(serverIp, 6);
        float pingNumberWidth = MEDIUM_FONT.get().getWidth(pingNumber, 6);
        float pingMsWidth = REGULAR_FONT.get().getWidth(pingMs, 5);
        float iconCWidth = ESSENCE_FONT.get().getWidth("c", 7);
        float iconNWidth = ESSENCE_FONT.get().getWidth("n", 7);
        float iconOWidth = ESSENCE_FONT.get().getWidth("o", 7);
        float circleSize = 2f;
        
        float POINT_GAP = 3f;
        float icon2Width = ICONS_FONT_1.get().getWidth("D", 11);
        float pointWidth = Fonts.getSize(12, Fonts.Type.BOLD).getStringWidth(point);
        
        float contentWidth = 5.5f + iconCWidth + 2.5f + usernameVisualWidth + POINT_GAP + circleSize + POINT_GAP + iconNWidth + 2.5f + serverIpVisualWidth + POINT_GAP + circleSize + POINT_GAP + iconOWidth + 2.5f + pingNumberVisualWidth + pingMsVisualWidth + 5.5f;
        float grayPlankWidth = contentWidth;
        float totalWidth = 27f + grayPlankWidth + 14f;

        setWidth((int) totalWidth);


        if (Hud.blur.isValue()) {
            Render2D.rectangleWithMask(matrix4f,getX(),getY(),totalWidth-9,getHeight()+12,7f,0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment());
        }
            rectangle.render(ShapeProperties.create(matrix, getX(), getY(), totalWidth - 9, getHeight() + 12)
                    .round(7f)
                    .softness(1)
                             .outlineColor(new Color(33, 33, 33, 255).getRGB())
                    .color(ThemeManager.BackgroundGui.getColor())
                    .build());




        rectangle.render(ShapeProperties.create(matrix, getX()+27, getY() +5, grayPlankWidth, getHeight()+3)
                .round(2.5f)
                .softness(1)
                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                         .color(ThemeManager.BackgroundSettings.getColor())
                .build());


        float squareSize = 20f;
        float squareX = getX() + 3;
        float squareY = getY() + (getHeight() - squareSize) / 2f;
        
        rectangle.render(ShapeProperties.create(matrix, squareX+ .5, squareY +6 , squareSize, squareSize)
                .round(4f)
                .color(ColorAssist.getClientColor(), 
                       ColorAssist.getClientColor2(),
                       ColorAssist.getClientColor(), 
                       ColorAssist.getClientColor2())
                .build());
        
        float aSize = 13f;
        float aWidth = ESSENCE_FONT.get().getWidth("a", aSize);
        float aX = squareX + (squareSize - aWidth) / 2f;
        float squareRenderY = squareY + 6f;
        float squareCenterY = squareRenderY + squareSize / 2f;
        float baselineHeight = ESSENCE_FONT.get().getMetrics().baselineHeight();
        float aY = squareCenterY - baselineHeight * aSize;
        Builder.text()
                .font(ESSENCE_FONT.get())
                .text("a")
                .size(aSize)
                .color(ThemeManager.textColor.getColor())
                .build()
                .render(matrix4f, aX, aY+5.5);


        float grayPlankStartX = getX() + 27;
        float grayPlankEndX = grayPlankStartX + grayPlankWidth;
        float YC = getY() + 10.2f;
        float circleY = YC + 2.8f;
        
        float iconCX = grayPlankStartX + 5.5f;
        Builder.text()
                .font(ESSENCE_FONT.get())
                .text("c")
                .size(7)
                .rainbow(true).rainbowColors(ColorAssist.getClientColor(), ColorAssist.getClientColor2())
                .build()
                .render(matrix4f, iconCX, YC);
        
        float usernameX = iconCX + iconCWidth + 2.5f;
        Builder.text()
                .font(MEDIUM_FONT.get())
                .text(username)
                .size(6.2f)
                .color(ThemeManager.textColor.getColor())
                .build()
                .render(matrix4f, usernameX, YC);
        
        float usernameRightEdge = usernameX + usernameVisualWidth;
        float point1X = usernameRightEdge + POINT_GAP;
        rectangle.render(ShapeProperties.create(matrix, point1X, circleY, circleSize, circleSize)
                .round(circleSize / 2f)
                .color(new Color(87, 87, 90, 255).getRGB())
                .build());
        
        float iconNX = point1X + circleSize + POINT_GAP;
        Builder.text()
                .font(ESSENCE_FONT.get())
                .text("n")
                .size(7)
                .rainbow(true).rainbowColors(ColorAssist.getClientColor(), ColorAssist.getClientColor2())
                .build()
                .render(matrix4f, iconNX, YC);
        
        float serverIpX = iconNX + iconNWidth + 2.5f;
        Builder.text()
                .font(MEDIUM_FONT.get())
                .text(serverIp)
                .size(6)
                .color(ThemeManager.textColor.getColor())
                .build()
                .render(matrix4f, serverIpX, YC);
        
        float serverIpRightEdge = serverIpX + serverIpVisualWidth;
        float point2X = serverIpRightEdge + POINT_GAP;
        rectangle.render(ShapeProperties.create(matrix, point2X, circleY, circleSize, circleSize)
                .round(circleSize / 2f)
                .color(new Color(87, 87, 90, 255).getRGB())
                .build());
        
        float iconOX = point2X + circleSize + POINT_GAP;
        Builder.text()
                .font(ESSENCE_FONT.get())
                .text("o")
                .size(7)
                .rainbow(true).rainbowColors(ColorAssist.getClientColor(), ColorAssist.getClientColor2())
                .build()
                .render(matrix4f, iconOX, YC);
        
        float pingNumberX = iconOX + iconOWidth + 2.5f;
        Builder.text()
                .font(MEDIUM_FONT.get())
                .text(pingNumber)
                .size(6)
                .color(ThemeManager.textColor.getColor())
                .build()
                .render(matrix4f, pingNumberX, YC);
        
        float pingNumberRightEdge = pingNumberX + pingNumberVisualWidth;
        float pingMsX = pingNumberRightEdge;
        
        Builder.text()
                .font(REGULAR_FONT.get())
                .text(pingMs)
                .size(5.5f)
                .color(new Color(115, 115, 115, 255).getRGB())
                .build()
                .render(matrix4f, pingMsX, YC+0.5);
    }
}
