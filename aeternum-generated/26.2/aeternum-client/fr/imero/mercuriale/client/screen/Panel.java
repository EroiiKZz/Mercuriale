package fr.imero.mercuriale.client.screen;

import fr.imero.mercuriale.theme.AeternumTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;

public final class Panel {
	private static final int TAB_MIN = 24;
	private static final int TAB_MAX = 72;
	private static final long ITEM_PROBE_INTERVAL_MS = 1_000L;

	private static boolean itemsReady;
	private static long lastItemProbe;

	private Panel() {
	}

	public static void backdrop(GuiGraphicsExtractor context, int width, int height) {
		context.fill(0, 0, width, height, Theme.BACKDROP);
	}

	public static void frame(GuiGraphicsExtractor context, int x, int y, int width, int height) {
		Shape.shadow(context, x, y, x + width, y + height);
		Shape.box(context, x, y, x + width, y + height, Theme.PANEL, Theme.BORDER);
		rule(context, x, y, width, height);
	}

	public static void rule(GuiGraphicsExtractor context, int x, int y, int width, int height) {
		if (Theme.RULE == AeternumTheme.Rule.NONE) {
			return;
		}
		int inset = Shape.radius(width, height);
		int left = x + inset;
		int span = Math.max(0, width - inset * 2);
		int right = Theme.RULE == AeternumTheme.Rule.FULL ? x + width - inset
			: left + Math.clamp(width / 4, Math.min(TAB_MIN, span), Math.min(TAB_MAX, span));
		Shape.pill(context, left, y, Math.max(left, right), y + Theme.PANEL_RULE, Theme.ACCENT);
	}

	public static void separator(GuiGraphicsExtractor context, int panelX, int y, int panelWidth) {
		context.fill(panelX + Theme.PADDING, y, panelX + panelWidth - Theme.PADDING, y + 1,
			Theme.BORDER);
	}

	public static boolean itemsReady() {
		if (itemsReady) {
			return true;
		}
		long now = System.currentTimeMillis();
		if (now - lastItemProbe < ITEM_PROBE_INTERVAL_MS) {
			return false;
		}
		lastItemProbe = now;
		try {
			new ItemStack(Items.STONE);
			itemsReady = true;
		} catch (RuntimeException e) {
			itemsReady = false;
		}
		return itemsReady;
	}

	public static void badge(GuiGraphicsExtractor context, ItemStack stack, int x, int panelY) {
		if (stack == null) {
			return;
		}
		context.item(stack, x, panelY + (Theme.HEADER_HEIGHT - Theme.ICON) / 2);
	}

	public static int titleX(int panelX, int badgeWidth) {
		return panelX + Theme.PADDING + (badgeWidth > 0 ? badgeWidth + Theme.GUTTER : 0);
	}

	public static int headerTextY(int panelY) {
		return panelY + (Theme.HEADER_HEIGHT - 8) / 2;
	}

	public static void masthead(GuiGraphicsExtractor context, Font font, Component title, Component stat,
			int panelX, int panelY, int panelWidth, int badgeWidth) {
		int textY = headerTextY(panelY);
		int right = panelX + panelWidth - Theme.PADDING;
		if (stat != null) {
			right -= font.width(stat);
			context.text(font, stat, right, textY, Theme.TEXT_SECONDARY, false);
			right -= Theme.GUTTER;
		}
		int titleX = titleX(panelX, badgeWidth);
		String shown = title.getString();
		while (!shown.isEmpty()
			&& titleX + Wordmark.width(font, shown, Wordmark.TITLE_TRACKING) > right) {
			shown = shown.substring(0, shown.offsetByCodePoints(shown.length(), -1));
		}
		Wordmark.draw(context, font, shown, titleX, textY, Theme.TEXT_PRIMARY,
			Wordmark.TITLE_TRACKING);
	}

	public static void scrollbar(GuiGraphicsExtractor context, int x, int trackTop, int trackHeight,
			int contentHeight, double scroll) {
		int max = Math.max(0, contentHeight - trackHeight);
		if (max <= 0 || trackHeight <= 0 || contentHeight <= 0) {
			return;
		}
		Shape.pill(context, x, trackTop, x + Theme.SCROLLBAR_WIDTH, trackTop + trackHeight,
			Theme.alpha(Theme.BORDER, 0x80));
		int thumbHeight = Math.max(20, trackHeight * trackHeight / contentHeight);
		int thumbY = trackTop
			+ (int) ((trackHeight - thumbHeight) * (Math.clamp(scroll, 0, max) / max));
		Shape.pill(context, x, thumbY + 1, x + Theme.SCROLLBAR_WIDTH, thumbY + thumbHeight - 1,
			Theme.mix(Theme.ACCENT, Theme.PANEL, 0.45));
	}
}
