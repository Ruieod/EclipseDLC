package code.essence.common.repository.way;
import code.essence.utils.display.render.geometry.Render2D;
import code.essence.utils.display.render.post.KawaseBlur;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import code.essence.utils.client.managers.event.EventHandler;
import code.essence.utils.client.managers.event.EventManager;
import code.essence.utils.display.render.font.FontRenderer;
import code.essence.utils.display.render.font.Fonts;
import code.essence.utils.display.render.shape.ShapeProperties;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.display.interfaces.QuickLogger;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.math.projection.Projection;
import code.essence.events.render.DrawEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WayRepository implements QuickImports, QuickLogger {
    public WayRepository(EventManager eventManager) {
        eventManager.register(this);
    }

    public List<Way> wayList = new ArrayList<>();

    public boolean isEmpty() {
        return wayList.isEmpty();
    }

    public void addWay(String name, BlockPos pos, String server) {
        wayList.add(new Way(name, pos, server));
    }


    public void upsertWay(String name, BlockPos pos, String server) {
        if (name == null) return;

        for (int i = 0; i < wayList.size(); i++) {
            Way w = wayList.get(i);
            if (w != null && w.name().equalsIgnoreCase(name)) {
                wayList.set(i, new Way(name, pos, server));
                return;
            }
        }
        addWay(name, pos, server);
    }

    public boolean hasWay(String text) {
        return wayList.stream().anyMatch(s -> s.name().equalsIgnoreCase(text));
    }

    public void deleteWay(String name) {
        wayList.removeIf(macro -> macro.name().equalsIgnoreCase(name));
    }

    public void clearList() {
        if (!isEmpty()) wayList.clear();
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (isEmpty() || mc.getNetworkHandler() == null || mc.getNetworkHandler().getServerInfo() == null) return;

        MatrixStack matrix = e.getDrawContext().getMatrices();

        wayList.forEach(way -> {
            Vec3d wayVec = way.pos().toCenterPos();
            Vec3d vec = Projection.worldSpaceToScreenSpace(wayVec);

            if (Projection.canSee(wayVec) && way.server().equalsIgnoreCase(mc.getNetworkHandler().getServerInfo().address)) {
                String text = way.name() + " - " + Math.round(mc.getEntityRenderDispatcher().camera.getPos().distanceTo(wayVec)) + "m";
                FontRenderer font = Fonts.getSize(13, Fonts.Type.SuisseIntlMedium);
                float height = font.getStringHeight(text) / 4;
                float width = font.getStringWidth(text);
                float padding = 3;
                double x = vec.getX() - width / 2;
                double y = vec.getY() - height / 2;

                float squareX = (float)(x - padding);
                float squareY = (float)(y - padding * 2);
                float squareWidth = width + padding * 2;
                float squareHeight = height + padding * 2;


              //  Render2D.rectangleWithMask(matrix.peek().getPositionMatrix(), squareX, squareY, squareWidth, squareHeight, 2f,0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment());

               /* rectangle.render(ShapeProperties.create(matrix, squareX, squareY, squareWidth, squareHeight)
                        .round(2)
                        .color(new Color(27,27,30,255).getRGB()).build());

                */

                FontRenderer waypointIcon = Fonts.getSize(25, Fonts.Type.WAYPOINT_ICONS);
                float bWidth = waypointIcon.getStringWidth("B");
                float bHeight = waypointIcon.getStringHeight("B");
                float bX = squareX + (squareWidth - bWidth) / 2f;
                float bY = squareY + (squareHeight - bHeight) / 2f;
                waypointIcon.drawString(matrix, "B", bX, bY+2, ColorAssist.getText());
                
                font.drawString(matrix,text,x,y - 2f, ColorAssist.getText());
            }
        });
    }
}