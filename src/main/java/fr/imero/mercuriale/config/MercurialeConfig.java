package fr.imero.mercuriale.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.imero.mercuriale.AeternumPaths;
import fr.imero.mercuriale.Mercuriale;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class MercurialeConfig {
	public static final String DEFAULT_URL = "https://vkanalyzer.nexsarum.dev/api/v1/prices";
	public static final String DEFAULT_SERVER = "vikingcraft";
	public static final String DEFAULT_SERVER_MENU_PATTERN = "^\\s*Menu\\s*\\|";
	public static final String DEFAULT_AUCTION_TITLE_PATTERN = "(?i)h[oô]tel des ventes";
	public static final String DEFAULT_LISTING_PRICE_PATTERN =
		"(?i)prix\\s*:?\\s*([0-9][0-9 .,\\u00a0\\u202f']*)\\s*\\$";
	public static final int UNDERCUT_MIN = 0;
	public static final int UNDERCUT_MAX = 50;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final String LEGACY_VITRINE_FILE = "vitrine.json";
	private static MercurialeConfig instance;

	public boolean enabled = true;
	public boolean market = true;
	public boolean shops = true;
	public PriceAmount amount = PriceAmount.UNIT;
	public boolean radarValues = true;
	public int shopUndercutPercent = 1;
	public boolean containerValue = false;
	public String serverMenuPattern = DEFAULT_SERVER_MENU_PATTERN;
	public boolean bargains = true;
	public String auctionTitlePattern = DEFAULT_AUCTION_TITLE_PATTERN;
	public String listingPricePattern = DEFAULT_LISTING_PRICE_PATTERN;
	public String url = DEFAULT_URL;
	public String server = DEFAULT_SERVER;

	private transient Map<String, Optional<Pattern>> compiled;

	public static synchronized MercurialeConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	public static Path file() {
		return AeternumPaths.config(Mercuriale.MOD_ID + ".json");
	}

	public static String patternError(String source) {
		if (source == null || source.isBlank()) {
			return null;
		}
		try {
			Pattern.compile(source);
			return null;
		} catch (PatternSyntaxException e) {
			return "invalid";
		}
	}

	public static String urlError(String value) {
		String url = value == null ? "" : value.strip();
		return url.startsWith("https://") || url.startsWith("http://") ? null : "invalid";
	}

	public Pattern serverMenu() {
		return pattern(serverMenuPattern);
	}

	public Pattern auctionTitle() {
		return pattern(auctionTitlePattern);
	}

	public Pattern listingPrice() {
		return pattern(listingPricePattern);
	}

	private Pattern pattern(String source) {
		if (source == null || source.isBlank()) {
			return null;
		}
		if (compiled == null) {
			compiled = new HashMap<>();
		}
		return compiled.computeIfAbsent(source,
			key -> patternError(key) == null ? Optional.of(Pattern.compile(key)) : Optional.empty()).orElse(null);
	}

	public void resetBargains() {
		MercurialeConfig fresh = new MercurialeConfig();
		bargains = fresh.bargains;
		auctionTitlePattern = fresh.auctionTitlePattern;
		listingPricePattern = fresh.listingPricePattern;
	}

	public void resetPrices() {
		MercurialeConfig fresh = new MercurialeConfig();
		enabled = fresh.enabled;
		market = fresh.market;
		shops = fresh.shops;
		amount = fresh.amount;
		url = fresh.url;
		server = fresh.server;
	}

	public void resetChestRadar() {
		MercurialeConfig fresh = new MercurialeConfig();
		radarValues = fresh.radarValues;
		shopUndercutPercent = fresh.shopUndercutPercent;
		containerValue = fresh.containerValue;
		serverMenuPattern = fresh.serverMenuPattern;
	}

	private static MercurialeConfig load() {
		MercurialeConfig config = null;
		Path file = file();
		try {
			if (Files.isRegularFile(file)) {
				config = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), MercurialeConfig.class);
			} else {
				config = fromVitrine(AeternumPaths.config(LEGACY_VITRINE_FILE));
			}
		} catch (Exception e) {
			Mercuriale.LOGGER.warn("[mercuriale] réglages illisibles, valeurs par défaut utilisées : {}", e.getMessage());
		}
		if (config == null) {
			config = new MercurialeConfig();
		}
		config.save();
		return config;
	}

	static MercurialeConfig fromVitrine(Path vitrine) {
		MercurialeConfig config = new MercurialeConfig();
		if (!Files.isRegularFile(vitrine)) {
			return config;
		}
		try {
			JsonObject old = JsonParser.parseString(Files.readString(vitrine, StandardCharsets.UTF_8)).getAsJsonObject();
			config.enabled = bool(old, "showPrices", config.enabled);
			config.market = bool(old, "pricesMarket", config.market);
			config.shops = bool(old, "pricesShops", config.shops);
			config.url = string(old, "pricesUrl", config.url);
			config.server = string(old, "pricesServer", config.server);
			String amount = string(old, "priceAmount", config.amount.name());
			for (PriceAmount candidate : PriceAmount.values()) {
				if (candidate.name().equals(amount)) {
					config.amount = candidate;
				}
			}
		} catch (Exception e) {
			Mercuriale.LOGGER.warn("[mercuriale] anciens réglages de Vitrine illisibles, ignorés : {}", e.getMessage());
		}
		return config;
	}

	private static boolean bool(JsonObject object, String name, boolean fallback) {
		JsonElement element = object.get(name);
		return element != null && element.isJsonPrimitive() ? element.getAsBoolean() : fallback;
	}

	private static String string(JsonObject object, String name, String fallback) {
		JsonElement element = object.get(name);
		return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
	}

	public synchronized void save() {
		clamp();
		Path file = file();
		try {
			Files.createDirectories(file.getParent());
			Path temp = file.resolveSibling(file.getFileName() + ".tmp");
			Files.writeString(temp, GSON.toJson(this), StandardCharsets.UTF_8);
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (Exception e) {
			Mercuriale.LOGGER.warn("[mercuriale] écriture des réglages impossible", e);
		}
	}

	void clamp() {
		if (amount == null) {
			amount = PriceAmount.UNIT;
		}
		shopUndercutPercent = Math.clamp(shopUndercutPercent, UNDERCUT_MIN, UNDERCUT_MAX);
		url = url == null || url.isBlank() ? DEFAULT_URL : url.strip();
		server = server == null ? DEFAULT_SERVER : server.strip();
		serverMenuPattern = serverMenuPattern == null ? DEFAULT_SERVER_MENU_PATTERN : serverMenuPattern;
		auctionTitlePattern = auctionTitlePattern == null ? DEFAULT_AUCTION_TITLE_PATTERN : auctionTitlePattern;
		listingPricePattern = listingPricePattern == null || listingPricePattern.isBlank()
			? DEFAULT_LISTING_PRICE_PATTERN : listingPricePattern;
	}
}
