package fr.imero.mercuriale.client.screen;

import fr.imero.mercuriale.theme.AeternumTheme;

public final class Theme {
	public static int BACKDROP;
	public static int PANEL;
	public static int BORDER;
	public static int ROW_HOVER;
	public static int TEXT_PRIMARY;
	public static int TEXT_SECONDARY;
	public static int TEXT_STALE;
	public static int ACCENT;

	public static int SURFACE;
	public static int SURFACE_HOVER;
	public static int TEXT_DISABLED;

	public static int SWITCH_OFF;
	public static int SWITCH_KNOB;
	public static int SWITCH_KNOB_ON;

	public static int RADIUS;
	public static int STROKE;
	public static AeternumTheme.Shadow SHADOW = AeternumTheme.Shadow.NONE;
	public static AeternumTheme.Rule RULE = AeternumTheme.Rule.TAB;
	public static AeternumTheme.Density DENSITY = AeternumTheme.Density.NORMAL;
	public static AeternumTheme.TextShadow TEXT_SHADOW = AeternumTheme.TextShadow.WORLD;
	public static AeternumTheme.Selection SELECTION = AeternumTheme.Selection.MARKER;
	public static int HUD_RADIUS;
	public static int SELECTED;

	public static int SETTING_ROW_HEIGHT;
	public static int SETTING_SECTION_GAP;
	public static int GUTTER;
	public static int PADDING;
	public static int HEADER_HEIGHT;
	public static int TOOLBAR_HEIGHT;
	public static int FOOTER_HEIGHT;
	public static int SECTION_GAP;
	public static int SEARCH_FIELD_HEIGHT;
	public static int FIELD_HEIGHT;
	public static int TOOLBAR_BAND;
	public static int BUTTON_HEIGHT;

	public static final int SWITCH_WIDTH = 22;
	public static final int SWITCH_HEIGHT = 10;

	public static final int ICON = 16;
	public static final int SCROLLBAR_WIDTH = 3;

	public static final int PANEL_RULE = 2;

	public static final int FOOTER_BUTTON_WIDTH = 70;

	public static final int SWATCH_WIDTH = 34;
	public static final int SWATCH_HEIGHT = 10;

	private Theme() {
	}

	private static final int SWATCH_PREVIEW = 9;
	private static final double CONTRAST_TOWARD_TEXT = 0.6;
	private static final double CONTRAST_TOWARD_BACKGROUND = 0.35;
	private static final double SELECTED_MIX = 0.22;

	private record Spacing(int row, int sectionGap, int gutter, int padding, int header, int toolbar, int footer, int gap,
			int search, int field, int button) {
	}

	private static final Spacing COMPACT = new Spacing(20, 6, 6, 8, 28, 20, 22, 6, 12, 16, 14);
	private static final Spacing NORMAL = new Spacing(24, 10, 8, 12, 34, 22, 24, 10, 14, 18, 16);
	private static final Spacing AIRY = new Spacing(28, 14, 10, 16, 40, 24, 28, 14, 16, 20, 18);

	private static int appliedRevision = Integer.MIN_VALUE;

	static {
		apply(AeternumTheme.Preset.AETERNUM.palette(), AeternumTheme.Style.DEFAULT);
	}

	public static void sync() {
		AeternumTheme.refresh();
		int revision = AeternumTheme.revision();
		if (revision != appliedRevision) {
			appliedRevision = revision;
			apply(AeternumTheme.palette(), AeternumTheme.style());
		}
	}

