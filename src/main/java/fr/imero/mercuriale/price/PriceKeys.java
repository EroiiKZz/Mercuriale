package fr.imero.mercuriale.price;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class PriceKeys {
	private static final Pattern LEGACY_FORMAT = Pattern.compile("§.");
	private static final Pattern SPACES = Pattern.compile("\\s+");
	private static final Pattern MARKS = Pattern.compile("\\p{M}+");
	private static final Pattern NOT_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

	private PriceKeys() {
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
