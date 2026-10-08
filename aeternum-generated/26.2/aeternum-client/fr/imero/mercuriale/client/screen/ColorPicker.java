package fr.imero.mercuriale.client.screen;

import fr.imero.mercuriale.theme.AeternumTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public final class ColorPicker {
	private static final int SV_WIDTH = 108;
	private static final int SV_HEIGHT = 72;
	private static final int HUE_WIDTH = 12;
	private static final int TITLE_HEIGHT = 12;
	private static final int FIELD_HEIGHT = 18;
	private static final int TEXT_HEIGHT = 8;
	private static final int QUICK_SIZE = 10;
	private static final int QUICK_GAP = 2;
	private static final int PADDING = 8;
	private static final int GUTTER = 6;

	public static final int WIDTH = PADDING * 2 + SV_WIDTH + GUTTER + HUE_WIDTH;

	private static final int DRAG_NONE = 0;
	private static final int DRAG_SV = 1;
	private static final int DRAG_HUE = 2;

	private boolean open;
	private int x;
	private int y;
	private int width;
	private int height;
	private Component title = Component.empty();
	private IntConsumer sink;

	private float hue;
	private float saturation;
	private float value;

	private String hex = "000000";
	private boolean hexFocused;
	private int drag = DRAG_NONE;

	private final List<Integer> quick = new ArrayList<>();

	public boolean isOpen() {
		return open;
	}

	public void open(Component label, int rgb, int anchorX, int anchorY, int screenWidth,
			int screenHeight, List<Integer> quickColors, IntConsumer consumer) {
		this.title = label;
		this.sink = consumer;
		this.quick.clear();
		if (quickColors != null) {
			this.quick.addAll(quickColors);
		}
		this.width = PADDING * 2 + SV_WIDTH + GUTTER + HUE_WIDTH;
		int quickRows = this.quick.isEmpty() ? 0 : (QUICK_SIZE + GUTTER);
		this.height = PADDING * 2 + TITLE_HEIGHT + SV_HEIGHT + GUTTER + FIELD_HEIGHT + quickRows;
		this.x = Math.clamp(anchorX, 4, Math.max(4, screenWidth - width - 4));
		this.y = Math.clamp(anchorY, 4, Math.max(4, screenHeight - height - 4));
		setColor(rgb, true);
		this.hexFocused = false;
		this.drag = DRAG_NONE;
		this.open = true;
	}

	public void close() {
		open = false;
		sink = null;
		drag = DRAG_NONE;
		hexFocused = false;
	}

	public int color() {
		return AeternumTheme.fromHsv(hue, saturation, value);
	}

	private void setColor(int rgb, boolean resetHue) {
		float[] hsv = AeternumTheme.toHsv(rgb);
		if (resetHue || hsv[1] > 0.0f) {
			hue = hsv[0];
		}
		saturation = hsv[1];
		value = hsv[2];
		hex = AeternumTheme.hex(rgb);
	}

	private void emit() {
		int rgb = color();
		hex = AeternumTheme.hex(rgb);
		if (sink != null) {
			sink.accept(rgb);
		}
	}

	private int svX() {
		return x + PADDING;
	}

	private int svY() {
		return y + PADDING + TITLE_HEIGHT;
	}

	private int hueX() {
		return svX() + SV_WIDTH + GUTTER;
	}

	private int fieldY() {
		return svY() + SV_HEIGHT + GUTTER;
	}

	private int quickY() {
		return fieldY() + FIELD_HEIGHT + GUTTER;
	}

	private boolean inside(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	public void render(GuiGraphicsExtractor context, Font font, int mouseX, int mouseY) {
		if (!open) {
			return;
		}
		Shape.shadow(context, x - 1, y - 1, x + width + 1, y + height + 1);
		Shape.fill(context, x - 1, y - 1, x + width + 1, y + height + 1, Theme.opaque(Theme.PANEL));
		Shape.ring(context, x - 1, y - 1, x + width + 1, y + height + 1, Theme.BORDER);

		Wordmark.draw(context, font, title.getString(), x + PADDING, y + PADDING,
			Theme.ACCENT, Wordmark.LABEL_TRACKING);

		int left = svX();
		int top = svY();
		for (int column = 0; column < SV_WIDTH; column++) {
			float columnSaturation = column / (float) (SV_WIDTH - 1);
			int bright = Theme.opaque(AeternumTheme.fromHsv(hue, columnSaturation, 1.0f));
			context.fillGradient(left + column, top, left + column + 1, top + SV_HEIGHT,
				bright, 0xFF000000);
		}
		context.fill(left - 1, top - 1, left + SV_WIDTH + 1, top, Theme.BORDER);
		context.fill(left - 1, top + SV_HEIGHT, left + SV_WIDTH + 1, top + SV_HEIGHT + 1, Theme.BORDER);
		context.fill(left - 1, top, left, top + SV_HEIGHT, Theme.BORDER);
		context.fill(left + SV_WIDTH, top, left + SV_WIDTH + 1, top + SV_HEIGHT, Theme.BORDER);

		int markerX = left + Math.round(saturation * (SV_WIDTH - 1));
		int markerY = top + Math.round((1.0f - value) * (SV_HEIGHT - 1));
		int ring = value > 0.55f && saturation < 0.75f ? 0xFF000000 : 0xFFFFFFFF;
		context.fill(markerX - 2, markerY, markerX + 3, markerY + 1, ring);
		context.fill(markerX, markerY - 2, markerX + 1, markerY + 3, ring);

		int hx = hueX();
		int segments = 6;
		int segmentHeight = SV_HEIGHT / segments;
		for (int segment = 0; segment < segments; segment++) {
			int segmentTop = top + segment * segmentHeight;
			int segmentBottom = segment == segments - 1 ? top + SV_HEIGHT : segmentTop + segmentHeight;
			int from = Theme.opaque(AeternumTheme.fromHsv(segment * 60.0f, 1.0f, 1.0f));
			int to = Theme.opaque(AeternumTheme.fromHsv((segment + 1) * 60.0f, 1.0f, 1.0f));
			context.fillGradient(hx, segmentTop, hx + HUE_WIDTH, segmentBottom, from, to);
		}
		context.fill(hx - 1, top - 1, hx + HUE_WIDTH + 1, top, Theme.BORDER);
		context.fill(hx - 1, top + SV_HEIGHT, hx + HUE_WIDTH + 1, top + SV_HEIGHT + 1, Theme.BORDER);
		context.fill(hx - 1, top, hx, top + SV_HEIGHT, Theme.BORDER);
		context.fill(hx + HUE_WIDTH, top, hx + HUE_WIDTH + 1, top + SV_HEIGHT, Theme.BORDER);

		int hueMarker = top + Math.round(hue / 360.0f * (SV_HEIGHT - 1));
		context.fill(hx - 2, hueMarker, hx + HUE_WIDTH + 2, hueMarker + 1, Theme.TEXT_PRIMARY);

		int fieldTop = fieldY();
		Shape.fill(context, left, fieldTop, left + FIELD_HEIGHT, fieldTop + FIELD_HEIGHT, Theme.BORDER);
		Shape.fill(context, left + 1, fieldTop + 1, left + FIELD_HEIGHT - 1, fieldTop + FIELD_HEIGHT - 1,
			Shape.inner(1), Theme.opaque(color()));

		int textX = left + FIELD_HEIGHT + GUTTER;
		int fieldRight = hueX() + HUE_WIDTH;
		int fieldBottom = fieldTop + FIELD_HEIGHT;
		int fieldBorder = hexFocused ? Theme.ACCENT : Theme.BORDER;
		Shape.fill(context, textX, fieldTop, fieldRight, fieldBottom, Theme.SURFACE);
		if (hexFocused) {
			Shape.ring(context, textX, fieldTop, fieldRight, fieldBottom, fieldBorder);
		} else {
			Shape.outline(context, textX, fieldTop, fieldRight, fieldBottom, fieldBorder);
		}
		String shown = "#" + hex + (hexFocused ? "_" : "");
		context.text(font, shown, textX + FlatField.PADDING, fieldTop + (FIELD_HEIGHT - TEXT_HEIGHT) / 2,
			hexFocused ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY, false);

		if (!quick.isEmpty()) {
			int quickTop = quickY();
			for (int index = 0; index < quick.size(); index++) {
				int swatchX = left + index * (QUICK_SIZE + QUICK_GAP);
				if (swatchX + QUICK_SIZE > fieldRight) {
					break;
				}
				boolean hot = mouseX >= swatchX && mouseX < swatchX + QUICK_SIZE
					&& mouseY >= quickTop && mouseY < quickTop + QUICK_SIZE;
				Shape.fill(context, swatchX - 1, quickTop - 1, swatchX + QUICK_SIZE + 1,
					quickTop + QUICK_SIZE + 1, hot ? Theme.ACCENT : Theme.BORDER);
				Shape.fill(context, swatchX, quickTop, swatchX + QUICK_SIZE, quickTop + QUICK_SIZE,
					Shape.inner(1), Theme.opaque(quick.get(index)));
			}
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!open) {
			return false;
		}
		if (!inside(mouseX, mouseY)) {
			close();
			return true;
		}
		if (button != 0) {
			return true;
		}
		int left = svX();
		int top = svY();
		if (mouseX >= left && mouseX < left + SV_WIDTH && mouseY >= top && mouseY < top + SV_HEIGHT) {
			drag = DRAG_SV;
			hexFocused = false;
			updateSv(mouseX, mouseY);
			return true;
		}
		int hx = hueX();
		if (mouseX >= hx && mouseX < hx + HUE_WIDTH && mouseY >= top && mouseY < top + SV_HEIGHT) {
			drag = DRAG_HUE;
			hexFocused = false;
			updateHue(mouseY);
			return true;
		}
		int fieldTop = fieldY();
		if (mouseY >= fieldTop && mouseY < fieldTop + FIELD_HEIGHT) {
			hexFocused = mouseX >= left + FIELD_HEIGHT + GUTTER;
			return true;
		}
		if (!quick.isEmpty()) {
			int quickTop = quickY();
			if (mouseY >= quickTop && mouseY < quickTop + QUICK_SIZE) {
				int index = (int) ((mouseX - left) / (QUICK_SIZE + QUICK_GAP));
				if (index >= 0 && index < quick.size()) {
					setColor(quick.get(index), true);
					emit();
					return true;
				}
			}
		}
		hexFocused = false;
		return true;
	}

	public boolean mouseDragged(double mouseX, double mouseY) {
		if (!open || drag == DRAG_NONE) {
			return false;
		}
		if (drag == DRAG_SV) {
			updateSv(mouseX, mouseY);
		} else {
			updateHue(mouseY);
		}
		return true;
	}

	public boolean mouseReleased() {
		if (!open || drag == DRAG_NONE) {
			return false;
		}
		drag = DRAG_NONE;
		return true;
	}

	private void updateSv(double mouseX, double mouseY) {
		saturation = (float) Math.clamp((mouseX - svX()) / (SV_WIDTH - 1.0), 0.0, 1.0);
		value = 1.0f - (float) Math.clamp((mouseY - svY()) / (SV_HEIGHT - 1.0), 0.0, 1.0);
		emit();
	}

	private void updateHue(double mouseY) {
		hue = (float) Math.clamp((mouseY - svY()) / (SV_HEIGHT - 1.0), 0.0, 1.0) * 360.0f;
		emit();
	}

	public boolean keyPressed(int keyCode) {
		if (!open) {
			return false;
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			close();
			return true;
		}
		if (!hexFocused) {
			return false;
		}
		if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
			if (!hex.isEmpty()) {
				hex = hex.substring(0, hex.length() - 1);
			}
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			commitHex();
			hexFocused = false;
			return true;
		}
		return true;
	}

	public boolean charTyped(char typed) {
		if (!open || !hexFocused) {
			return false;
		}
		if (Character.digit(typed, 16) < 0 || hex.length() >= 6) {
			return true;
		}
		hex = hex + Character.toUpperCase(typed);
		if (hex.length() == 6) {
			commitHex();
		}
		return true;
	}

	private void commitHex() {
		if (!AeternumTheme.valid(hex)) {
			hex = AeternumTheme.hex(color());
			return;
		}
		int rgb = AeternumTheme.parse(hex, color());
		setColor(rgb, false);
		if (sink != null) {
			sink.accept(rgb);
		}
	}
}
