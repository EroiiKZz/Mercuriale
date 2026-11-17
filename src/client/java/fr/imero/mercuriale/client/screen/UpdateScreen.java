package fr.imero.mercuriale.client.screen;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class UpdateScreen extends Screen {
	private static final int BULLET = 9;
	private static final float TITLE_SCALE = 1.5f;

	private final String version;
	private final String notes;
	private final BooleanConsumer answer;
	private final List<List<FormattedCharSequence>> items = new ArrayList<>();

	private int left;
	private int top;
	private int cardWidth;
	private int cardHeight;

	private UpdateScreen(String version, String notes, BooleanConsumer answer) {
		super(Component.translatable("mercuriale.update.title", version));
		this.version = version;
		this.notes = notes;
		this.answer = answer;
	}

	public static Screen ask(String version, String notes, BooleanConsumer answer) {
		return new UpdateScreen(version, notes, answer);
	}

	@Override
	protected void init() {
		Theme.sync();
		cardWidth = Math.min(Theme.NARROW, width - 24);
		int inner = cardWidth - Theme.SPACE_6 * 2;
		items.clear();
		for (String raw : notes.split("\n")) {
			String line = raw.strip().replaceFirst("^[•\\-*]\\s*", "");
			if (!line.isEmpty()) {
				items.add(font.split(Component.literal(line), inner - Theme.SPACE_4 * 2 - BULLET));
			}
		}
		cardHeight = Theme.SPACE_6 + bodyHeight() + Theme.SPACE_6;
		left = (width - cardWidth) / 2;
		top = Math.max(12, (height - cardHeight) / 2);

		int right = left + cardWidth - Theme.SPACE_6;
		int buttonY = top + cardHeight - Theme.SPACE_6 - Theme.CONTROL;
		Component yes = Component.translatable("mercuriale.update.yes");
		int yesWidth = font.width(yes) + 28;
		addRenderableWidget(new FlatButton(right - yesWidth, buttonY, yesWidth, Theme.CONTROL, yes, b -> answer.accept(true))
			.primary());
		Component later = Component.translatable("mercuriale.update.later");
		int laterWidth = font.width(later) + 24;
		addRenderableWidget(new FlatButton(right - yesWidth - 6 - laterWidth, buttonY, laterWidth, Theme.CONTROL, later,
			b -> answer.accept(false)));
	}

	private int boxHeight() {
		if (items.isEmpty()) {
			return 0;
		}
		int lines = 0;
		for (List<FormattedCharSequence> item : items) {
			lines += Math.max(1, item.size());
		}
		return Theme.SPACE_4 * 2 + lines * Theme.LINE + (items.size() - 1) * Theme.SPACE_2;
	}

	private int bodyHeight() {
		int box = boxHeight();
		return 26 + 12 + Theme.LINE + 6 + box + (box > 0 ? 10 : 0) + Theme.LINE + 12 + Theme.CONTROL;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		Theme.sync();
		Panel.backdrop(context, width, height);
		Shape.window(context, left, top, left + cardWidth, top + cardHeight);
		int x = left + Theme.SPACE_6;
		int y = top + Theme.SPACE_6;
		context.pose().pushMatrix();
		context.pose().translate(x, y);
		context.pose().scale(TITLE_SCALE, TITLE_SCALE);
		context.text(font, title, 0, 0, Theme.TEXT_PRIMARY, false);
		context.pose().popMatrix();
		context.text(font, Component.translatable("mercuriale.update.current", installed(), minecraft()), x,
			y + 16, Theme.TEXT_SECONDARY, false);
		y += 26 + 12;
		context.text(font, Component.translatable("mercuriale.update.intro"), x, y, Theme.TEXT_SECONDARY, false);
		y += Theme.LINE + 6;
		int box = boxHeight();
		if (box > 0) {
			Shape.tile(context, x, y, left + cardWidth - Theme.SPACE_6, y + box);
			int cursor = y + Theme.SPACE_4;
			for (List<FormattedCharSequence> item : items) {
				Shape.pill(context, x + Theme.SPACE_4, cursor + 2, x + Theme.SPACE_4 + 4, cursor + 6, Theme.ACCENT);
				for (FormattedCharSequence line : item) {
					context.text(font, line, x + Theme.SPACE_4 + BULLET, cursor, Theme.TEXT_PRIMARY, false);
					cursor += Theme.LINE;
				}
				cursor += Theme.SPACE_2;
			}
			y += box + 10;
		}
		context.text(font, Component.translatable("mercuriale.update.question"), x, y, Theme.TEXT_PRIMARY,
			false);
		super.extractRenderState(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
			answer.accept(false);
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	private static String installed() {
		return FabricLoader.getInstance().getModContainer("mercuriale")
			.map(mod -> mod.getMetadata().getVersion().getFriendlyString().replaceFirst("\\+.*$", "")).orElse("?");
	}

	private static String minecraft() {
		return FabricLoader.getInstance().getModContainer("minecraft")
			.map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("?");
	}
}
