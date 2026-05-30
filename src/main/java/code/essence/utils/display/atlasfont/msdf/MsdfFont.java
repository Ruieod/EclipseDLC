package code.essence.utils.display.atlasfont.msdf;

import code.essence.features.impl.render.Hud;
import com.mojang.blaze3d.systems.RenderSystem;
import code.essence.utils.display.atlasfont.providers.ResourceProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import code.essence.utils.display.atlasfont.msdf.FontData.AtlasData;
import code.essence.utils.display.atlasfont.msdf.FontData.MetricsData;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class MsdfFont {

	private final String name;
	private final AbstractTexture texture;
	private final AtlasData atlas;
	private final MetricsData metrics;
	private final Map<Integer, MsdfGlyph> glyphs;
	private final Map<Integer, Map<Integer, Float>> kernings;

	private MsdfFont(String name, AbstractTexture texture, AtlasData atlas, MetricsData metrics, Map<Integer, MsdfGlyph> glyphs, Map<Integer, Map<Integer, Float>> kernings) {
		this.name = name;
		this.texture = texture;
		this.atlas = atlas;
		this.metrics = metrics;
		this.glyphs = glyphs;
		this.kernings = kernings;
	}

	public int getTextureId() {
		return this.texture.getGlId();
	}
	
	public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, int color) {
		int prevChar = -1;
		for (int i = 0; i < text.length(); i++) {
			int _char = (int) text.charAt(i);
			MsdfGlyph glyph = this.glyphs.get(_char);
			
			if (glyph == null) continue;

			Map<Integer, Float> kerning = this.kernings.get(prevChar);
			if (kerning != null) {
				x += kerning.getOrDefault(_char, 0.0f) * size;
			}

			x += glyph.apply(matrix, consumer, size, x, y, z, color) + thickness + spacing;
			prevChar = _char;
		}
	}

	public void applyRainbowGlyphs(Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, int color1, int color2) {
		int prevChar = -1;
		long time = System.currentTimeMillis();
		for (int i = 0; i < text.length(); i++) {
			int _char = (int) text.charAt(i);
			MsdfGlyph glyph = this.glyphs.get(_char);
			
			if (glyph == null) continue;

			Map<Integer, Float> kerning = this.kernings.get(prevChar);
			if (kerning != null) {
				x += kerning.getOrDefault(_char, 0.0f) * size;
			}

			float t = (float) ((Math.sin((time + i * 100) / 500.0) + 1.0) / 2.0);
			t = Math.max(0.0f, Math.min(1.0f, t));
			int color = interpolateColor(color1, color2, t);
			x += glyph.apply(matrix, consumer, size, x, y, z, color) + thickness + spacing;
			prevChar = _char;
		}
	}

	private int interpolateColor(int colorStart, int colorEnd, float t) {
		float startAlpha = (colorStart >> 24 & 255) / 255.0F;
		float startRed = (colorStart >> 16 & 255) / 255.0F;
		float startGreen = (colorStart >> 8 & 255) / 255.0F;
		float startBlue = (colorStart & 255) / 255.0F;

		float endAlpha = (colorEnd >> 24 & 255) / 255.0F;
		float endRed = (colorEnd >> 16 & 255) / 255.0F;
		float endGreen = (colorEnd >> 8 & 255) / 255.0F;
		float endBlue = (colorEnd & 255) / 255.0F;

		float alpha = startAlpha + t * (endAlpha - startAlpha);
		float red = startRed + t * (endRed - startRed);
		float green = startGreen + t * (endGreen - startGreen);
		float blue = startBlue + t * (endBlue - startBlue);

		return ((int) (alpha * 255.0F) << 24) | ((int) (red * 255.0F) << 16) | ((int) (green * 255.0F) << 8) | (int) (blue * 255.0F);
	}
	
	public float getWidth(String text, float size) {
		int prevChar = -1;
		float width = 0.0f;
		for (int i = 0; i < text.length(); i++) {
			int _char = (int) text.charAt(i);
			MsdfGlyph glyph = this.glyphs.get(_char);
			
			if (glyph == null)
				continue;
			
			Map<Integer, Float> kerning = this.kernings.get(prevChar);
			if (kerning != null) {
				width += kerning.getOrDefault(_char, 0.0f) * size;
			}
			
			width += glyph.getWidth(size);
			prevChar = _char;
		}
		
		return width;
	}
	
	
	public float getVisualRightEdge(String text, float size, float effectiveThickness, float spacing) {
		if (text == null || text.isEmpty()) {
			return 0f;
		}
		
		int prevChar = -1;
		float x = 0.0f;
		
		for (int i = 0; i < text.length(); i++) {
			int _char = (int) text.charAt(i);
			MsdfGlyph glyph = this.glyphs.get(_char);
			
			if (glyph == null)
				continue;
			
			Map<Integer, Float> kerning = this.kernings.get(prevChar);
			if (kerning != null) {
				x += kerning.getOrDefault(_char, 0.0f) * size;
			}
			
			
			if (i < text.length() - 1) {
				x += glyph.getWidth(size) + effectiveThickness + spacing;
			} else {
				
				x += glyph.getVisualWidth(size);
			}
			
			prevChar = _char;
		}
		
		return x;
	}
	
	public String getName() {
		return this.name;
	}
	
	public AtlasData getAtlas() {
		return this.atlas;
	}
	
	public MetricsData getMetrics() {
		return this.metrics;
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private String name = "?";
		private Identifier dataIdentifer;
		private Identifier atlasIdentifier;
		
		private Builder() {}
		
		public Builder name(String name) {
			this.name = name;
			return this;
		}
		
		public Builder data(String dataFileName) {
			this.dataIdentifer = Identifier.of("mre", "fonts/" + dataFileName + ".json");
			return this;
		}
		
		public Builder atlas(String atlasFileName) {
			this.atlasIdentifier = Identifier.of("mre", "fonts/" + atlasFileName + ".png");
			return this;
		}
		
	public MsdfFont build() {
		FontData data = ResourceProvider.fromJsonToInstance(this.dataIdentifer, FontData.class);
		AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(this.atlasIdentifier);


		if (data == null) {
			throw new RuntimeException("Failed to read font data file: " + this.dataIdentifer.toString() +
					"; Are you sure this is json file? Try to check the correctness of its syntax.");
		}

		if (texture == null) {
			throw new RuntimeException("Failed to load texture: " + this.atlasIdentifier.toString() +
					"; Make sure the texture file exists at assets/mre/fonts/" + this.atlasIdentifier.getPath());
		}

		RenderSystem.recordRenderCall(() -> texture.setFilter(true, false));
			
		float aWidth = data.atlas().width();
		float aHeight = data.atlas().height();
		Map<Integer, MsdfGlyph> glyphs = data.glyphs().stream()
				.collect(Collectors.<FontData.GlyphData, Integer, MsdfGlyph>toMap(
						(glyphData) -> glyphData.unicode(),
						(glyphData) -> new MsdfGlyph(glyphData, aWidth, aHeight)
				));

	
			Map<Integer, Map<Integer, Float>> kernings = new HashMap<>();
			data.kernings().forEach((kerning) -> {
				Map<Integer, Float> map = kernings.get(kerning.leftChar());
				if (map == null) {
					map = new HashMap<>();
					kernings.put(kerning.leftChar(), map);
				}

				map.put(kerning.rightChar(), kerning.advance());
			});

			return new MsdfFont(this.name, texture, data.atlas(), data.metrics(), glyphs, kernings);
		}

	}

}