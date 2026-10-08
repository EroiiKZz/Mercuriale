package fr.imero.mercuriale.client.screen;

import fr.imero.mercuriale.client.PriceService;
import fr.imero.mercuriale.config.MercurialeConfig;
import fr.imero.mercuriale.theme.AeternumTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConfigScreen extends Screen {
	private static final String ENABLED = "mercuriale.config.enabled";
	private static final String BARGAINS = "mercuriale.config.bargains";
	private static final String CONTAINER_VALUE = "mercuriale.config.container_value";
	private static final String HINT = "mercuriale.config.hint";

	private static final int PANEL_MIN_WIDTH = 320;
	private static final int PANEL_MAX_WIDTH = 480;
	private static final int PANEL_MIN_HEIGHT = 200;
	private static final int PANEL_MARGIN = 20;
	private static final double PANEL_WIDTH_FRACTION = 0.55;
	private static final int TAB_MIN = 24;
	private static final int TAB_MAX = 72;

	private static final int SECTION_HEADER = 18;
	private static final int TIP_LEADING = 1;
	private static final int ROW_GAP = 4;
	private static final int ACTION_INSET = 2;
	private static final int HOVER_BLEED = 4;
	private static final int KNOB = 8;
	private static final double FIELD_FRACTION = 0.48;
	private static final int TEXT_LIMIT = 512;
	private static final int GLYPH_HEIGHT = 8;
	private static final int TITLE_TRACKING = 2;
	private static final int LABEL_TRACKING = 1;
	private static final int THUMB_MIN = 20;

	private record Section(Component label, int virtualY) {
	}

	private record Row(AbstractWidget widget, int virtualY, int offset, Component label,
			List<FormattedCharSequence> tip, BooleanSupplier gate) {
		boolean on() {
			return gate == null || gate.getAsBoolean();
		}
	}

	private final Screen parent;
	private final MercurialeConfig config = MercurialeConfig.get();
	private final List<Section> sections = new ArrayList<>();
	private final List<Row> rows = new ArrayList<>();
	private ItemStack badge;

	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;
	private int rowX;
	private int rowWidth;
	private int virtualCursor;
	private int contentHeight;
	private double scroll;

	public ConfigScreen(Screen parent) {
		super(Component.translatable("mercuriale.config.title"));
		this.parent = parent;
	}

	private int headerSeparatorY() {
		return panelY + Theme.HEADER_HEIGHT;
	}

	private int contentTop() {
		return headerSeparatorY() + 1 + Theme.SECTION_GAP;
	}

	private int footerTop() {
		return panelY + panelHeight - Theme.FOOTER_HEIGHT;
	}

	private int footerSeparatorY() {
		return footerTop() - Theme.SECTION_GAP - 1;
	}

	private int contentBottom() {
		return footerSeparatorY();
	}

	private int maxScroll() {
		return Math.max(0, contentHeight - (contentBottom() - contentTop()));
	}

	private boolean inContentBand(double mouseY) {
		return mouseY >= contentTop() && mouseY < contentBottom();
	}

	@Override
	protected void init() {
		Theme.sync();
		sections.clear();
		rows.clear();

		panelWidth = Math.min(Math.clamp((int) (width * PANEL_WIDTH_FRACTION), PANEL_MIN_WIDTH, PANEL_MAX_WIDTH),
			Math.max(PANEL_MIN_WIDTH, width - PANEL_MARGIN));
		panelHeight = Math.max(PANEL_MIN_HEIGHT, height - PANEL_MARGIN * 2);
		panelX = (width - panelWidth) / 2;
		panelY = (height - panelHeight) / 2;
		rowX = panelX + Theme.PADDING;
		rowWidth = panelWidth - Theme.PADDING * 2;
		virtualCursor = 0;

		BooleanSupplier on = () -> config.enabled;

		section("mercuriale.config.section.prices");
		toggle(ENABLED, () -> config.enabled, value -> config.enabled = value, null);
		toggle("mercuriale.config.market", () -> config.market, value -> config.market = value, on);
		toggle("mercuriale.config.shops", () -> config.shops, value -> config.shops = value, on);
		choice("mercuriale.config.amount", () -> Component.translatable(config.amount.translationKey()),
			() -> config.amount = config.amount.next(), on);
		action("mercuriale.config.reset_prices", () -> reset(config::resetPrices));

		section("mercuriale.config.section.bargains");
		toggle(BARGAINS, () -> config.bargains, value -> config.bargains = value, on);
		BooleanSupplier bargains = () -> config.enabled && config.bargains;
		text("mercuriale.config.auction_title_pattern", config.auctionTitlePattern,
			value -> config.auctionTitlePattern = value, MercurialeConfig::patternError, bargains);
		text("mercuriale.config.listing_price_pattern", config.listingPricePattern,
			value -> config.listingPricePattern = value, MercurialeConfig::patternError, bargains);
		action("mercuriale.config.reset_bargains", () -> reset(config::resetBargains));

		section("mercuriale.config.section.stock");
		toggle(CONTAINER_VALUE, () -> config.containerValue, value -> config.containerValue = value, on);
		text("mercuriale.config.server_menu_pattern", config.serverMenuPattern,
			value -> config.serverMenuPattern = value, MercurialeConfig::patternError,
			() -> config.enabled && config.containerValue);
		action("mercuriale.config.reset_stock", () -> reset(config::resetChestRadar));

		section("mercuriale.config.section.source");
		text("mercuriale.config.server", config.server, value -> config.server = value.strip(), null, null);
		text("mercuriale.config.url", config.url, value -> config.url = value.strip(),
			MercurialeConfig::urlError, null);

		contentHeight = virtualCursor + Theme.PADDING;
		scroll = Math.clamp(scroll, 0, maxScroll());

		int buttonY = footerTop() + (Theme.FOOTER_HEIGHT - Theme.BUTTON_HEIGHT) / 2;
		addRenderableWidget(new FlatButton(rowX + rowWidth - Theme.FOOTER_BUTTON_WIDTH, buttonY,
			Theme.FOOTER_BUTTON_WIDTH, Theme.BUTTON_HEIGHT, CommonComponents.GUI_DONE, this::onClose, true, false));

		place();
	}

	private void section(String key) {
		if (!sections.isEmpty()) {
			virtualCursor += Theme.SETTING_SECTION_GAP;
		}
		sections.add(new Section(Component.translatable(key), virtualCursor));
		virtualCursor += SECTION_HEADER;
	}

	private void toggle(String key, BooleanSupplier value, Consumer<Boolean> setter, BooleanSupplier gate) {
		add(key, new SettingButton(Component.translatable(key), () -> setter.accept(!value.getAsBoolean()),
			value, null), 0, null, gate);
	}

	private void choice(String key, Supplier<Component> shown, Runnable cycle, BooleanSupplier gate) {
		add(key, new SettingButton(Component.translatable(key), cycle, null, shown), 0, null, gate);
	}

	private void action(String key, Runnable press) {
		add(key, new FlatButton(rowX, 0, rowWidth, Theme.SETTING_ROW_HEIGHT - ACTION_INSET * 2,
			Component.translatable(key), press, false, true), ACTION_INSET, null, null);
	}

	private void text(String key, String value, Consumer<String> setter, Function<String, String> validator,
			BooleanSupplier gate) {
		int labelArea = (int) (rowWidth * (1 - FIELD_FRACTION));
		TextRow field = new TextRow(rowX + labelArea, rowWidth - labelArea, Component.translatable(key), value,
			setter, validator);
		add(key, field, (Theme.SETTING_ROW_HEIGHT - Theme.FIELD_HEIGHT) / 2, Component.translatable(key), gate);
	}

	private void add(String key, AbstractWidget widget, int offset, Component label, BooleanSupplier gate) {
		String tipKey = key + ".tip";
		List<FormattedCharSequence> tip = I18n.exists(tipKey)
			? font.split(Component.translatable(tipKey), rowWidth) : List.of();
		rows.add(new Row(widget, virtualCursor, offset, label, tip, gate));
		addWidget(widget);
		virtualCursor += Theme.SETTING_ROW_HEIGHT;
		if (!tip.isEmpty()) {
			virtualCursor += tip.size() * (font.lineHeight + TIP_LEADING) + ROW_GAP;
		}
	}

	private void place() {
		int top = contentTop();
		int bottom = contentBottom();
		for (Row row : rows) {
			int y = top + row.virtualY() - (int) scroll;
			row.widget().setY(y + row.offset());
			row.widget().visible = y + Theme.SETTING_ROW_HEIGHT > top && y < bottom;
		}
	}

	private void reset(Runnable action) {
		action.run();
		config.save();
		rebuildWidgets();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (inContentBand(mouseY) && maxScroll() > 0) {
			scroll = Math.clamp(scroll - vertical * Theme.SETTING_ROW_HEIGHT, 0, maxScroll());
			place();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public void onClose() {
		config.save();
		PriceService.forceRefresh();
		if (minecraft != null) {
			minecraft.setScreen(parent);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, width, height, Theme.BACKDROP);
		graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, Theme.PANEL);
		outline(graphics, panelX, panelY, panelX + panelWidth, panelY + panelHeight, Theme.BORDER);
		int tab = Math.clamp(panelWidth / 4, TAB_MIN, Math.min(TAB_MAX, panelWidth));
		graphics.fill(panelX, panelY, panelX + tab, panelY + Theme.PANEL_RULE, Theme.ACCENT);
		graphics.fill(panelX + Theme.PADDING, headerSeparatorY(), panelX + panelWidth - Theme.PADDING,
			headerSeparatorY() + 1, Theme.BORDER);
		graphics.fill(panelX + 1, footerSeparatorY(), panelX + panelWidth - 1, footerSeparatorY() + 1, Theme.BORDER);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		Theme.sync();
		for (Row row : rows) {
			boolean on = row.on();
			row.widget().active = on;
			if (row.widget() instanceof TextRow field) {
				field.setEditable(on);
				field.recolor();
			}
		}

		super.render(graphics, mouseX, mouseY, delta);

		masthead(graphics);
		Component hint = Component.translatable(HINT);
		int hintWidth = rowWidth - Theme.FOOTER_BUTTON_WIDTH - Theme.GUTTER;
		graphics.drawString(font, font.plainSubstrByWidth(hint.getString(), Math.max(0, hintWidth)), rowX,
			footerTop() + (Theme.FOOTER_HEIGHT - GLYPH_HEIGHT) / 2, Theme.TEXT_SECONDARY, false);

		graphics.enableScissor(panelX + 1, contentTop(), panelX + panelWidth - 1, contentBottom());
		int top = contentTop();
		for (Section section : sections) {
			int y = top + section.virtualY() - (int) scroll;
			tracked(graphics, font, section.label().getString(), rowX, y + 5, Theme.ACCENT, LABEL_TRACKING);
			graphics.fill(rowX, y + SECTION_HEADER - 5, rowX + rowWidth, y + SECTION_HEADER - 4, Theme.BORDER);
		}
		for (Row row : rows) {
			int y = top + row.virtualY() - (int) scroll;
			boolean on = row.on();
			if (row.label() != null) {
				int labelWidth = row.widget().getX() - rowX - Theme.GUTTER;
				label(graphics, font, row.label(), rowX, y + (Theme.SETTING_ROW_HEIGHT - GLYPH_HEIGHT) / 2, labelWidth,
					on ? Theme.TEXT_PRIMARY : Theme.TEXT_DISABLED);
			}
			if (row.widget().visible) {
				row.widget().render(graphics, mouseX, mouseY, delta);
			}
			int tipY = y + Theme.SETTING_ROW_HEIGHT;
			for (FormattedCharSequence line : row.tip()) {
				graphics.drawString(font, line, rowX, tipY, on ? Theme.TEXT_SECONDARY : Theme.TEXT_DISABLED, false);
				tipY += font.lineHeight + TIP_LEADING;
			}
		}
		graphics.disableScissor();

		scrollbar(graphics, panelX + panelWidth - Theme.SCROLLBAR_WIDTH - 2, contentTop(),
			contentBottom() - contentTop());
	}

	private void masthead(GuiGraphics graphics) {
		if (badge == null) {
			badge = new ItemStack(Items.EMERALD);
		}
		graphics.renderItem(badge, rowX, panelY + (Theme.HEADER_HEIGHT - Theme.ICON) / 2);
		int titleX = rowX + Theme.ICON + Theme.GUTTER;
		int right = panelX + panelWidth - Theme.PADDING;
		String shown = title.getString();
		while (!shown.isEmpty() && titleX + trackedWidth(font, shown, TITLE_TRACKING) > right) {
			shown = shown.substring(0, shown.offsetByCodePoints(shown.length(), -1));
		}
		tracked(graphics, font, shown, titleX, panelY + (Theme.HEADER_HEIGHT - GLYPH_HEIGHT) / 2, Theme.TEXT_PRIMARY,
			TITLE_TRACKING);
	}

	private void scrollbar(GuiGraphics graphics, int x, int trackTop, int trackHeight) {
		int max = maxScroll();
		if (max <= 0 || trackHeight <= 0 || contentHeight <= 0) {
			return;
		}
		graphics.fill(x, trackTop, x + Theme.SCROLLBAR_WIDTH, trackTop + trackHeight, Theme.alpha(Theme.BORDER, 0x80));
		int thumbHeight = Math.max(THUMB_MIN, trackHeight * trackHeight / contentHeight);
		int thumbY = trackTop + (int) ((trackHeight - thumbHeight) * (Math.clamp(scroll, 0, max) / max));
		graphics.fill(x, thumbY + 1, x + Theme.SCROLLBAR_WIDTH, thumbY + thumbHeight - 1,
			Theme.mix(Theme.ACCENT, Theme.PANEL, 0.45));
	}

	private static void outline(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
		graphics.fill(left, top, right, top + 1, color);
		graphics.fill(left, bottom - 1, right, bottom, color);
		graphics.fill(left, top, left + 1, bottom, color);
		graphics.fill(right - 1, top, right, bottom, color);
	}

	private static void label(GuiGraphics graphics, Font font, Component label, int x, int y, int maxWidth,
			int color) {
		graphics.drawString(font, font.plainSubstrByWidth(label.getString(), Math.max(0, maxWidth)), x, y, color,
			false);
	}

	private static void tracked(GuiGraphics graphics, Font font, String text, int x, int y, int color, int tracking) {
		int pen = x;
		for (int i = 0; i < text.length(); ) {
			int codePoint = text.codePointAt(i);
			String glyph = new String(Character.toChars(codePoint)).toUpperCase(Locale.ROOT);
			graphics.drawString(font, glyph, pen, y, color, false);
			pen += font.width(glyph) + tracking;
			i += Character.charCount(codePoint);
		}
	}

	private static int trackedWidth(Font font, String text, int tracking) {
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

	private class SettingButton extends Button {
		private final BooleanSupplier state;
		private final Supplier<Component> shown;

		SettingButton(Component label, Runnable press, BooleanSupplier state, Supplier<Component> shown) {
			super(rowX, 0, rowWidth, Theme.SETTING_ROW_HEIGHT, label, button -> press.run(), DEFAULT_NARRATION);
			this.state = state;
			this.shown = shown;
		}

		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return super.isMouseOver(mouseX, mouseY) && inContentBand(mouseY);
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
			Font font = Minecraft.getInstance().font;
			boolean hot = active && (isMouseOver(mouseX, mouseY) || isFocused());
			if (hot) {
				graphics.fill(getX() - HOVER_BLEED, getY(), getRight() + HOVER_BLEED, getBottom(), Theme.ROW_HOVER);
			}
			int textY = getY() + (getHeight() - GLYPH_HEIGHT) / 2;
			int right = getRight();
			if (state != null) {
				boolean on = state.getAsBoolean() && active;
				int trackX = right - Theme.SWITCH_WIDTH;
				int trackY = getY() + (getHeight() - Theme.SWITCH_HEIGHT) / 2;
				graphics.fill(trackX, trackY, trackX + Theme.SWITCH_WIDTH, trackY + Theme.SWITCH_HEIGHT,
					on ? Theme.ACCENT : Theme.SWITCH_OFF);
				int knobX = state.getAsBoolean() ? trackX + Theme.SWITCH_WIDTH - KNOB - 1 : trackX + 1;
				graphics.fill(knobX, trackY + 1, knobX + KNOB, trackY + Theme.SWITCH_HEIGHT - 1,
					on ? Theme.SWITCH_KNOB_ON : Theme.SWITCH_KNOB);
				right = trackX - Theme.GUTTER;
			} else {
				Component value = shown.get();
				int valueWidth = font.width(value);
				graphics.drawString(font, value, right - valueWidth, textY,
					!active ? Theme.TEXT_DISABLED : hot ? Theme.ACCENT : Theme.TEXT_PRIMARY, false);
				right -= valueWidth + Theme.GUTTER;
			}
			label(graphics, font, getMessage(), getX(), textY, right - getX(),
				active ? Theme.TEXT_PRIMARY : Theme.TEXT_DISABLED);
		}
	}

	private class FlatButton extends Button {
		private final boolean primary;
		private final boolean scrolls;

		FlatButton(int x, int y, int width, int height, Component label, Runnable press, boolean primary,
				boolean scrolls) {
			super(x, y, width, height, label, button -> press.run(), DEFAULT_NARRATION);
			this.primary = primary;
			this.scrolls = scrolls;
		}

		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return super.isMouseOver(mouseX, mouseY) && (!scrolls || inContentBand(mouseY));
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
			Font font = Minecraft.getInstance().font;
			boolean hot = active && (isMouseOver(mouseX, mouseY) || isFocused());
			graphics.fill(getX(), getY(), getRight(), getBottom(), hot ? Theme.SURFACE_HOVER : Theme.SURFACE);
			outline(graphics, getX(), getY(), getRight(), getBottom(), hot ? Theme.ACCENT : Theme.BORDER);
			int color = !active ? Theme.TEXT_DISABLED : primary || hot ? Theme.ACCENT : Theme.TEXT_PRIMARY;
			String text = font.plainSubstrByWidth(getMessage().getString(), Math.max(0, getWidth() - 8));
			graphics.drawString(font, text, getX() + (getWidth() - font.width(text)) / 2,
				getY() + (getHeight() - GLYPH_HEIGHT) / 2, color, false);
		}
	}

	private class TextRow extends EditBox {
		private final Function<String, String> validator;

		TextRow(int x, int width, Component label, String value, Consumer<String> setter,
				Function<String, String> validator) {
			super(Minecraft.getInstance().font, x, 0, width, Theme.FIELD_HEIGHT, label);
			this.validator = validator;
			setMaxLength(TEXT_LIMIT);
			setValue(value == null ? "" : value);
			moveCursorToStart(false);
			setResponder(text -> {
				if (error(text) == null) {
					setter.accept(text);
				}
			});
		}

		private String error(String text) {
			return validator == null ? null : validator.apply(text);
		}

		void recolor() {
			setTextColor(error(getValue()) != null ? Theme.opaque(AeternumTheme.palette().bad()) : Theme.TEXT_PRIMARY);
			setTextColorUneditable(Theme.TEXT_DISABLED);
		}

		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return super.isMouseOver(mouseX, mouseY) && inContentBand(mouseY);
		}
	}
}
