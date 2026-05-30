package code.essence.utils.client.managers.api.draggable;

import code.essence.utils.interactions.interact.PlayerInteractionHelper;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import code.essence.common.animation.Animation;
import code.essence.common.animation.Direction;
import code.essence.common.animation.implement.Decelerate;
import code.essence.utils.display.interfaces.QuickImports;
import code.essence.utils.display.interfaces.QuickLogger;
import code.essence.utils.display.color.ColorAssist;
import code.essence.utils.display.render.geometry.Render2D;
import code.essence.Essence;
import code.essence.events.container.SetScreenEvent;
import code.essence.events.packet.PacketEvent;
import code.essence.features.impl.render.Hud;
import org.lwjgl.glfw.GLFW;

@Setter
@Getter
public abstract class AbstractDraggable implements Draggable, QuickImports, QuickLogger {
    private String name;
    private int x, y, width, height;
    private boolean dragging, canDrag;
    private int dragX, dragY;
    private boolean resizeModeActive = false;
    private long lastClickTime = 0;
    private static final long DOUBLE_CLICK_DELAY_MS = 300;
    private static AbstractDraggable currentlyEditing = null;
    private float scaleFactor = 1.0f; 

    public AbstractDraggable(String name, int x, int y, int width, int height, boolean canDrag) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.canDrag = canDrag;
    }

    public Animation scaleAnimation = new Decelerate().setValue(1).setMs(100);

    @Override
    public boolean visible() {
        return true;
    }

    @Override
    public void tick() {
        validPosition();
    }

    @Override
    public void packet(PacketEvent e) {}

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!dragging) {
            dragX = 0;
            dragY = 0;
        }
        Hud hud = Hud.getInstance();
        float mouseDragX = mouseX + dragX;
        float mouseDragY = mouseY + dragY;
        int windowWidth = window.getScaledWidth();
        int windowHeight = window.getScaledHeight();
        int radius = 3;

        if (resizeModeActive && isHovered(mouseX, mouseY)) {
            GLFW.glfwSetCursor(mc.getWindow().getHandle(), GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_NWSE_CURSOR));
        }

        if (dragging) {
            int screenWidth = window.getScaledWidth();
            int screenHeight = window.getScaledHeight();
            
            int newX = (int) mouseDragX;
            int newY = (int) mouseDragY;
            
            newX = Math.max(0, Math.min(newX, screenWidth - width));
            newY = Math.max(0, Math.min(newY, screenHeight - height));
            
            this.x = newX;
            this.y = newY;
        }

    }

    @Override
    public void setScreen(SetScreenEvent e) {
        if (!PlayerInteractionHelper.isChat(e.getScreen())) {
            dragging = false;
            resizeModeActive = false;
            dragX = 0;
            dragY = 0;
            if (currentlyEditing == this) {
                currentlyEditing = null;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && canDrag && isHovered(mouseX, mouseY)) {
            dragging = true;
            dragX = x - (int) mouseX;
            dragY = y - (int) mouseY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            try {
                Essence.getInstance().getFileController().saveFile(code.essence.utils.client.managers.file.impl.ElementsFile.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        dragging = false;
        dragX = 0;
        dragY = 0;
        GLFW.glfwSetCursor(mc.getWindow().getHandle(), GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR));
        return false;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (isHovered(mouseX, mouseY)) {
            float[] levels = {0.5f, 0.7f, 0.85f, 1.0f, 1.3333f, 1.7f}; 
            int currentLevel = 1; 
            
            for (int i = 0; i < levels.length; i++) {
                if (Math.abs(scaleFactor - levels[i]) < 0.05f) {
                    currentLevel = i;
                    break;
                }
            }
            
            int newLevel = currentLevel + (amount > 0 ? 1 : -1);
            newLevel = Math.max(0, Math.min(5, newLevel));
            
            if (newLevel != currentLevel) {
                scaleFactor = levels[newLevel];
                try {
                    Essence.getInstance().getFileController().saveFile(code.essence.utils.client.managers.file.impl.ElementsFile.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
        return true;
            }
        }
        return false;
    }
    
    public float getScaleFactor() {
        return scaleFactor;
    }
    
    public void setScaleFactor(float scale) {
        this.scaleFactor = scale;
    }

    public abstract void drawDraggable(DrawContext context);

    public void drawRect(float x, float y, float width, float height) {
        Render2D.drawQuad(x, y, width, height, ColorAssist.getText(0.5F));
    }

    public void stopAnimation() {
        scaleAnimation.setDirection(Direction.BACKWARDS);
    }

    public void startAnimation() {
        scaleAnimation.setDirection(Direction.FORWARDS);
    }

    public void validPosition() {
        int screenWidth = window.getScaledWidth();
        int screenHeight = window.getScaledHeight();
        
        if (width > screenWidth) width = Math.max(50, screenWidth - 10);
        if (height > screenHeight) height = Math.max(20, screenHeight - 10);
        
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x + width > screenWidth) x = screenWidth - width;
        if (y + height > screenHeight) y = screenHeight - height;
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width + 1 && mouseY >= y && mouseY <= y + height + 1;
    }

    public boolean isCloseAnimationFinished() {
        return scaleAnimation.isFinished(Direction.BACKWARDS);
    }

    public boolean canDraw(Hud hud, AbstractDraggable draggable) {
        return hud.isState() && hud.interfaceSettings.isSelected(draggable.getName()) && visible();
    }
}