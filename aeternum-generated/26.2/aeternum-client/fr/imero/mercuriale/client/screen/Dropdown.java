package fr.imero.mercuriale.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public final class Dropdown {
	private static final int ITEM_HEIGHT = 16;
	private static final int PADDING = 8;
	private static final int MAX_VISIBLE = 10;
	private static final int MARGIN = 4;
	private static final int MARKER_INSET = 2;
	private static final String ELLIPSIS = "…";

	private final List<String> options = new ArrayList<>();
	private IntConsumer sink;
	private boolean open;
	private int selected;
	private int x;
	private int y;
	private int width;
	private int height;
	private int scroll;

	public boolean isOpen() {
		return open;
	}

	public void open(Font font, List<String> labels, int current, int anchorLeft, int anchorRight, int anchorTop,
			int anchorBottom, int screenWidth, int screenHeight, IntConsumer consumer) {
		options.clear();
		options.addAll(labels);
		if (options.isEmpty()) {
			return;
		}
		sink = consumer;
		selected = current;
		int widest = 0;
		for (String option : options) {
			widest = Math.max(widest, font.width(option));
		}
		int maxWidth = Math.max(60, screenWidth - MARGIN * 2);
		width = Math.min(maxWidth, Math.max(anchorRight - anchorLeft, widest + PADDING * 2));
		int visible = Math.min(options.size(), MAX_VISIBLE);
		height = visible * ITEM_HEIGHT;
		x = Math.clamp(anchorRight - width, MARGIN, Math.max(MARGIN, screenWidth - width - MARGIN));
		y = anchorBottom + height + MARGIN <= screenHeight ? anchorBottom : Math.max(MARGIN, anchorTop - height);
		scroll = Math.clamp(current - visible / 2, 0, Math.max(0, options.size() - visible));
		open = true;
	}

	public void close() {
		open = false;
		sink = null;
	}

	private int visibleCount() {
		return Math.min(options.size(), MAX_VISIBLE);
	}

	private boolean inside(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private int indexAt(double mouseX, double mouseY) {
		if (!inside(mouseX, mouseY)) {
			return -1;
		}
		int index = scroll + (int) ((mouseY - y) / ITEM_HEIGHT);
		return index < options.size() ? index : -1;
	}

	public void render(GuiGraphicsExtractor context, Font font, int mouseX, int mouseY) {
		if (!open) {
			return;
		}
		Shape.shadow(context, x - 1, y - 1, x + width + 1, y + height + 1);
		Shape.fill(context, x - 1, y - 1, x + width + 1, y + height + 1, Theme.opaque(Theme.PANEL));
		Shape.ring(context, x - 1, y - 1, x + width + 1, y + height + 1, Theme.ACCENT);
		int hovered = indexAt(mouseX, mouseY);
		int inset = Shape.rounded() ? MARKER_INSET : 0;
		for (int row = 0; row < visibleCount(); row++) {
			int index = scroll + row;
			int top = y + row * ITEM_HEIGHT;
			if (index == hovered) {
				Shape.fill(context, x + inset, top + (inset > 0 ? 1 : 0), x + width - inset,
					top + ITEM_HEIGHT - (inset > 0 ? 1 : 0), Shape.inner(1), Theme.ROW_HOVER);
			}
			if (index == selected) {
				Shape.select(context, x + inset, top + (inset > 0 ? 1 : 0), x + width - inset,
					top + ITEM_HEIGHT - (inset > 0 ? 1 : 0), x + inset);
			}
			int color = index == selected ? Theme.ACCENT : index == hovered ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;
			context.text(font, fit(font, options.get(index), width - PADDING * 2), x + PADDING,
				top + (ITEM_HEIGHT - 8) / 2, color, false);
		}
		if (options.size() > visibleCount()) {
			Panel.scrollbar(context, x + width - Theme.SCROLLBAR_WIDTH - 1, y, height,
				options.size() * ITEM_HEIGHT, scroll * ITEM_HEIGHT);
		}
	}

	private static String fit(Font font, String text, int maxWidth) {
		if (font.width(text) <= maxWidth) {
			return text;
		}
		String cut = font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width(ELLIPSIS))).stripTrailing();
		return cut.isEmpty() ? "" : cut + ELLIPSIS;
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!open) {
			return false;
		}
		int index = button == 0 ? indexAt(mouseX, mouseY) : -1;
		IntConsumer target = sink;
		close();
		if (index >= 0 && target != null) {
			target.accept(index);
		}
		return true;
	}

	public boolean mouseScrolled(double vertical) {
		if (!open) {
			return false;
		}
		scroll = Math.clamp(scroll - (int) Math.signum(vertical), 0, Math.max(0, options.size() - visibleCount()));
		return true;
	}

	public boolean keyPressed(int keyCode) {
		if (!open) {
			return false;
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			close();
		}
		return true;
	}
}
