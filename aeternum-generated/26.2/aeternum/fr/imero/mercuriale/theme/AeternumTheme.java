package fr.imero.mercuriale.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.imero.mercuriale.AeternumFiles;
import fr.imero.mercuriale.AeternumPaths;
import fr.imero.mercuriale.Mercuriale;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class AeternumTheme {
	public static final String FILE_NAME = "theme.json";
	private static final String LEGACY_FILE_NAME = "aeternum.json";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String LOOK_OWNER = "vernis";

	private static Boolean globalLook;

	public enum Role {
		BACKGROUND("background"),
		SURFACE("surface"),
		BORDER("border"),
		ACCENT("accent"),
		SECONDARY("secondary"),
		TEXT("text"),
		MUTED("muted"),
		GOOD("good"),
		FAIR("fair"),
		BAD("bad");

		private final String key;

		Role(String key) {
			this.key = key;
		}

		public String key() {
			return key;
		}

		public String translationKey() {
			return "aeternum.role." + key;
		}
	}

	public record Palette(int background, int surface, int border, int accent, int secondary,
			int text, int muted, int good, int fair, int bad) {

		public int role(Role role) {
			return switch (role) {
				case BACKGROUND -> background;
				case SURFACE -> surface;
				case BORDER -> border;
				case ACCENT -> accent;
				case SECONDARY -> secondary;
				case TEXT -> text;
				case MUTED -> muted;
				case GOOD -> good;
				case FAIR -> fair;
				case BAD -> bad;
			};
		}

		public Palette with(Role role, int rgb) {
			int value = rgb & 0xFFFFFF;
			return new Palette(
				role == Role.BACKGROUND ? value : background,
				role == Role.SURFACE ? value : surface,
				role == Role.BORDER ? value : border,
				role == Role.ACCENT ? value : accent,
				role == Role.SECONDARY ? value : secondary,
				role == Role.TEXT ? value : text,
				role == Role.MUTED ? value : muted,
				role == Role.GOOD ? value : good,
				role == Role.FAIR ? value : fair,
				role == Role.BAD ? value : bad);
		}
	}

	public enum Preset {
		AETERNUM("aeternum", new Palette(0x0F0B06, 0x1E1710, 0x3B3125, 0xF2B441, 0x5FB8A6,
			0xF0E7D6, 0x9C8D77, 0x6FA84F, 0xF2B441, 0xDD6A55)),
		NOCTURNE("nocturne", new Palette(0x060A0F, 0x111A22, 0x273A45, 0x5FB8A6, 0x6FC3E8,
			0xDFEAEC, 0x8AA0A6, 0x62B48F, 0x6FC3E8, 0xE0705F)),
		VESPER("vesper", new Palette(0x0A0710, 0x18121F, 0x342942, 0xB07CE8, 0xE0B44A,
			0xE7E0F0, 0x9A8CAE, 0x76B98A, 0xE0B44A, 0xDD6A70)),
		CUSTOM("custom", null);

		private final String key;
		private final Palette palette;

		Preset(String key, Palette palette) {
			this.key = key;
			this.palette = palette;
		}

		public String key() {
			return key;
		}

		public Palette palette() {
			return palette;
		}

		public String translationKey() {
			return "aeternum.theme." + key;
		}

		public Preset next() {
			Preset[] all = values();
			return all[(ordinal() + 1) % all.length];
		}

		public static Preset of(String key) {
			if (key != null) {
				for (Preset preset : values()) {
					if (preset.key.equalsIgnoreCase(key)) {
						return preset;
					}
				}
			}
			return AETERNUM;
		}
	}

	public enum Rule {
		TAB("tab"),
		FULL("full"),
		NONE("none");

		private final String key;

		Rule(String key) {
			this.key = key;
		}

		public String key() {
			return key;
		}

		public String translationKey() {
			return "aeternum.rule." + key;
		}

		public Rule next() {
			Rule[] all = values();
			return all[(ordinal() + 1) % all.length];
		}

		public static Rule of(String key) {
			if (key != null) {
				for (Rule rule : values()) {
					if (rule.key.equalsIgnoreCase(key)) {
						return rule;
					}
				}
			}
			return TAB;
		}
	}

	public enum Shadow {
		NONE("none"),
		SOFT("soft"),
		STRONG("strong");

		private final String key;

		Shadow(String key) {
			this.key = key;
		}

		public String key() {
			return key;
		}

		public String translationKey() {
			return "aeternum.shadow." + key;
		}

		public Shadow next() {
			Shadow[] all = values();
			return all[(ordinal() + 1) % all.length];
		}

		public static Shadow of(String key) {
			if (key != null) {
				for (Shadow shadow : values()) {
					if (shadow.key.equalsIgnoreCase(key)) {
						return shadow;
					}
				}
			}
			return NONE;
		}
	}

	public enum Density {
		COMPACT("compact"),
		NORMAL("normal"),
		AIRY("airy");

		private final String key;

		Density(String key) {
			this.key = key;
		}

		public String key() {
			return key;
		}

		public String translationKey() {
			return "aeternum.density." + key;
		}

		public Density next() {
			Density[] all = values();
			return all[(ordinal() + 1) % all.length];
		}

		public static Density of(String key) {
			if (key != null) {
				for (Density value : values()) {
					if (value.key.equalsIgnoreCase(key)) {
						return value;
					}
				}
			}
			return NORMAL;
		}
	}

	public enum TextShadow {
		NONE("none"),
		WORLD("world"),
		ALL("all");

		private final String key;

		TextShadow(String key) {
			this.key = key;
		}

		public String key() {
			return key;
		}

		public String translationKey() {
			return "aeternum.text_shadow." + key;
		}

		public TextShadow next() {
			TextShadow[] all = values();
			return all[(ordinal() + 1) % all.length];
		}

		public static TextShadow of(String key) {
			if (key != null) {
				for (TextShadow value : values()) {
					if (value.key.equalsIgnoreCase(key)) {
						return value;
					}
				}
			}
			return WORLD;
		}
	}

	public enum Selection {
		MARKER("marker"),
		FILL("fill"),
		UNDERLINE("underline");

		private final String key;

		Selection(String key) {
			this.key = key;
		}

		public String key() {
			return key;
		}

		public String translationKey() {
			return "aeternum.selection." + key;
		}

		public Selection next() {
			Selection[] all = values();
			return all[(ordinal() + 1) % all.length];
		}

		public static Selection of(String key) {
			if (key != null) {
				for (Selection value : values()) {
					if (value.key.equalsIgnoreCase(key)) {
						return value;
					}
				}
			}
			return MARKER;
		}
	}

	public record Style(int radius, int stroke, Shadow shadow, Rule rule, int backdrop, int panel, Density density,
			TextShadow textShadow, boolean roundHud, Selection selection, int contrast) {
		public static final int RADIUS_MAX = 10;
		public static final int STROKE_MAX = 2;
		public static final int BACKDROP_MAX = 100;
		public static final int PANEL_MIN = 40;
		public static final int PANEL_MAX = 100;
		public static final int CONTRAST_MAX = 100;
		public static final int CONTRAST_DEFAULT = 50;
		public static final Style DEFAULT = new Style(0, 1, Shadow.NONE, Rule.TAB, 80, 95);

		public Style {
			radius = Math.clamp(radius, 0, RADIUS_MAX);
			stroke = Math.clamp(stroke, 0, STROKE_MAX);
			shadow = shadow == null ? Shadow.NONE : shadow;
			rule = rule == null ? Rule.TAB : rule;
			backdrop = Math.clamp(backdrop, 0, BACKDROP_MAX);
			panel = Math.clamp(panel, PANEL_MIN, PANEL_MAX);
			density = density == null ? Density.NORMAL : density;
			textShadow = textShadow == null ? TextShadow.WORLD : textShadow;
			selection = selection == null ? Selection.MARKER : selection;
			contrast = Math.clamp(contrast, 0, CONTRAST_MAX);
		}

		public Style(int radius, int stroke, Shadow shadow, Rule rule, int backdrop, int panel) {
			this(radius, stroke, shadow, rule, backdrop, panel, Density.NORMAL, TextShadow.WORLD, false, Selection.MARKER,
				CONTRAST_DEFAULT);
		}

		public Style withRadius(int value) {
			return new Style(value, stroke, shadow, rule, backdrop, panel, density, textShadow, roundHud, selection, contrast);
		}

		public Style withStroke(int value) {
			return new Style(radius, value, shadow, rule, backdrop, panel, density, textShadow, roundHud, selection, contrast);
		}

		public Style withShadow(Shadow value) {
			return new Style(radius, stroke, value, rule, backdrop, panel, density, textShadow, roundHud, selection, contrast);
		}

		public Style withRule(Rule value) {
			return new Style(radius, stroke, shadow, value, backdrop, panel, density, textShadow, roundHud, selection, contrast);
		}

		public Style withBackdrop(int value) {
			return new Style(radius, stroke, shadow, rule, value, panel, density, textShadow, roundHud, selection, contrast);
		}

		public Style withPanel(int value) {
			return new Style(radius, stroke, shadow, rule, backdrop, value, density, textShadow, roundHud, selection, contrast);
		}

		public Style withDensity(Density value) {
			return new Style(radius, stroke, shadow, rule, backdrop, panel, value, textShadow, roundHud, selection, contrast);
		}

		public Style withTextShadow(TextShadow value) {
			return new Style(radius, stroke, shadow, rule, backdrop, panel, density, value, roundHud, selection, contrast);
		}

		public Style withRoundHud(boolean value) {
			return new Style(radius, stroke, shadow, rule, backdrop, panel, density, textShadow, value, selection, contrast);
		}

		public Style withSelection(Selection value) {
			return new Style(radius, stroke, shadow, rule, backdrop, panel, density, textShadow, roundHud, value, contrast);
		}

		public Style withContrast(int value) {
			return new Style(radius, stroke, shadow, rule, backdrop, panel, density, textShadow, roundHud, selection, value);
		}
	}

	public record Saved(String name, Palette palette) {
	}

	record State(Preset preset, Palette palette, String active, List<Saved> saved, Style style) {
		State(Preset preset, Palette palette, String active, List<Saved> saved) {
			this(preset, palette, active, saved, Style.DEFAULT);
		}
	}

	private static final class StyleData {
		Integer radius;
		Integer border;
		String shadow;
		String rule;
		Integer backdrop;
		Integer panel;
		String density;
		String textShadow;
		Boolean roundHud;
		String selection;
		Integer contrast;
	}

	private record Pending(State state, int edits) {
	}

	private static final class SavedData {
		String name;
		Map<String, String> colors;
	}

	private static final class Data {
		String theme;
		Map<String, String> custom;
		String active;
		List<SavedData> saved;
		StyleData style;
	}

	public static final String MIGRATED_NAME = "Personnalisé";
	private static final String FALLBACK_NAME = "Thème";

	private static Preset preset = Preset.AETERNUM;
	private static Palette custom = Preset.AETERNUM.palette();
	private static String active;
	private static final List<Saved> saved = new ArrayList<>();
	private static Style style = Style.DEFAULT;
	private static final AtomicReference<Pending> PENDING = new AtomicReference<>();
	private static volatile Path path;
	private static volatile long stamp = Long.MIN_VALUE;
	private static volatile int edits;
	private static int revision;
	private static volatile boolean loaded;

	private AeternumTheme() {
	}

	public static boolean globalLook() {
		if (globalLook == null) {
			globalLook = FabricLoader.getInstance().isModLoaded(LOOK_OWNER);
		}
		return globalLook;
	}

	public static Path file() {
		return AeternumPaths.config(LEGACY_FILE_NAME, FILE_NAME);
	}

	public static Preset preset() {
		ensureLoaded();
		return preset;
	}

	public static Palette palette() {
		ensureLoaded();
		return preset == Preset.CUSTOM ? custom : preset.palette();
	}

	public static Palette customPalette() {
		ensureLoaded();
		return custom;
	}

	public static int revision() {
		ensureLoaded();
		return revision;
	}

	public static List<Saved> saved() {
		ensureLoaded();
		return List.copyOf(saved);
	}

	public static String activeSaved() {
		ensureLoaded();
		return preset == Preset.CUSTOM ? active : null;
	}

	public static Style style() {
		ensureLoaded();
		return style;
	}

	public static void setStyle(Style value) {
		ensureLoaded();
		Style next = value == null ? Style.DEFAULT : value;
		if (next.equals(style)) {
			return;
		}
		style = next;
		revision++;
		save();
	}

	public static void resetStyle() {
		setStyle(Style.DEFAULT);
	}

	public static void cycle() {
		ensureLoaded();
		List<String> options = new ArrayList<>();
		for (Preset candidate : Preset.values()) {
			if (candidate != Preset.CUSTOM) {
				options.add(candidate.key());
			}
		}
		int firstSaved = options.size();
		for (Saved theme : saved) {
			options.add(theme.name());
		}
		int current = preset == Preset.CUSTOM ? firstSaved + indexOf(active) : preset.ordinal();
		int next = (current + 1) % options.size();
		if (next < firstSaved) {
			select(Preset.of(options.get(next)));
		} else {
			selectSaved(options.get(next));
		}
	}

	public static void select(Preset target) {
		ensureLoaded();
		if (target == Preset.CUSTOM) {
			if (!saved.isEmpty() && preset != Preset.CUSTOM) {
				selectSaved(saved.getFirst().name());
			}
			return;
		}
		if (target == preset) {
			return;
		}
		preset = target;
		active = null;
		revision++;
		save();
	}

	public static void selectSaved(String name) {
		ensureLoaded();
		int index = indexOf(name);
		if (index < 0) {
			return;
		}
		Saved theme = saved.get(index);
		preset = Preset.CUSTOM;
		custom = theme.palette();
		active = theme.name();
		revision++;
		save();
	}

	public static String createSaved(String requestedName) {
		ensureLoaded();
		String name = uniqueName(requestedName, names(), FALLBACK_NAME);
		Palette base = palette();
		saved.add(new Saved(name, base));
		preset = Preset.CUSTOM;
		custom = base;
		active = name;
		revision++;
		save();
		return name;
	}

	public static String renameSaved(String from, String requestedName) {
		ensureLoaded();
		int index = indexOf(from);
		if (index < 0) {
			return from;
		}
		List<String> others = new ArrayList<>(names());
		others.remove(index);
		String name = uniqueName(requestedName, others, from);
		saved.set(index, new Saved(name, saved.get(index).palette()));
		if (from.equals(active)) {
			active = name;
		}
		revision++;
		save();
		return name;
	}

	public static void deleteSaved(String name) {
		ensureLoaded();
		int index = indexOf(name);
		if (index < 0) {
			return;
		}
		saved.remove(index);
		if (name.equals(active)) {
			active = null;
			preset = Preset.AETERNUM;
			custom = Preset.AETERNUM.palette();
		}
		revision++;
		save();
	}

	public static void setRole(Role role, int rgb) {
		ensureLoaded();
		if (preset != Preset.CUSTOM || indexOf(active) < 0) {
			createSaved(MIGRATED_NAME);
		}
		custom = custom.with(role, rgb);
		saved.set(indexOf(active), new Saved(active, custom));
		revision++;
		save();
	}

	public static void reset() {
		ensureLoaded();
		preset = Preset.AETERNUM;
		active = null;
		revision++;
		save();
	}

	public static boolean refresh() {
		if (!loaded) {
			ensureLoaded();
			return true;
		}
		Pending ready = PENDING.get();
		if (ready == null || !PENDING.compareAndSet(ready, null) || ready.edits() != edits) {
			return false;
		}
		apply(ready.state());
		revision++;
		return true;
	}

	private static void ensureLoaded() {
		if (loaded) {
			return;
		}
		synchronized (AeternumTheme.class) {
			if (loaded) {
				return;
			}
			Path target = file();
			path = target;
			stamp = AeternumFiles.modified(target);
			apply(decode(read(target), MIGRATED_NAME));
			revision++;
			loaded = true;
		}
		AeternumFiles.watch(AeternumTheme::poll);
	}

	private static void poll() {
		Path target = path;
		long modified = AeternumFiles.modified(target);
		if (modified == stamp) {
			return;
		}
		stamp = modified;
		int seen = edits;
		PENDING.set(new Pending(decode(read(target), MIGRATED_NAME), seen));
	}

	private static int indexOf(String name) {
		if (name == null) {
			return -1;
		}
		for (int index = 0; index < saved.size(); index++) {
			if (saved.get(index).name().equals(name)) {
				return index;
			}
		}
		return -1;
	}

	private static List<String> names() {
		List<String> names = new ArrayList<>();
		for (Saved theme : saved) {
			names.add(theme.name());
		}
		return names;
	}

	private static String read(Path target) {
		if (!Files.isRegularFile(target)) {
			return null;
		}
		try {
			return Files.readString(target);
		} catch (Exception e) {
			Mercuriale.LOGGER.warn("[mercuriale] {} illisible, thème par défaut", FILE_NAME, e);
			return null;
		}
	}

	private static void apply(State state) {
		preset = state.preset();
		custom = state.palette();
		active = state.active();
		saved.clear();
		saved.addAll(state.saved());
		style = state.style();
	}

	private static void save() {
		edits++;
		Path target = path;
		String json = encode(new State(preset, custom, active, List.copyOf(saved), style));
		AeternumFiles.run(() -> {
			try {
				Files.createDirectories(target.getParent());
				Files.writeString(target, json);
				stamp = AeternumFiles.modified(target);
			} catch (Exception e) {
				Mercuriale.LOGGER.warn("[mercuriale] écriture de {} impossible", FILE_NAME, e);
			}
		});
	}

	static State decode(String json, String migratedName) {
		Palette fallback = Preset.AETERNUM.palette();
		if (json == null || json.isBlank()) {
			return new State(Preset.AETERNUM, fallback, null, List.of());
		}
		Data data;
		try {
			data = GSON.fromJson(json, Data.class);
		} catch (Exception e) {
			Mercuriale.LOGGER.warn("[mercuriale] {} illisible, thème par défaut", FILE_NAME, e);
			return new State(Preset.AETERNUM, fallback, null, List.of());
		}
		if (data == null) {
			return new State(Preset.AETERNUM, fallback, null, List.of());
		}
		Style shape = style(data.style);
		List<Saved> themes = new ArrayList<>();
		List<String> taken = new ArrayList<>();
		if (data.saved != null) {
			for (SavedData entry : data.saved) {
				if (entry == null || entry.name == null || entry.name.isBlank() || taken.contains(entry.name.strip())) {
					continue;
				}
				String name = entry.name.strip();
				themes.add(new Saved(name, colors(entry.colors, fallback)));
				taken.add(name);
			}
		}
		Preset chosen = Preset.of(data.theme);
		if (chosen != Preset.CUSTOM) {
			return new State(chosen, colors(data.custom, fallback), null, List.copyOf(themes), shape);
		}
		String wanted = data.active == null || data.active.isBlank() ? migratedName : data.active.strip();
		for (Saved theme : themes) {
			if (theme.name().equals(wanted)) {
				return new State(Preset.CUSTOM, theme.palette(), wanted, List.copyOf(themes), shape);
			}
		}
		String name = uniqueName(wanted, taken, migratedName);
		Palette recovered = colors(data.custom, fallback);
		themes.add(new Saved(name, recovered));
		return new State(Preset.CUSTOM, recovered, name, List.copyOf(themes), shape);
	}

	private static Style style(StyleData data) {
		Style base = Style.DEFAULT;
		if (data == null) {
			return base;
		}
		return new Style(
			data.radius == null ? base.radius() : data.radius,
			data.border == null ? base.stroke() : data.border,
			data.shadow == null ? base.shadow() : Shadow.of(data.shadow),
			data.rule == null ? base.rule() : Rule.of(data.rule),
			data.backdrop == null ? base.backdrop() : data.backdrop,
			data.panel == null ? base.panel() : data.panel,
			data.density == null ? base.density() : Density.of(data.density),
			data.textShadow == null ? base.textShadow() : TextShadow.of(data.textShadow),
			data.roundHud == null ? base.roundHud() : data.roundHud,
			data.selection == null ? base.selection() : Selection.of(data.selection),
			data.contrast == null ? base.contrast() : data.contrast);
	}

	static String encode(State state) {
		Data data = new Data();
		data.theme = state.preset().key();
		data.custom = hexes(state.palette());
		data.active = state.preset() == Preset.CUSTOM ? state.active() : null;
		data.saved = new ArrayList<>();
		for (Saved theme : state.saved()) {
			SavedData entry = new SavedData();
			entry.name = theme.name();
			entry.colors = hexes(theme.palette());
			data.saved.add(entry);
		}
		Style shape = state.style();
		data.style = new StyleData();
		data.style.radius = shape.radius();
		data.style.border = shape.stroke();
		data.style.shadow = shape.shadow().key();
		data.style.rule = shape.rule().key();
		data.style.backdrop = shape.backdrop();
		data.style.panel = shape.panel();
		data.style.density = shape.density().key();
		data.style.textShadow = shape.textShadow().key();
		data.style.roundHud = shape.roundHud();
		data.style.selection = shape.selection().key();
		data.style.contrast = shape.contrast();
		return GSON.toJson(data);
	}

	static String uniqueName(String requested, Collection<String> taken, String fallback) {
		String base = requested == null || requested.isBlank() ? fallback : requested.strip();
		if (!taken.contains(base)) {
			return base;
		}
		int suffix = 2;
		while (taken.contains(base + " " + suffix)) {
			suffix++;
		}
		return base + " " + suffix;
	}

	private static Palette colors(Map<String, String> hexes, Palette fallback) {
		Palette palette = fallback;
		if (hexes != null) {
			for (Role role : Role.values()) {
				String hex = hexes.get(role.key());
				if (hex != null) {
					palette = palette.with(role, parse(hex, palette.role(role)));
				}
			}
		}
		return palette;
	}

	private static Map<String, String> hexes(Palette palette) {
		Map<String, String> hexes = new LinkedHashMap<>();
		for (Role role : Role.values()) {
			hexes.put(role.key(), hex(palette.role(role)));
		}
		return hexes;
	}

	public static int parse(String value, int fallback) {
		if (value == null) {
			return fallback;
		}
		String digits = value.startsWith("#") ? value.substring(1) : value;
		if (digits.length() != 6) {
			return fallback;
		}
		try {
			return Integer.parseInt(digits, 16) & 0xFFFFFF;
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	public static boolean valid(String value) {
		if (value == null) {
			return false;
		}
		String digits = value.startsWith("#") ? value.substring(1) : value;
		if (digits.length() != 6) {
			return false;
		}
		for (int i = 0; i < digits.length(); i++) {
			if (Character.digit(digits.charAt(i), 16) < 0) {
				return false;
			}
		}
		return true;
	}

	public static String hex(int rgb) {
		return String.format(Locale.ROOT, "%06X", rgb & 0xFFFFFF);
	}

	public static float[] toHsv(int rgb) {
		float r = ((rgb >> 16) & 0xFF) / 255.0f;
		float g = ((rgb >> 8) & 0xFF) / 255.0f;
		float b = (rgb & 0xFF) / 255.0f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float delta = max - min;
		float hue = 0.0f;
		if (delta > 0.0f) {
			if (max == r) {
				hue = ((g - b) / delta) % 6.0f;
			} else if (max == g) {
				hue = ((b - r) / delta) + 2.0f;
			} else {
				hue = ((r - g) / delta) + 4.0f;
			}
			hue *= 60.0f;
			if (hue < 0.0f) {
				hue += 360.0f;
			}
		}
		float saturation = max <= 0.0f ? 0.0f : delta / max;
		return new float[] {hue, saturation, max};
	}

	public static int fromHsv(float hue, float saturation, float value) {
		float h = ((hue % 360.0f) + 360.0f) % 360.0f;
		float s = Math.clamp(saturation, 0.0f, 1.0f);
		float v = Math.clamp(value, 0.0f, 1.0f);
		float c = v * s;
		float x = c * (1.0f - Math.abs((h / 60.0f) % 2.0f - 1.0f));
		float m = v - c;
		float r;
		float g;
		float b;
		if (h < 60.0f) {
			r = c;
			g = x;
			b = 0.0f;
		} else if (h < 120.0f) {
			r = x;
			g = c;
			b = 0.0f;
		} else if (h < 180.0f) {
			r = 0.0f;
			g = c;
			b = x;
		} else if (h < 240.0f) {
			r = 0.0f;
			g = x;
			b = c;
		} else if (h < 300.0f) {
			r = x;
			g = 0.0f;
			b = c;
		} else {
			r = c;
			g = 0.0f;
			b = x;
		}
		int red = Math.round((r + m) * 255.0f);
		int green = Math.round((g + m) * 255.0f);
		int blue = Math.round((b + m) * 255.0f);
		return (red << 16) | (green << 8) | blue;
	}
}
