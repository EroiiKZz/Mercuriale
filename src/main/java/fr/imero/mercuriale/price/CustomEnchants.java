package fr.imero.mercuriale.price;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record CustomEnchants(int enchants, List<String> books, int augmenters, boolean rune) {
	public static final CustomEnchants NONE = new CustomEnchants(0, List.of(), 0, false);
	public static final String BOOK_MATERIAL = "enchanted_book";
	public static final String AUGMENTER_MATERIAL = "ender_eye";
	public static final String AUGMENTER_NAME = "Augmenteur de Slots";
	public static final String RUNE_MATERIAL = "nether_star";
	public static final String RUNE_NAME = "Rune Protectrice";

	private static final String ENCHANT_KEY = "advancedenchantments:ae_enchantment-";
	private static final int FREE_SLOTS = 5;
	private static final int MAX_SLOTS = 15;
	private static final Pattern BOOK = Pattern.compile("^\\S.*\\s[IVXL]+$");
	private static final Pattern SLOTS_LEFT = Pattern.compile("slots disponibles\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
	private static final String RUNE = PriceKeys.loose(RUNE_NAME);

	public static CustomEnchants read(String customData, List<String> lore) {
		int enchants = 0;
		for (int at = customData.indexOf(ENCHANT_KEY); at >= 0; at = customData.indexOf(ENCHANT_KEY, at + 1)) {
			enchants++;
		}
		List<String> books = new ArrayList<>();
		int slotsLeft = -1;
		boolean rune = false;
		for (String line : lore) {
			String plain = line.strip();
			Matcher slots = SLOTS_LEFT.matcher(plain);
			if (slots.find()) {
				slotsLeft = Integer.parseInt(slots.group(1));
			} else if (PriceKeys.loose(plain).contains(RUNE)) {
				rune = true;
			} else if (books.size() < enchants && BOOK.matcher(plain).matches()) {
				books.add(plain);
			}
		}
		if (enchants == 0 && slotsLeft < 0) {
			return NONE;
		}
		int augmenters = slotsLeft < 0 ? 0 : Math.clamp(enchants + slotsLeft - FREE_SLOTS, 0, MAX_SLOTS - FREE_SLOTS);
		return new CustomEnchants(enchants, List.copyOf(books), augmenters, rune);
	}

	public boolean isEmpty() {
		return enchants == 0 && augmenters == 0 && !rune;
	}
}
