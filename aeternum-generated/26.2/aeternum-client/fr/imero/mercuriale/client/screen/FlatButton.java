package fr.imero.mercuriale.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class FlatButton extends AbstractButton {
	private final Consumer<FlatButton> onPress;

	private boolean primary;

	private boolean shiftHeld;

	public FlatButton(int x, int y, int width, int height, Component message, Consumer<FlatButton> onPress) {
		super(x, y, width, height, message);
		this.onPress = onPress;
	}

	public FlatButton primary() {
		return primary(true);
	}

	public FlatButton primary(boolean on) {
		this.primary = on;
		return this;
	}

	public boolean shiftHeld() {
		return shiftHeld;
	}

	@Override
	public void onPress(InputWithModifiers input) {
		shiftHeld = input != null && input.hasShiftDown();
		onPress.accept(this);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		boolean hot = active && (isHovered() || isFocused());
		Shape.fill(context, getX(), getY(), getRight(), getBottom(), hot ? Theme.SURFACE_HOVER : Theme.SURFACE);
		if (hot) {
			Shape.ring(context, getX(), getY(), getRight(), getBottom(), Theme.ACCENT);
		} else {
			Shape.outline(context, getX(), getY(), getRight(), getBottom(), Theme.BORDER);
		}
		int color = !active ? Theme.TEXT_DISABLED
			: (primary || hot ? Theme.ACCENT : Theme.TEXT_PRIMARY);
		Minecraft client = Minecraft.getInstance();
		if (client != null) {
			context.centeredText(client.font, getMessage(), getX() + width / 2,
				getY() + (height - 9) / 2, color);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput builder) {
		defaultButtonNarrationText(builder);
	}
}
