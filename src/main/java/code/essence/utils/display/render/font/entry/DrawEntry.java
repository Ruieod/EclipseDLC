package code.essence.utils.display.render.font.entry;

import code.essence.utils.display.render.font.glyph.Glyph;

public record DrawEntry(float atX, float atY, int color, Glyph toDraw) {
}
