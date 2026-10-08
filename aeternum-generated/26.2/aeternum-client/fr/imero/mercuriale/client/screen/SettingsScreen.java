package fr.imero.mercuriale.client.screen;

import fr.imero.mercuriale.client.lang.AeternumLanguage;
import fr.imero.mercuriale.theme.AeternumTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public abstract class SettingsScreen extends Screen {
	protected static final String MOD = "mercuriale";

	private static final int SCREEN_MARGIN = 12;
	private static final int PANEL_MIN_WIDTH = 300;
	private static final int PANEL_MAX_WIDTH = 960;
	private static final int PANEL_MIN_HEIGHT = 220;

	private static final double SIDEBAR_FRACTION = 0.2;
	private static final int SIDEBAR_MIN = 112;
	private static final int SIDEBAR_MAX = 172;
	private static final int SIDE_PADDING = 8;
	private static final int SIDE_MOD_HEIGHT = 16;
	private static final int SIDE_PAGE_HEIGHT = 14;
	private static final int SIDE_INDENT = 8;
	private static final int SIDE_INSET = 4;
	private static final int CHEVRON = 7;
	private static final Identifier CHEVRON_DOWN = Identifier.fromNamespaceAndPath("aeternum",
		"textures/gui/icon/chevron_down.png");
	private static final Identifier CHEVRON_RIGHT = Identifier.fromNamespaceAndPath("aeternum",
		"textures/gui/icon/chevron_right.png");

	private static final int DETAILS_MIN_PANEL = 560;
	private static final String ELLIPSIS = "…";
	private static final double DETAILS_FRACTION = 0.3;
	private static final int DETAILS_MIN = 150;
	private static final int DETAILS_MAX = 240;
	private static final double PREVIEW_FRACTION = 0.55;
	private static final int PREVIEW_MAX = 170;

	private static final int READABLE_WIDTH = 440;
	private static final int ROW_GAP = 6;
	private static final int DESCRIPTION_LINE = 10;
	private static final int DESCRIPTION_TOP = 2;
	private static final int HEADING_HEIGHT = 26;
	private static final int PAGE_BAND = 24;
	private static final int PAGE_LABEL_Y = 9;

	protected static final int SECTION_HEADER = 18;
	protected static final int TOOLTIP_WIDTH = 230;
	private static final int SAMPLE_BAR_WIDTH = 9;
	private static final int SAMPLE_BAR_HEIGHT = 3;

	private static final BooleanSupplier ALWAYS = () -> true;

	private static final String THEME_SECTION = "aeternum.config.section.theme";
	private static final String STYLE_OWNER = "vernis";
	private static final String STYLE_PAGE = "vernis.config.section.style";
	private static final String PAGE_REQUEST = "aeternum.settings.page";

	protected final Screen parent;
	private final Item badgeItem;
	private ItemStack badge;

	private record Requirement(BooleanSupplier gate, String labelKey) {
	}

	protected interface Placeable {
		void place(int x, int width);
	}

	protected static final class Row {
		final String key;
		final AbstractWidget widget;
		final String haystack;
		final List<Requirement> requirements = new ArrayList<>();
		BooleanSupplier enabled = ALWAYS;
		boolean folded;
		boolean wide;
		Component disabledOverride;
		int virtualY;
		int slotX;
		int slotWidth;
		int slotHeight;
		int screenY;
		boolean heading;
		boolean hidden;
		List<FormattedCharSequence> tip = List.of();
		boolean shown = true;

		Row(String key, AbstractWidget widget, String haystack) {
			this.key = key;
			this.widget = widget;
			this.haystack = haystack;
		}

		boolean active() {
			if (!enabled.getAsBoolean()) {
				return false;
			}
			for (Requirement requirement : requirements) {
				if (!requirement.gate().getAsBoolean()) {
					return false;
				}
			}
			return true;
		}

		boolean canBeDisabled() {
			return enabled != ALWAYS || !requirements.isEmpty();
		}

		Component disabledText() {
			for (Requirement requirement : requirements) {
				if (!requirement.gate().getAsBoolean()) {
					return Component.translatable("aeternum.config.requires",
						Component.translatable(requirement.labelKey()));
				}
			}
			return disabledOverride != null ? disabledOverride : Component.translatable("aeternum.config.disabled");
		}

		public AbstractWidget widget() {
			return widget;
		}
	}

	private record Section(String key, int virtualY, int count) {
	}

	private enum SideKind {
		MOD,
		GROUP,
		PAGE
	}

	private record SideEntry(SideKind kind, String key, String label, int virtualY, int height, boolean current,
			int depth) {
	}

	private static final Map<String, String> PAGES = new java.util.HashMap<>();
	private static final int SCROLLBAR_HIT = 8;

	private boolean draggingScrollbar;

	protected interface TooltipSource {
		List<FormattedCharSequence> tooltip(Font font);
	}

	protected record Declared(Row row) {
		public Declared dependsOn(BooleanSupplier gate) {
			row.enabled = gate;
			return this;
		}

		public Declared requires(String labelKey, BooleanSupplier gate) {
			row.requirements.add(new Requirement(gate, labelKey));
			return this;
		}

		public Declared disabledSays(String key) {
			row.disabledOverride = Component.translatable(key);
			return this;
		}

		public Declared wide() {
			row.wide = true;
			return this;
		}

		public Declared themed() {
			row.hidden = AeternumTheme.globalLook();
			return this;
		}
	}

	private final Map<String, List<Row>> sections = new LinkedHashMap<>();
	private final Map<String, Component> sectionLabels = new LinkedHashMap<>();
	private final Map<String, String> sectionGroups = new LinkedHashMap<>();
	private final Map<String, Component> groupLabels = new LinkedHashMap<>();
	private final List<Row> allRows = new ArrayList<>();
	private final List<Section> visibleSections = new ArrayList<>();
	private final List<SideEntry> sideEntries = new ArrayList<>();

	private String currentSection;
	private String currentGroup;
	private String currentPage;
	private int contentHeight;
	protected double scroll;
	private double sideScroll;
	private int sideHeight;

	protected EditBox filterField;
	protected String filterText = "";
	private String status = "";

	protected final ColorPicker picker = new ColorPicker();
	protected final Dropdown dropdown = new Dropdown();

	protected int panelX;
	protected int panelY;
	protected int panelWidth;
	protected int panelHeight;
	private int sidebarWidth;
	private int detailsWidth;
	private int centerLeft;
	private int centerWidth;
	private int columnWidth;

	protected SettingsScreen(Screen parent, Component title, Item badge) {
		super(title);
		this.parent = parent;
		this.badgeItem = badge;
	}

	private ItemStack badge() {
		if (badge == null && badgeItem != null && Panel.itemsReady()) {
			badge = new ItemStack(badgeItem);
		}
		return badge;
	}

	protected abstract void buildRows(int x, int w);

	protected abstract void save();

	protected boolean showsThemeSection() {
		return true;
	}

	protected boolean showsSuite() {
		return true;
	}

	protected boolean ownsStyle() {
		return false;
	}

	protected void themeRows(int x, int w) {
	}

	protected void onThemeChanged() {
	}

	protected void footerButtons(int x, int y, int doneX) {
	}

	protected void renderBehindPanel(GuiGraphicsExtractor context, int mouseX, int mouseY) {
	}

	protected boolean hasPreview() {
		return false;
	}

	protected void renderPreview(GuiGraphicsExtractor context, int x, int y, int width, int height, int mouseX,
			int mouseY) {
	}

	protected int previewHeight(int available) {
		return Math.min(PREVIEW_MAX, (int) (available * PREVIEW_FRACTION));
	}

	protected Component hint() {
		String key = MOD + ".config.hint";
		return Language.getInstance().has(key) ? Component.translatable(key) : null;
	}

	protected int headerSeparatorY() {
		return panelY + Theme.HEADER_HEIGHT;
	}

	private int bodyTop() {
		return headerSeparatorY() + 1;
	}

	protected int contentTop() {
		return bodyTop() + PAGE_BAND;
	}

	protected int footerTop() {
		return panelY + panelHeight - Theme.FOOTER_HEIGHT;
	}

	protected int footerSeparatorY() {
		return footerTop() - 1;
	}

	protected int contentBottom() {
		return footerSeparatorY();
	}

	protected int contentViewHeight() {
		return contentBottom() - contentTop();
	}

	protected int maxScroll() {
		return Math.max(0, contentHeight - contentViewHeight());
	}

	protected boolean inContentBand(double mouseY) {
		return mouseY >= contentTop() && mouseY < contentBottom();
	}

	private int sidebarRight() {
		return panelX + sidebarWidth;
	}

	private int detailsLeft() {
		return panelX + panelWidth - detailsWidth;
	}

	private int centerRight() {
		return centerLeft + centerWidth;
	}

	private int sideListTop() {
		return bodyTop() + SIDE_PADDING + Theme.FIELD_HEIGHT + SIDE_PADDING;
	}

	private int sideListBottom() {
		return footerSeparatorY() - SIDE_PADDING;
	}

	@Override
	protected void init() {
		Theme.sync();
		picker.close();
		dropdown.close();
		sections.clear();
		sectionLabels.clear();
		sectionGroups.clear();
		groupLabels.clear();
		currentGroup = null;
		allRows.clear();
		visibleSections.clear();

		int natural = SIDEBAR_MAX + 1 + Theme.PADDING * 2 + READABLE_WIDTH + Theme.SCROLLBAR_WIDTH + 4
			+ (hasPreview() ? DETAILS_MAX + 1 : 0);
		panelWidth = Math.clamp(width - SCREEN_MARGIN * 2, PANEL_MIN_WIDTH, Math.clamp(natural, PANEL_MIN_WIDTH, PANEL_MAX_WIDTH));
		panelHeight = Math.max(PANEL_MIN_HEIGHT, height - SCREEN_MARGIN * 2);
		panelX = (width - panelWidth) / 2;
		panelY = (height - panelHeight) / 2;
		sidebarWidth = Math.clamp((int) (panelWidth * SIDEBAR_FRACTION), SIDEBAR_MIN, SIDEBAR_MAX);
		boolean detailsFit = hasPreview() && panelWidth >= DETAILS_MIN_PANEL;
		detailsWidth = detailsFit ? Math.clamp((int) (panelWidth * DETAILS_FRACTION), DETAILS_MIN, DETAILS_MAX) : 0;
		centerLeft = sidebarRight() + 1 + Theme.PADDING;
		int centerEnd = detailsWidth > 0 ? detailsLeft() - 1 : panelX + panelWidth;
		centerWidth = centerEnd - Theme.PADDING - Theme.SCROLLBAR_WIDTH - 4 - centerLeft;
		columnWidth = Math.min(centerWidth, READABLE_WIDTH);

		filterField = new FlatField(font, panelX + SIDE_PADDING, bodyTop() + SIDE_PADDING,
			sidebarWidth - SIDE_PADDING * 2, Theme.FIELD_HEIGHT, Component.translatable("aeternum.config.filter"));
		filterField.setHint(Component.translatable("aeternum.config.filter")
			.withStyle(s -> s.withColor(Theme.rgb(Theme.TEXT_SECONDARY))));
		filterField.setValue(filterText);
		filterField.setResponder(text -> {
			filterText = text;
			scroll = 0;
			applyFilter();
		});
		addRenderableWidget(filterField);
		setInitialFocus(filterField);

		buildRows(centerLeft, columnWidth);
		endGroup();
		if (showsThemeSection()) {
			buildThemeSection(centerLeft, columnWidth);
		}

		int footerX = panelX + Theme.PADDING;
		int buttonY = footerTop() + (Theme.FOOTER_HEIGHT - Theme.BUTTON_HEIGHT) / 2;
		int doneX = panelX + panelWidth - Theme.PADDING - Theme.FOOTER_BUTTON_WIDTH;
		footerButtons(footerX, buttonY, doneX);
		addRenderableWidget(new FlatButton(doneX, buttonY, Theme.FOOTER_BUTTON_WIDTH,
			Theme.BUTTON_HEIGHT, CommonComponents.GUI_DONE, b -> onClose()).primary());

		consumePageRequest();
		currentPage = PAGES.get(getClass().getName());
		applyFilter();
	}

	@Override
	protected void setInitialFocus() {
		if (filterField != null) {
			setInitialFocus(filterField);
			filterField.moveCursorToEnd(false);
		}
	}

	private void buildThemeSection(int x, int w) {
		section(THEME_SECTION);
		choice(x, w, "aeternum.config.language", () -> AeternumLanguage.choice().translationKey(), () -> {
			AeternumLanguage.cycle();
			refresh();
		});
		if (!ownsStyle() && lookOwnerReachable()) {
			heading("aeternum.config.heading.look");
			action(x, w, "aeternum.config.theme_link", () -> openSuitePage(STYLE_OWNER, THEME_SECTION));
			action(x, w, "aeternum.config.style_link", () -> openSuitePage(STYLE_OWNER, STYLE_PAGE));
			themeRows(x, w);
			return;
		}
		choiceText(x, w, "aeternum.config.theme", SettingsScreen::themeName, () -> {
			AeternumTheme.cycle();
			Theme.sync();
			onThemeChanged();
		});
		heading("aeternum.config.heading.colors");
		for (AeternumTheme.Role role : AeternumTheme.Role.values()) {
			add(role.translationKey(), new ColorRow(x, w, Component.translatable(role.translationKey()),
				() -> AeternumTheme.palette().role(role),
				rgb -> {
					AeternumTheme.setRole(role, rgb);
					Theme.sync();
				}));
		}
		action(x, w, "aeternum.config.theme_reset", () -> {
			AeternumTheme.reset();
			Theme.sync();
			onThemeChanged();
		});
		themeRows(x, w);
	}

	private static boolean lookOwnerReachable() {
		return AeternumTheme.globalLook()
			&& SuiteLinks.members().stream().anyMatch(member -> member.id().equals(STYLE_OWNER));
	}

	protected final void themeNotice(int x, int w) {
		if (AeternumTheme.globalLook()) {
			add("aeternum.config.follows_global", new NoticeRow(x, w,
				Component.translatable("aeternum.config.follows_global")));
		}
	}

	private void consumePageRequest() {
		String request = System.getProperty(PAGE_REQUEST);
		if (request != null && request.startsWith(MOD + ":")) {
			System.clearProperty(PAGE_REQUEST);
			PAGES.put(getClass().getName(), request.substring(MOD.length() + 1));
		}
	}

	protected final void openSuitePage(String id, String page) {
		if (minecraft == null) {
			return;
		}
		save();
		System.setProperty(PAGE_REQUEST, id + ":" + page);
		Screen target = SuiteLinks.open(id, parent);
		if (target == parent) {
			System.clearProperty(PAGE_REQUEST);
		}
		minecraft.setScreenAndShow(target);
	}

	protected final void styleRows(int x, int w) {
		heading("aeternum.config.heading.shape");
		slider(x, w, "aeternum.config.radius", 0, AeternumTheme.Style.RADIUS_MAX,
			() -> AeternumTheme.style().radius(), v -> restyle(s -> s.withRadius((int) v)),
			v -> v == 0 ? I18n.get("aeternum.config.radius.square") : unit(v, "aeternum.unit.px"));
		toggle(x, w, "aeternum.config.round_hud", AeternumTheme.style().roundHud(),
			v -> restyle(s -> s.withRoundHud(v)))
			.requires("aeternum.config.radius", () -> AeternumTheme.style().radius() > 0);
		slider(x, w, "aeternum.config.stroke", 0, AeternumTheme.Style.STROKE_MAX,
			() -> AeternumTheme.style().stroke(), v -> restyle(s -> s.withStroke((int) v)),
			v -> v == 0 ? I18n.get("aeternum.config.stroke.none") : unit(v, "aeternum.unit.px"));
		choice(x, w, "aeternum.config.rule", () -> AeternumTheme.style().rule().translationKey(),
			() -> restyle(s -> s.withRule(s.rule().next())));
		choice(x, w, "aeternum.config.shadow", () -> AeternumTheme.style().shadow().translationKey(),
			() -> restyle(s -> s.withShadow(s.shadow().next())));
		heading("aeternum.config.heading.layout");
		choice(x, w, "aeternum.config.density", () -> AeternumTheme.style().density().translationKey(), () -> {
			restyle(s -> s.withDensity(s.density().next()));
			refresh();
		});
		choice(x, w, "aeternum.config.selection", () -> AeternumTheme.style().selection().translationKey(),
			() -> restyle(s -> s.withSelection(s.selection().next())));
		heading("aeternum.config.heading.legibility");
		choice(x, w, "aeternum.config.text_shadow", () -> AeternumTheme.style().textShadow().translationKey(),
			() -> restyle(s -> s.withTextShadow(s.textShadow().next())));
		slider(x, w, "aeternum.config.contrast", 0, AeternumTheme.Style.CONTRAST_MAX,
			() -> AeternumTheme.style().contrast(), v -> restyle(s -> s.withContrast((int) v)), v -> v + " %");
		heading("aeternum.config.heading.transparency");
		slider(x, w, "aeternum.config.backdrop", 0, AeternumTheme.Style.BACKDROP_MAX,
			() -> AeternumTheme.style().backdrop(), v -> restyle(s -> s.withBackdrop((int) v)), v -> v + " %");
		slider(x, w, "aeternum.config.panel", AeternumTheme.Style.PANEL_MIN, AeternumTheme.Style.PANEL_MAX,
			() -> AeternumTheme.style().panel(), v -> restyle(s -> s.withPanel((int) v)), v -> v + " %");
		action(x, w, "aeternum.config.style_reset", () -> {
			AeternumTheme.resetStyle();
			Theme.sync();
			refresh();
		});
	}

	private static void restyle(UnaryOperator<AeternumTheme.Style> change) {
		AeternumTheme.setStyle(change.apply(AeternumTheme.style()));
		Theme.sync();
	}

	public static String themeName() {
		String saved = AeternumTheme.activeSaved();
		return saved != null ? saved : I18n.get(AeternumTheme.preset().translationKey());
	}

	protected final void refresh() {
		double keep = scroll;
		rebuildWidgets();
		scroll = Math.clamp(keep, 0, maxScroll());
		position();
	}

	protected final void section(String key) {
		section(key, Component.translatable(key));
	}

	protected final void section(String id, Component label) {
		currentSection = id;
		sections.computeIfAbsent(id, k -> new ArrayList<>());
		sectionLabels.putIfAbsent(id, label);
		if (currentGroup != null) {
			sectionGroups.putIfAbsent(id, currentGroup);
		}
	}

	protected final void group(String key) {
		group(key, Component.translatable(key));
	}

	protected final void group(String id, Component label) {
		currentGroup = id;
		groupLabels.putIfAbsent(id, label);
	}

	protected final void endGroup() {
		currentGroup = null;
	}

	private String pageLabel(String key) {
		String group = sectionGroups.get(key);
		String label = sectionLabels.get(key).getString();
		return group == null ? label : groupLabels.get(group).getString() + " · " + label;
	}

	protected final Declared add(String key, AbstractWidget widget) {
		return add(key, widget, "");
	}

	protected final Declared add(String key, AbstractWidget widget, String extraSearch) {
		String tipKey = key + ".tip";
		String haystack = (widget.getMessage().getString() + " " + I18n.get(key) + " "
			+ pageLabel(currentSection) + " "
			+ (Language.getInstance().has(tipKey) ? I18n.get(tipKey) : "") + " " + extraSearch)
			.toLowerCase(Locale.ROOT);
		Row row = new Row(key, widget, haystack);
		row.wide = widget instanceof TextRow || widget instanceof NoticeRow;
		sections.get(currentSection).add(row);
		allRows.add(row);
		addWidget(widget);
		return new Declared(row);
	}

	protected final void heading(String key) {
		heading(key, Component.translatable(key));
	}

	protected final void heading(String id, Component label) {
		Row row = new Row(id, new HeadingRow(label), "");
		row.heading = true;
		sections.get(currentSection).add(row);
		allRows.add(row);
	}

	protected final Declared toggle(int x, int w, String key, boolean initial, Consumer<Boolean> setter) {
		return add(key, new ToggleRow(x, w, Component.translatable(key), initial, setter));
	}

	protected final Declared slider(int x, int w, String key, int min, int max,
			DoubleSupplier getter, DoubleConsumer setter, IntFunction<String> format) {
		return add(key, new SliderRow(x, w, key, min, max, getter, setter, format));
	}

	protected final Declared choice(int x, int w, String key, Supplier<String> valueKey, Runnable cycle) {
		return add(key, new ChoiceRow(x, w, Component.translatable(key), valueKey, cycle));
	}

	protected final Declared choiceText(int x, int w, String key, Supplier<String> shown, Runnable cycle) {
		return add(key, new ChoiceRow(x, w, Component.translatable(key), shown, cycle, false));
	}

	protected final Declared text(int x, int w, String key, String value, Consumer<String> setter,
			Function<String, String> validator) {
		return add(key, new TextRow(x, w, Component.translatable(key), value, setter, validator), value);
	}

	protected final Declared dropdown(int x, int w, String key, Supplier<List<String>> labels, IntSupplier selected,
			IntConsumer select) {
		return add(key, new DropdownRow(x, w, Component.translatable(key), labels, selected, select));
	}

	public final void openDropdown(AbstractWidget anchor, List<String> labels, int selected, IntConsumer sink) {
		dropdown.open(font, labels, selected, anchor.getX(), anchor.getRight(), anchor.getY(), anchor.getBottom(),
			width, height, sink);
	}

	protected final Declared action(int x, int w, String key, Runnable onPress) {
		return add(key, new ActionRow(x, w, Component.translatable(key), b -> onPress.run()));
	}

	protected final Declared color(int x, int w, String key, IntSupplier getter, IntConsumer setter) {
		return add(key, new ColorRow(x, w, Component.translatable(key), getter, setter));
	}

	protected final Declared hexColor(int x, int w, String key, Supplier<String> getter,
			Consumer<String> setter) {
		return color(x, w, key, () -> AeternumTheme.parse(getter.get(), 0),
			rgb -> setter.accept(AeternumTheme.hex(rgb)));
	}

	protected static String unit(int value, String key) {
		return value + " " + I18n.get(key);
	}

	public final void openPicker(Component label, int rgb, AbstractWidget anchor, IntConsumer sink) {
		AeternumTheme.Palette palette = AeternumTheme.palette();
		List<Integer> quick = new ArrayList<>();
		for (AeternumTheme.Role role : AeternumTheme.Role.values()) {
			quick.add(palette.role(role));
		}
		picker.open(label, rgb, anchor.getRight() - ColorPicker.WIDTH,
			anchor.getY() + Theme.SETTING_ROW_HEIGHT, width, height, quick, sink);
	}

	private void applyFilter() {
		String query = filterText.trim().toLowerCase(Locale.ROOT);
		for (Row row : allRows) {
			row.shown = !row.hidden
				&& (row.heading ? query.isEmpty() : query.isEmpty() || row.haystack.contains(query));
		}
		relayout();
	}

	private boolean filtering() {
		return !filterText.trim().isEmpty();
	}

	protected final void openSection(String section, boolean open) {
		if (open) {
			PAGES.put(getClass().getName(), section);
			currentPage = section;
		}
	}

	private String firstPage() {
		for (Map.Entry<String, List<Row>> entry : sections.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				return entry.getKey();
			}
		}
		return null;
	}

	private void selectPage(String key) {
		openSection(key, true);
		if (filtering()) {
			filterText = "";
			filterField.setValue("");
		}
		scroll = 0;
		applyFilter();
	}

	private void relayout() {
		visibleSections.clear();
		if (currentPage == null || !sections.containsKey(currentPage) || sections.get(currentPage).isEmpty()) {
			currentPage = firstPage();
		}
		int y = 0;
		for (Map.Entry<String, List<Row>> entry : sections.entrySet()) {
			boolean onPage = filtering() || entry.getKey().equals(currentPage);
			List<Row> kept = new ArrayList<>();
			for (Row row : entry.getValue()) {
				row.folded = !onPage;
				if (onPage && row.shown) {
					kept.add(row);
				}
			}
			if (kept.isEmpty()) {
				continue;
			}
			if (filtering()) {
				if (!visibleSections.isEmpty()) {
					y += Theme.SETTING_SECTION_GAP;
				}
				visibleSections.add(new Section(entry.getKey(), y, kept.size()));
				y += SECTION_HEADER;
			} else {
				visibleSections.add(new Section(entry.getKey(), y, kept.size()));
			}
			for (Row row : kept) {
				row.tip = tipLines(row);
				int height = rowHeight(row);
				slot(row, centerLeft, columnWidth, y, height);
				y += height;
			}
		}
		contentHeight = y + Theme.PADDING;
		scroll = Math.clamp(scroll, 0, maxScroll());
		rebuildSidebar();
		position();
	}

	private static void slot(Row row, int x, int width, int y, int height) {
		row.slotX = x;
		row.slotWidth = width;
		row.virtualY = y;
		row.slotHeight = height;
	}

	private List<FormattedCharSequence> tipLines(Row row) {
		String tipKey = row.key + ".tip";
		if (row.heading || row.widget instanceof TooltipSource || !Language.getInstance().has(tipKey)) {
			return List.of();
		}
		return font.split(Component.translatable(tipKey), columnWidth);
	}

	private static int descriptionLines(Row row) {
		return Math.max(row.tip.size(), row.canBeDisabled() ? 1 : 0);
	}

	private static int rowHeight(Row row) {
		if (row.heading) {
			return HEADING_HEIGHT;
		}
		int lines = descriptionLines(row);
		return Theme.SETTING_ROW_HEIGHT + (lines > 0 ? DESCRIPTION_TOP + lines * DESCRIPTION_LINE : 0) + ROW_GAP;
	}

	private void drawDescription(GuiGraphicsExtractor context, Row row) {
		int lines = descriptionLines(row);
		if (row.heading || lines == 0) {
			return;
		}
		List<FormattedCharSequence> shown;
		int color;
		if (!row.widget.active && row.canBeDisabled()) {
			shown = font.split(row.disabledText(), columnWidth);
			color = Theme.TEXT_STALE;
		} else {
			shown = row.tip;
			color = row.widget.active ? Theme.TEXT_SECONDARY : Theme.TEXT_DISABLED;
		}
		int y = row.screenY + Theme.SETTING_ROW_HEIGHT + DESCRIPTION_TOP - 2;
		for (int i = 0; i < Math.min(lines, shown.size()); i++) {
			context.text(font, shown.get(i), row.slotX, y + i * DESCRIPTION_LINE, color, false);
		}
	}

	protected final void position() {
		int top = contentTop();
		int bottom = contentBottom();
		for (Row row : allRows) {
			if (!row.shown || row.folded) {
				row.widget.visible = false;
				continue;
			}
			if (row.widget instanceof Placeable placeable) {
				placeable.place(row.slotX, row.slotWidth);
			} else {
				if (row.widget.getX() != row.slotX) {
					row.widget.setX(row.slotX);
				}
				if (row.widget.getWidth() != row.slotWidth) {
					row.widget.setWidth(row.slotWidth);
				}
			}
			int screenY = top + row.virtualY - (int) scroll;
			row.screenY = screenY;
			row.widget.setY(screenY);
			row.widget.visible = screenY + row.slotHeight > top && screenY < bottom;
		}
	}

	protected final void scrollTo(String sectionKey) {
		selectPage(sectionKey);
	}

	private void rebuildSidebar() {
		sideEntries.clear();
		int y = 0;
		List<SuiteLinks.Member> members = showsSuite() ? SuiteLinks.members() : List.of();
		boolean inSuite = false;
		for (SuiteLinks.Member member : members) {
			if (member.id().equals(MOD)) {
				inSuite = true;
			}
		}
		if (!inSuite) {
			y = addPages(y);
		} else {
			for (SuiteLinks.Member member : members) {
				boolean current = member.id().equals(MOD);
				sideEntries.add(new SideEntry(SideKind.MOD, member.id(), member.name(), y, SIDE_MOD_HEIGHT, current, 0));
				y += SIDE_MOD_HEIGHT;
				if (current) {
					y = addPages(y);
				}
			}
		}
		sideHeight = y;
		sideScroll = Math.clamp(sideScroll, 0, maxSideScroll());
	}

	private int addPages(int y) {
		String openGroup = currentPage == null ? null : sectionGroups.get(currentPage);
		String lastGroup = null;
		for (Map.Entry<String, List<Row>> entry : sections.entrySet()) {
			if (entry.getValue().isEmpty()) {
				continue;
			}
			String group = sectionGroups.get(entry.getKey());
			if (group != null && !group.equals(lastGroup)) {
				sideEntries.add(new SideEntry(SideKind.GROUP, group, groupLabels.get(group).getString(), y,
					SIDE_PAGE_HEIGHT, group.equals(openGroup), 0));
				y += SIDE_PAGE_HEIGHT;
			}
			lastGroup = group;
			if (group != null && !group.equals(openGroup)) {
				continue;
			}
			boolean current = !filtering() && entry.getKey().equals(currentPage);
			sideEntries.add(new SideEntry(SideKind.PAGE, entry.getKey(), sectionLabels.get(entry.getKey()).getString(),
				y, SIDE_PAGE_HEIGHT, current, group != null ? 1 : 0));
			y += SIDE_PAGE_HEIGHT;
		}
		return y;
	}

	private String firstPageOf(String group) {
		for (Map.Entry<String, List<Row>> entry : sections.entrySet()) {
			if (!entry.getValue().isEmpty() && group.equals(sectionGroups.get(entry.getKey()))) {
				return entry.getKey();
			}
		}
		return null;
	}

	private int maxSideScroll() {
		return Math.max(0, sideHeight - (sideListBottom() - sideListTop()));
	}

	private boolean inSidebar(double mouseX, double mouseY) {
		return mouseX >= panelX && mouseX < sidebarRight() && mouseY >= sideListTop() && mouseY < sideListBottom();
	}

	private SideEntry sideEntryAt(double mouseX, double mouseY) {
		if (!inSidebar(mouseX, mouseY)) {
			return null;
		}
		for (SideEntry entry : sideEntries) {
			int top = sideListTop() + entry.virtualY() - (int) sideScroll;
			if (mouseY >= top && mouseY < top + entry.height()) {
				return entry;
			}
		}
		return null;
	}

	private boolean clickSidebar(double mouseX, double mouseY) {
		SideEntry entry = sideEntryAt(mouseX, mouseY);
		if (entry == null) {
			return false;
		}
		if (entry.kind() == SideKind.PAGE) {
			selectPage(entry.key());
		} else if (entry.kind() == SideKind.GROUP) {
			String first = firstPageOf(entry.key());
			if (first != null && !entry.current()) {
				selectPage(first);
			}
		} else if (!entry.key().equals(MOD) && minecraft != null) {
			save();
			minecraft.setScreenAndShow(SuiteLinks.open(entry.key(), parent));
		}
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
		if (dropdown.isOpen()) {
			return dropdown.mouseClicked(click.x(), click.y(), click.button());
		}
		if (picker.isOpen()) {
			return picker.mouseClicked(click.x(), click.y(), click.button());
		}
		if (click.button() == 0 && overScrollbar(click.x(), click.y())) {
			draggingScrollbar = true;
			dragScrollbar(click.y());
			return true;
		}
		if (click.button() == 0 && clickSidebar(click.x(), click.y())) {
			return true;
		}
		return super.mouseClicked(click, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
		if (draggingScrollbar) {
			dragScrollbar(click.y());
			return true;
		}
		if (picker.mouseDragged(click.x(), click.y())) {
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		if (draggingScrollbar) {
			draggingScrollbar = false;
			return true;
		}
		if (picker.mouseReleased()) {
			return true;
		}
		return super.mouseReleased(click);
	}

	private int scrollbarX() {
		return centerRight() + 4;
	}

	private boolean overScrollbar(double mouseX, double mouseY) {
		int x = scrollbarX();
		return maxScroll() > 0 && inContentBand(mouseY)
			&& mouseX >= x - SCROLLBAR_HIT + Theme.SCROLLBAR_WIDTH && mouseX < x + Theme.SCROLLBAR_WIDTH + 2;
	}

	private void dragScrollbar(double mouseY) {
		int track = contentViewHeight();
		int thumb = Math.max(20, track * track / Math.max(1, contentHeight));
		double travel = Math.max(1, track - thumb);
		double ratio = (mouseY - contentTop() - thumb / 2.0) / travel;
		scroll = Math.clamp(ratio * maxScroll(), 0, maxScroll());
		position();
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		if (dropdown.keyPressed(input.key())) {
			return true;
		}
		if (picker.keyPressed(input.key())) {
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharacterEvent input) {
		if (dropdown.isOpen()) {
			return true;
		}
		if (picker.charTyped((char) input.codepoint())) {
			return true;
		}
		return super.charTyped(input);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (dropdown.mouseScrolled(vertical)) {
			return true;
		}
		if (picker.isOpen()) {
			return true;
		}
		if (inSidebar(mouseX, mouseY) && maxSideScroll() > 0) {
			sideScroll = Math.clamp(sideScroll - vertical * SIDE_PAGE_HEIGHT, 0, maxSideScroll());
			return true;
		}
		if (inContentBand(mouseY) && maxScroll() > 0) {
			scroll = Math.clamp(scroll - vertical * Theme.SETTING_ROW_HEIGHT, 0, maxScroll());
			position();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public void onClose() {
		save();
		if (minecraft != null) {
			minecraft.setScreenAndShow(parent);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		Theme.sync();
		for (Row row : allRows) {
			row.widget.active = row.active();
		}
		boolean overlay = picker.isOpen() || dropdown.isOpen();
		int pointerX = overlay ? -1 : mouseX;
		int pointerY = overlay ? -1 : mouseY;

		Panel.backdrop(context, width, height);
		renderBehindPanel(context, pointerX, pointerY);
		Panel.frame(context, panelX, panelY, panelWidth, panelHeight);
		Panel.badge(context, badge(), panelX + Theme.PADDING, panelY);
		Panel.masthead(context, font, title, null, panelX, panelY, panelWidth, Theme.ICON);
		Panel.separator(context, panelX, headerSeparatorY(), panelWidth);
		context.fill(panelX + 1, footerSeparatorY(), panelX + panelWidth - 1, footerSeparatorY() + 1, Theme.BORDER);
		context.fill(sidebarRight(), bodyTop(), sidebarRight() + 1, footerSeparatorY(), Theme.BORDER);
		if (detailsWidth > 0) {
			context.fill(detailsLeft() - 1, bodyTop(), detailsLeft(), footerSeparatorY(), Theme.BORDER);
		}

		drawSidebar(context, pointerX, pointerY);
		drawPageBand(context);

		context.enableScissor(centerLeft - 4, contentTop(), centerRight() + 4, contentBottom());
		if (filtering()) {
			for (Section section : visibleSections) {
				int y = contentTop() + section.virtualY() - (int) scroll;
				Wordmark.draw(context, font, pageLabel(section.key()),
					centerLeft, y + 5, Theme.ACCENT, Wordmark.LABEL_TRACKING);
				context.fill(centerLeft, y + SECTION_HEADER - 5, centerRight(), y + SECTION_HEADER - 4,
					Theme.BORDER);
			}
		}
		for (Row row : allRows) {
			if (row.shown && row.widget.visible) {
				row.widget.extractRenderState(context, pointerX, pointerY, delta);
				drawDescription(context, row);
			}
		}
		if (visibleSections.isEmpty()) {
			Component empty = Component.translatable("aeternum.config.no_match");
			context.text(font, empty, centerLeft + (centerWidth - font.width(empty)) / 2,
				contentTop() + contentViewHeight() / 2 - 4, Theme.TEXT_SECONDARY, false);
		}
		context.disableScissor();

		super.extractRenderState(context, pointerX, pointerY, delta);

		drawFooterHint(context);
		Panel.scrollbar(context, scrollbarX(), contentTop(), contentViewHeight(), contentHeight, scroll);
		if (detailsWidth > 0) {
			drawDetails(context, pointerX, pointerY);
		}
		if (!overlay) {
			drawSettingTooltip(context, mouseX, mouseY);
		}
		picker.render(context, font, mouseX, mouseY);
		dropdown.render(context, font, mouseX, mouseY);
	}

	private void drawSidebar(GuiGraphicsExtractor context, int mouseX, int mouseY) {
		SideEntry hovered = sideEntryAt(mouseX, mouseY);
		int indent = sideEntries.stream().anyMatch(e -> e.kind() == SideKind.MOD) ? SIDE_INDENT : 0;
		context.enableScissor(panelX + 1, sideListTop(), sidebarRight(), sideListBottom());
		for (SideEntry entry : sideEntries) {
			int top = sideListTop() + entry.virtualY() - (int) sideScroll;
			if (top + entry.height() < sideListTop() || top > sideListBottom()) {
				continue;
			}
			boolean hot = entry == hovered && !(entry.kind() != SideKind.PAGE && entry.current());
			int inset = Shape.rounded() ? SIDE_INSET : 0;
			if (hot) {
				Shape.fill(context, panelX + 1 + inset, top, sidebarRight() - inset, top + entry.height(),
					Theme.ROW_HOVER);
			}
			int textY = top + (entry.height() - 8) / 2;
			if (entry.kind() == SideKind.MOD) {
				int color = entry.current() ? Theme.ACCENT : hot ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;
				context.text(font, fit(entry.label(), sidebarWidth - SIDE_PADDING * 2), panelX + SIDE_PADDING, textY,
					color, false);
				continue;
			}
			int left = panelX + SIDE_PADDING + indent + entry.depth() * SIDE_INDENT;
			int maxWidth = sidebarRight() - SIDE_PADDING - left;
			if (entry.kind() == SideKind.GROUP) {
				int color = entry.current() || hot ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;
				chevron(context, left, top + (entry.height() - CHEVRON) / 2, entry.current(), color);
				context.text(font, fit(entry.label(), maxWidth - CHEVRON - 4), left + CHEVRON + 4, textY, color, false);
				continue;
			}
			if (entry.current()) {
				Shape.select(context, panelX + 1 + inset, top, sidebarRight() - inset, top + entry.height(),
					panelX + 1 + inset);
			}
			int color = entry.current() || hot ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;
			context.text(font, fit(entry.label(), maxWidth), left, textY, color, false);
		}
		context.disableScissor();
		if (maxSideScroll() > 0) {
			Panel.scrollbar(context, sidebarRight() - Theme.SCROLLBAR_WIDTH - 1, sideListTop(),
				sideListBottom() - sideListTop(), sideHeight, sideScroll);
		}
	}

	private static void chevron(GuiGraphicsExtractor context, int x, int y, boolean open, int color) {
		context.blit(RenderPipelines.GUI_TEXTURED, open ? CHEVRON_DOWN : CHEVRON_RIGHT, x, y, 0, 0, CHEVRON, CHEVRON,
			CHEVRON, CHEVRON, color);
	}

	private void drawPageBand(GuiGraphicsExtractor context) {
		String label;
		if (filtering()) {
			long shown = allRows.stream().filter(row -> row.shown).count();
			label = I18n.get("aeternum.config.results", shown);
		} else {
			label = currentPage == null ? "" : pageLabel(currentPage);
		}
		Wordmark.draw(context, font, label, centerLeft, bodyTop() + PAGE_LABEL_Y, Theme.ACCENT,
			Wordmark.LABEL_TRACKING);
		context.fill(centerLeft, contentTop() - 4, centerRight(), contentTop() - 3, Theme.BORDER);
	}

	private void drawDetails(GuiGraphicsExtractor context, int mouseX, int mouseY) {
		int left = detailsLeft() + Theme.PADDING;
		int innerWidth = detailsWidth - Theme.PADDING * 2;
		int top = bodyTop() + Theme.PADDING;
		int bottom = footerSeparatorY() - Theme.PADDING;
		renderPreview(context, left, top, innerWidth, previewHeight(bottom - top), mouseX, mouseY);
	}

	private String fit(String text, int maxWidth) {
		if (font.width(text) <= maxWidth) {
			return text;
		}
		String cut = font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width(ELLIPSIS))).stripTrailing();
		return cut.isEmpty() ? "" : cut + ELLIPSIS;
	}

	private Row hoveredRow(int mouseX, int mouseY) {
		if (!inContentBand(mouseY)) {
			return null;
		}
		for (Row row : allRows) {
			if (row.shown && row.widget.visible && row.widget.isMouseOver(mouseX, mouseY)) {
				return row;
			}
		}
		return null;
	}

	protected final void status(String message) {
		status = message == null ? "" : message;
	}

	private void drawFooterHint(GuiGraphicsExtractor context) {
		int available = panelWidth - Theme.PADDING * 2 - Theme.FOOTER_BUTTON_WIDTH - Theme.GUTTER
			- footerReserve();
		int x = panelX + Theme.PADDING;
		int y = footerTop() + (Theme.FOOTER_HEIGHT - 8) / 2;
		if (!status.isEmpty()) {
			context.text(font, font.plainSubstrByWidth(status, Math.max(0, available)), x, y,
				Theme.ACCENT, false);
			return;
		}
	}

	protected int footerReserve() {
		return 0;
	}

	private void drawSettingTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
		Row row = hoveredRow(mouseX, mouseY);
		if (row == null) {
			return;
		}
		if (row.widget instanceof TooltipSource source) {
			List<FormattedCharSequence> custom = source.tooltip(font);
			if (!custom.isEmpty()) {
				context.setTooltipForNextFrame(font, custom, mouseX, mouseY);
			}
			return;
		}
	}

	protected static void drawRowLabel(GuiGraphicsExtractor context, Font font, Component label,
			int x, int y, int maxWidth, int color) {
		context.text(font, font.plainSubstrByWidth(label.getString(), Math.max(0, maxWidth)),
			x, y, color, false);
	}

	protected static void drawRowBackground(GuiGraphicsExtractor context, AbstractWidget widget, boolean hot) {
		if (hot) {
			Shape.fill(context, widget.getX() - 4, widget.getY(), widget.getRight() + 4,
				widget.getBottom(), Theme.ROW_HOVER);
		}
	}

	protected static void drawSwatch(GuiGraphicsExtractor context, int right, int y, int height,
			int rgb, boolean hot, boolean active) {
		int swatchX = right - Theme.SWATCH_WIDTH;
		int swatchY = y + (height - Theme.SWATCH_HEIGHT) / 2;
		Shape.fill(context, swatchX - 1, swatchY - 1, right + 1, swatchY + Theme.SWATCH_HEIGHT + 1,
			hot ? Theme.ACCENT : Theme.BORDER);
		Shape.fill(context, swatchX, swatchY, right, swatchY + Theme.SWATCH_HEIGHT, Shape.inner(1),
			active ? Theme.opaque(rgb) : Theme.SWITCH_OFF);
	}

	protected abstract class SettingButton extends AbstractButton {
		protected SettingButton(int x, int width, Component label) {
			super(x, 0, width, Theme.SETTING_ROW_HEIGHT, label);
		}

		protected SettingButton(int x, int width, int height, Component label) {
			super(x, 0, width, height, label);
		}

		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return super.isMouseOver(mouseX, mouseY) && inContentBand(mouseY);
		}

		protected boolean hot(int mouseX, int mouseY) {
			return active && (isMouseOver(mouseX, mouseY) || isFocused());
		}

		protected int labelColor() {
			return active ? Theme.TEXT_PRIMARY : Theme.TEXT_DISABLED;
		}

		protected int textY() {
			return getY() + (getHeight() - 8) / 2;
		}

		protected Font font() {
			return Minecraft.getInstance().font;
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput builder) {
			defaultButtonNarrationText(builder);
		}
	}

	protected class ToggleRow extends SettingButton {
		private final Consumer<Boolean> setter;
		private boolean value;

		public ToggleRow(int x, int width, Component label, boolean initial, Consumer<Boolean> setter) {
			super(x, width, label);
			this.value = initial;
			this.setter = setter;
		}

		@Override
		public void onPress(InputWithModifiers input) {
			value = !value;
			setter.accept(value);
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
			drawRowBackground(context, this, hot(mouseX, mouseY));
			int trackX = getRight() - Theme.SWITCH_WIDTH;
			int trackY = getY() + (getHeight() - Theme.SWITCH_HEIGHT) / 2;
			drawRowLabel(context, font(), getMessage(), getX(), textY(),
				trackX - getX() - Theme.GUTTER, labelColor());
			Shape.pill(context, trackX, trackY, trackX + Theme.SWITCH_WIDTH, trackY + Theme.SWITCH_HEIGHT,
				value && active ? Theme.ACCENT : Theme.SWITCH_OFF);
			int knobX = value ? trackX + Theme.SWITCH_WIDTH - 9 : trackX + 1;
			Shape.pill(context, knobX, trackY + 1, knobX + 8, trackY + Theme.SWITCH_HEIGHT - 1,
				value && active ? Theme.SWITCH_KNOB_ON : Theme.SWITCH_KNOB);
		}
	}

	protected class ChoiceRow extends SettingButton {
		private final Supplier<String> valueKey;
		private final Runnable cycle;
		private final boolean translate;

		public ChoiceRow(int x, int width, Component label, Supplier<String> valueKey, Runnable cycle) {
			this(x, width, label, valueKey, cycle, true);
		}

		public ChoiceRow(int x, int width, Component label, Supplier<String> valueKey, Runnable cycle,
				boolean translate) {
			super(x, width, label);
			this.valueKey = valueKey;
			this.cycle = cycle;
			this.translate = translate;
		}

		@Override
		public void onPress(InputWithModifiers input) {
			cycle.run();
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
			drawRowBackground(context, this, hot(mouseX, mouseY));
			Font font = font();
			String value = translate ? I18n.get(valueKey.get()) : valueKey.get();
			String shown = hot(mouseX, mouseY) ? value + " ›" : value;
			int valueWidth = font.width(shown);
			drawRowLabel(context, font, getMessage(), getX(), textY(),
				getWidth() - valueWidth - Theme.GUTTER, labelColor());
			context.text(font, shown, getRight() - valueWidth, textY(),
				active ? Theme.ACCENT : Theme.TEXT_DISABLED, false);
		}
	}

	protected class DropdownRow extends SettingButton {
		private final Supplier<List<String>> labels;
		private final IntSupplier selected;
		private final IntConsumer select;

		public DropdownRow(int x, int width, Component label, Supplier<List<String>> labels, IntSupplier selected,
				IntConsumer select) {
			super(x, width, label);
			this.labels = labels;
			this.selected = selected;
			this.select = select;
		}

		@Override
		public void onPress(InputWithModifiers input) {
			openDropdown(this, labels.get(), selected.getAsInt(), select);
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
			drawRowBackground(context, this, hot(mouseX, mouseY));
			Font font = font();
			List<String> options = labels.get();
			int index = selected.getAsInt();
			String value = index >= 0 && index < options.size() ? options.get(index) : "";
			int arrowX = getRight() - CHEVRON;
			String shown = fit(value, Math.max(0, (getWidth() - CHEVRON - 4) / 2));
			int valueWidth = font.width(shown);
			int color = active ? Theme.ACCENT : Theme.TEXT_DISABLED;
			drawRowLabel(context, font, getMessage(), getX(), textY(),
				getWidth() - valueWidth - CHEVRON - 4 - Theme.GUTTER, labelColor());
			context.text(font, shown, arrowX - 4 - valueWidth, textY(), color, false);
			chevron(context, arrowX, getY() + (getHeight() - CHEVRON) / 2, true, color);
		}
	}

	protected class ColorRow extends SettingButton {
		private final IntSupplier getter;
		private final IntConsumer setter;

		public ColorRow(int x, int width, Component label, IntSupplier getter, IntConsumer setter) {
			super(x, width, label);
			this.getter = getter;
			this.setter = setter;
		}

		@Override
		public void onPress(InputWithModifiers input) {
			openPicker(getMessage(), getter.getAsInt(), this, setter);
		}

		protected void drawSample(GuiGraphicsExtractor context, int x, int rgb) {
			int barY = getY() + (getHeight() - SAMPLE_BAR_HEIGHT) / 2;
			Shape.pill(context, x, barY, x + SAMPLE_BAR_WIDTH, barY + SAMPLE_BAR_HEIGHT, Theme.opaque(rgb));
		}

		protected int sampleWidth() {
			return 0;
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
			boolean hot = hot(mouseX, mouseY);
			drawRowBackground(context, this, hot);
			Font font = font();
			int rgb = getter.getAsInt() & 0xFFFFFF;
			drawSwatch(context, getRight(), getY(), getHeight(), rgb, hot, active);
			String hex = "#" + AeternumTheme.hex(rgb);
			int hexWidth = font.width(hex);
			int hexX = getRight() - Theme.SWATCH_WIDTH - Theme.GUTTER - hexWidth;
			context.text(font, hex, hexX, textY(),
				active ? Theme.TEXT_SECONDARY : Theme.TEXT_DISABLED, false);
			int labelRight = hexX - Theme.GUTTER;
			if (sampleWidth() > 0) {
				labelRight -= sampleWidth() + Theme.GUTTER;
				if (active) {
					drawSample(context, labelRight + Theme.GUTTER, rgb);
				}
			}
			drawRowLabel(context, font, getMessage(), getX(), textY(), labelRight - getX(), labelColor());
		}
	}

	protected class SliderRow extends AbstractSliderButton {
		private final String key;
		private final int min;
		private final int max;
		private final DoubleConsumer setter;
		private final IntFunction<String> format;

		public SliderRow(int x, int width, String key, int min, int max,
				DoubleSupplier getter, DoubleConsumer setter, IntFunction<String> format) {
			super(x, 0, width, Theme.SETTING_ROW_HEIGHT, Component.empty(),
				Math.clamp((getter.getAsDouble() - min) / (double) (max - min), 0.0, 1.0));
			this.key = key;
			this.min = min;
			this.max = max;
			this.setter = setter;
			this.format = format;
			updateMessage();
		}

		private int current() {
			return (int) Math.round(min + value * (max - min));
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.translatable(key));
		}

		@Override
		protected void applyValue() {
			setter.accept(current());
		}

		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return super.isMouseOver(mouseX, mouseY) && inContentBand(mouseY);
		}

		@Override
		public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
			boolean hot = active && (isMouseOver(mouseX, mouseY) || isFocused());
			drawRowBackground(context, this, hot);
			Font font = Minecraft.getInstance().font;
			String shown = format.apply(current());
			int valueWidth = font.width(shown);
			int textY = getY() + 4;
			drawRowLabel(context, font, getMessage(), getX(), textY,
				getWidth() - valueWidth - Theme.GUTTER,
				active ? Theme.TEXT_PRIMARY : Theme.TEXT_DISABLED);
			context.text(font, shown, getRight() - valueWidth, textY,
				active ? Theme.ACCENT : Theme.TEXT_DISABLED, false);

			int railLeft = getX() + 4;
			int railRight = getRight() - 4;
			int railY = getBottom() - 7;
			Shape.pill(context, railLeft, railY, railRight, railY + 2, Theme.SWITCH_OFF);
			int fillEnd = railLeft + (int) Math.round((getWidth() - 8) * value);
			Shape.pill(context, railLeft, railY, fillEnd, railY + 2,
				active ? Theme.ACCENT : Theme.TEXT_DISABLED);
			int knobX = Math.clamp(fillEnd - 2, railLeft, railRight - 4);
			Shape.pill(context, knobX, railY - 2, knobX + 4, railY + 4,
				active ? Theme.ACCENT : Theme.TEXT_DISABLED);
		}
	}

	protected class TextRow extends FlatField implements Placeable {
		private static final double FIELD_FRACTION = 0.55;

		private final Component label;
		private int labelX;
		private int labelWidth;
		private final Function<String, String> validator;

		public TextRow(int x, int width, Component label, String value, Consumer<String> setter,
				Function<String, String> validator) {
			super(Minecraft.getInstance().font, x + (int) (width * (1 - FIELD_FRACTION)), 0,
				(int) (width * FIELD_FRACTION), Theme.FIELD_HEIGHT, label);
			this.label = label;
			this.labelX = x;
			this.labelWidth = (int) (width * (1 - FIELD_FRACTION)) - Theme.GUTTER;
			this.validator = validator;
			setMaxLength(512);
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

		@Override
		public void setY(int y) {
			super.setY(y + (Theme.SETTING_ROW_HEIGHT - boxHeight()) / 2 + textOffset());
		}

		@Override
		public void place(int x, int width) {
			int labelArea = (int) (width * (1 - FIELD_FRACTION));
			labelX = x;
			labelWidth = labelArea - Theme.GUTTER;
			super.setX(x + labelArea + FlatField.PADDING);
			super.setWidth(Math.max(0, width - labelArea - FlatField.PADDING * 2));
		}

		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return super.isMouseOver(mouseX, mouseY) && inContentBand(mouseY);
		}

		@Override
		public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
			Font font = Minecraft.getInstance().font;
			drawRowLabel(context, font, label, labelX, getY(), labelWidth,
				active ? Theme.TEXT_PRIMARY : Theme.TEXT_DISABLED);
			invalid(error(getValue()) != null);
			super.extractWidgetRenderState(context, mouseX, mouseY, delta);
		}
	}

	protected class HeadingRow extends SettingButton {
		public HeadingRow(Component label) {
			super(0, 0, HEADING_HEIGHT, label);
		}

		@Override
		public void onPress(InputWithModifiers input) {
		}

		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return false;
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
			int textY = getY() + HEADING_HEIGHT - 16;
			Wordmark.draw(context, font(), getMessage().getString(), getX(), textY, Theme.TEXT_SECONDARY,
				Wordmark.LABEL_TRACKING);
			context.fill(getX(), getY() + HEADING_HEIGHT - 5, getRight(), getY() + HEADING_HEIGHT - 4,
				Theme.alpha(Theme.BORDER, 0xA0));
		}
	}

	protected class NoticeRow extends SettingButton {
		public NoticeRow(int x, int width, Component label) {
			super(x, width, label);
		}

		@Override
		public void onPress(InputWithModifiers input) {
		}

		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return false;
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
			drawRowLabel(context, font(), getMessage(), getX(), textY(), getWidth(), Theme.TEXT_STALE);
		}
	}

	protected class ActionRow extends FlatButton {
		public ActionRow(int x, int width, Component label, Consumer<FlatButton> onPress) {
			super(x, 0, width, Theme.SETTING_ROW_HEIGHT - 4, label, onPress);
		}

		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return super.isMouseOver(mouseX, mouseY) && inContentBand(mouseY);
		}
	}
}
