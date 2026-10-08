package fr.imero.mercuriale.price;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Enchantements custom VikingCraft lus dans la lore : « Broyeur d'os III », « Slots disponibles: 2 », « ★ RUNE PROTECTRICE ★ »
public record CustomEnchants(List<String> enchants, int freeSlots, boolean rune) {
	public static final int BASE_SLOTS = 5;
	public static final int MAX_SLOTS = 15;
	// Clés de la table VkAnalyzer : un livre se vend sous le nom affiché dans la lore (« Immolation III »),
	// l'augmenteur est un œil de l'Ender « ⭐ Augmenteur de Slots ⭐ » (les étoiles sautent à la clé loose)
	private static final String BOOK = "ENCHANTED_BOOK";
	private static final String AUGMENTER_MATERIAL = "ENDER_EYE";
	private static final String AUGMENTER_NAME = "Augmenteur de Slots";
	private static final String RUNE_NAME = "Rune protectrice";

	// Commence par une lettre : les enchants vanilla, précédés d'une icône du pack, ne passent pas
	private static final Pattern ENCHANT = Pattern.compile("^(\\p{L}[\\p{L}\\p{M}'’\\- ]*?)\\s+([IVXL]+)$");
	private static final Pattern FREE_SLOTS = Pattern.compile("slots?\\s+disponibles?\\s*:\\s*(\\d+)",
		Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final String RUNE_MARK = "rune protectrice";

	public record Appraisal(double enchants, int unpriced, MarketPrice augmenter, MarketPrice rune) {
		public double augmenters(int count) {
			return augmenter == null ? 0.0 : augmenter.price() * count;
		}
	}

	public static CustomEnchants read(List<String> lore) {
		List<String> enchants = new ArrayList<>();
		Integer free = null;
		boolean rune = false;
		boolean header = true;
		for (String raw : lore == null ? List.<String>of() : lore) {
			String line = raw == null ? "" : raw.strip();
			// Les enchants forment le premier bloc de la lore, jusqu'à la première ligne vide
			if (line.isEmpty()) {
				header = false;
				continue;
			}
			if (header && ENCHANT.matcher(line).matches()) {
				enchants.add(line);
				continue;
			}
			Matcher slots = FREE_SLOTS.matcher(line);
			if (free == null && slots.find()) {
				free = Integer.parseInt(slots.group(1));
			}
			if (line.toLowerCase(Locale.ROOT).contains(RUNE_MARK)) {
				rune = true;
			}
		}
		int freeSlots = free != null ? free : Math.max(0, BASE_SLOTS - enchants.size());
		return new CustomEnchants(List.copyOf(enchants), freeSlots, rune);
	}

	public boolean isEmpty() {
		return enchants.isEmpty() && !rune;
	}

	public int slots() {
		return Math.min(MAX_SLOTS, Math.max(BASE_SLOTS, enchants.size() + freeSlots));
	}

	public int augmenters() {
		return slots() - BASE_SLOTS;
	}

	public Appraisal appraise(PriceTable table) {
		double total = 0.0;
		int unpriced = 0;
		for (String enchant : enchants) {
			MarketPrice price = priced(table.quote(BOOK, enchant).market());
			if (price == null) {
				unpriced++;
			} else {
				total += price.price();
			}
		}
		MarketPrice augmenter = priced(table.quote(AUGMENTER_MATERIAL, AUGMENTER_NAME).market());
		return new Appraisal(total, unpriced, augmenter, priced(table.anyMaterial(RUNE_NAME)));
	}

	private static MarketPrice priced(MarketPrice price) {
		return price == null || price.price() <= 0 ? null : price;
	}
}
