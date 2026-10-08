package fr.imero.mercuriale.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;

public final class Wordmark {
	public static final int TITLE_TRACKING = 2;
	public static final int LABEL_TRACKING = 1;

	private Wordmark() {
	}

	public static int draw(GuiGraphicsExtractor context, Font font, String text, int x, int y,
			int color, int tracking) {
		int cursor = x;
		for (int i = 0; i < text.length(); ) {
			int codePoint = text.codePointAt(i);
			String glyph = new String(Character.toChars(codePoint)).toUpperCase(Locale.ROOT);
			context.text(font, glyph, cursor, y, color, false);
			cursor += font.width(glyph) + tracking;
			i += Character.charCount(codePoint);
		}
		return Math.max(x, cursor - tracking);
	}

	public static int width(Font font, String text, int tracking) {
		int total = 0;
		int glyphs = 0;
		for (int i = 0; i < text.length(); ) {
			int codePoint = text.codePointAt(i);
			total += font.width(new String(Character.toChars(codePoint)).toUpperCase(Locale.ROOT));
			glyphs++;
			i += Character.charCount(codePoint);
		}
		return total + Math.max(0, glyphs - 1) * tracking;
	}
}