	public static void apply(AeternumTheme.Palette palette, AeternumTheme.Style style) {
		int background = palette.background();
		int surface = palette.surface();
		int border = palette.border();
		int accent = palette.accent();
		int text = palette.text();
		int muted = palette.muted();

		double lift = (style.contrast() - AeternumTheme.Style.CONTRAST_DEFAULT)
			/ (double) AeternumTheme.Style.CONTRAST_DEFAULT;
		int secondary = lift >= 0 ? mix(muted, text, lift * CONTRAST_TOWARD_TEXT)
			: mix(muted, background, -lift * CONTRAST_TOWARD_BACKGROUND);

		BACKDROP = alpha(background, percent(style.backdrop()));
		PANEL = alpha(mix(background, surface, 0.25), percent(style.panel()));
		BORDER = opaque(border);
		ROW_HOVER = opaque(mix(surface, accent, 0.06));
		TEXT_PRIMARY = opaque(text);
		TEXT_SECONDARY = opaque(secondary);
		TEXT_STALE = opaque(mix(secondary, background, 0.45));
		ACCENT = opaque(accent);
		SURFACE = opaque(surface);
		SURFACE_HOVER = opaque(mix(surface, accent, 0.14));
		TEXT_DISABLED = opaque(mix(secondary, background, 0.55));
		SELECTED = opaque(mix(surface, accent, SELECTED_MIX));
		SWITCH_OFF = opaque(border);
		SWITCH_KNOB = opaque(mix(text, muted, 0.45));
		SWITCH_KNOB_ON = opaque(mix(background, surface, 0.35));

		RADIUS = style.radius();
		STROKE = style.stroke();
		SHADOW = style.shadow();
		RULE = style.rule();
		DENSITY = style.density();
		TEXT_SHADOW = style.textShadow();
		SELECTION = style.selection();
		HUD_RADIUS = style.roundHud() ? RADIUS : 0;
		space(switch (DENSITY) {
			case COMPACT -> COMPACT;
			case AIRY -> AIRY;
			case NORMAL -> NORMAL;
		});
	}

	private static void space(Spacing spacing) {
		SETTING_ROW_HEIGHT = spacing.row();
		SETTING_SECTION_GAP = spacing.sectionGap();
		GUTTER = spacing.gutter();
		PADDING = spacing.padding();
		HEADER_HEIGHT = spacing.header();
		TOOLBAR_HEIGHT = spacing.toolbar();
		FOOTER_HEIGHT = spacing.footer();
		SECTION_GAP = spacing.gap();
		SEARCH_FIELD_HEIGHT = spacing.search();
		FIELD_HEIGHT = spacing.field();
		TOOLBAR_BAND = FIELD_HEIGHT + SECTION_GAP * 2;
		BUTTON_HEIGHT = spacing.button();
	}

	public static boolean worldShadow(boolean local) {
		return AeternumTheme.globalLook() ? TEXT_SHADOW != AeternumTheme.TextShadow.NONE : local;
	}

	public static boolean panelShadow(boolean local) {
		return AeternumTheme.globalLook() ? TEXT_SHADOW == AeternumTheme.TextShadow.ALL : local;
	}

	private static int percent(int value) {
		return (int) Math.round(Math.clamp(value, 0, 100) * 2.55);
	}

	public static int swatchPreview() {
		return SWATCH_PREVIEW;
	}

	public static int opaque(int rgb) {
		return 0xFF000000 | (rgb & 0xFFFFFF);
	}

	public static int scale(int rgb, double factor) {
		int out = 0;
		for (int shift = 0; shift <= 16; shift += 8) {
			int channel = (rgb >> shift) & 0xFF;
			out |= (Math.clamp((int) Math.round(channel * factor), 0, 255) & 0xFF) << shift;
		}
		return out;
	}

	public static int rgb(int argb) {
		return argb & 0xFFFFFF;
	}

	public static int alpha(int argb, int a) {
		return (Math.clamp(a, 0, 255) << 24) | (argb & 0xFFFFFF);
	}

	public static int mix(int from, int to, double t) {
		double k = Math.clamp(t, 0.0, 1.0);
		int out = 0;
		for (int shift = 0; shift <= 24; shift += 8) {
			int a = (from >> shift) & 0xFF;
			int b = (to >> shift) & 0xFF;
			out |= ((int) Math.round(a + (b - a) * k) & 0xFF) << shift;
		}
		return out;
	}
}
