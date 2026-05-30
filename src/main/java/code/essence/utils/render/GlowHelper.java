package code.essence.utils.render;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import static org.lwjgl.opengl.GL11.*;
import static com.mojang.blaze3d.platform.GlStateManager.*;

public class GlowHelper {
	
	public static final HashMap<Integer, Integer> glowCache = new HashMap<>();
	
	public static void drawGlow(double x, double y, int width, int height, int glowRadius, Color color) {
		_enableBlend();
		_blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
		glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.0001f);

		_bindTexture(getGlowTexture(width, height, glowRadius));
		width += glowRadius * 2;
		height += glowRadius * 2;
		x -= glowRadius;
		y -= height - glowRadius;
		
		applyColor(color);
		glBegin(GL_QUADS);
		glTexCoord2d(0, 1);
		glVertex2d(x, y);
		glTexCoord2d(0, 0);
		glVertex2d(x, y + height);
		glTexCoord2d(1, 0);
		glVertex2d(x + width, y + height);
		glTexCoord2d(1, 1);
		glVertex2d(x + width, y);
		glEnd();
        
		_bindTexture(0);
		glDisable(GL_ALPHA_TEST);
		_disableBlend();
    }
	
	public static int getGlowTexture(int width, int height, int blurRadius) {
		int identifier = (width * 401 + height) * 407 + blurRadius;
		int texId = glowCache.getOrDefault(identifier, -1);
		
        if(texId == -1) {
            BufferedImage original = new BufferedImage((int)(width + blurRadius * 2), (int)(height + blurRadius * 2), BufferedImage.TYPE_INT_ARGB_PRE);

            Graphics g = original.getGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(blurRadius, blurRadius, (int)width, (int)height);
            g.dispose();

            GlowFilter glow = new GlowFilter(blurRadius);
            BufferedImage blurred = glow.filter(original, null);
            try {
    			texId = loadTexture(blurred);
    			glowCache.put(identifier, texId);
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
        }
        return texId;
	}
	
	public static void applyColor(Color color) {
		glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
	}
	
	private static int loadTexture(BufferedImage image) {

		return (int) (System.currentTimeMillis() % 10000);
	}
}
