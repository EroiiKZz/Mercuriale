package fr.imero.mercuriale.price;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PriceKeys {
	private static final Pattern LEGACY_FORMAT = Pattern.compile("§.");
	private static final Pattern SPACES = Pattern.compile("\\s+");
	private static final Pattern MARKS = Pattern.compile("\\p{M}+");
	private static final Pattern NOT_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
	// Le plugin d'enchant d'octobre 2026 appelle ses livres "Enchant Suspendre III", le site les range sans le
	// "Enchant".
	private static final Pattern PLUGIN_PREFIX = Pattern.compile("^Enchant (?=\\S)");
	private static final Pattern RARITY = Pattern.compile("^•\\s*Raret[ée]\\s+(.+)$");
	// Mêmes marqueurs de lore que VARIANTS dans item-names.ts de VkAnalyzer, qui les ajoute en suffixe aux noms des
	// annonces.
	private static final Map<Pattern, String> BOOSTERS = Map.of(
		Pattern.compile("de boost d'EXP\\b", Pattern.CASE_INSENSITIVE), "XP",
		Pattern.compile("de boost d'argent\\b", Pattern.CASE_INSENSITIVE), "Argent");

	private PriceKeys() {
	}

	/** Le nom sous lequel le site liste l'objet, avant tout suffixe de variante. */
	public static String listed(String name) {
		return PLUGIN_PREFIX.matcher(name.strip()).replaceFirst("");
	}

	/** Le " · <variante>" que le site ajoute à ce nom pour ce lore, ou null. */
	public static String variant(String name, List<String> lore) {
		if (name.startsWith("Carte:")) {
			for (String line : lore) {
				Matcher rarity = RARITY.matcher(line.strip());
				if (rarity.matches()) {
					return rarity.group(1).strip();
				}
			}
			return null;
		}
		for (var booster : BOOSTERS.entrySet()) {
			if (lore.stream().anyMatch(line -> booster.getKey().matcher(line).find())) {
				return booster.getValue();
			}
		}
		return null;
	}

	public static String material(String material) {
		return material == null ? "" : material.trim().toUpperCase(Locale.ROOT);
	}

	public static String exact(String name) {
		String plain = LEGACY_FORMAT.matcher(Normalizer.normalize(name, Normalizer.Form.NFC)).replaceAll("");
		return SPACES.matcher(plain).replaceAll(" ").trim().toLowerCase(Locale.ROOT);
	}

	public static String loose(String name) {
		String plain = LEGACY_FORMAT.matcher(name).replaceAll("");
		String bare = MARKS.matcher(Normalizer.normalize(plain, Normalizer.Form.NFD)).replaceAll("");
		return NOT_ALPHANUMERIC.matcher(bare.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
	}
}
