package fr.imero.mercuriale.client.screen;

import fr.imero.mercuriale.theme.AeternumTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class FlatField extends EditBox {
	public static final int PADDING = 6;

	private static final int TEXT_HEIGHT = 8;
	private static final double HOVER_MIX = 0.35;

	private final int boxHeight;
	private boolean invalid;

	public FlatField(Font font, int x, int y, int width, int height, Component message) {
		super(font, x + PADDING, y + (height - TEXT_HEIGHT) / 2, Math.max(0, width - PADDING * 2), TEXT_HEIGHT,
			message);
		this.boxHeight = height;
		setBordered(false);
	}

	public int boxX() {
		return getX() - PADDING;
	}

	public int boxY() {
		return getY() - textOffset();
	}

	public int boxWidth() {
		return getWidth() + PADDING * 2;
	}

	public int boxHeight() {
		return boxHeight;
	}

	protected int textOffset() {
		return (boxHeight - TEXT_HEIGHT) / 2;
	}

	public void setBox(int x, int y, int width) {
		setX(x + PADDING);
		setY(y + textOffset());
		setWidth(Math.max(0, width - PADDING * 2));
	}

	public FlatField invalid(boolean invalid) {
		this.invalid = invalid;
		return this;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return visible && mouseX >= boxX() && mouseX < boxX() + boxWidth()
			&& mouseY >= boxY() && mouseY < boxY() + boxHeight;
	}

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		int left = boxX();
		int top = boxY();
		int right = left + boxWidth();
		int bottom = top + boxHeight;
		boolean emphasised = invalid || isFocused() || isMouseOver(mouseX, mouseY);
		int border = invalid ? Theme.opaque(AeternumTheme.palette().bad())
			: isFocused() ? Theme.ACCENT
			: isMouseOver(mouseX, mouseY) ? Theme.mix(Theme.BORDER, Theme.ACCENT, HOVER_MIX)
			: Theme.BORDER;
		Shape.fill(context, left, top, right, bottom, active ? Theme.SURFACE : Theme.PANEL);
		if (emphasised) {
			Shape.ring(context, left, top, right, bottom, border);
		} else {
			Shape.outline(context, left, top, right, bottom, border);
		}
		super.extractWidgetRenderState(context, mouseX, mouseY, delta);
	}
}
