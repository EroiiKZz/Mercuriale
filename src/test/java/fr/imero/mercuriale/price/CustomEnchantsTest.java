package fr.imero.mercuriale.price;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomEnchantsTest {
	private static final List<String> SWORD = List.of("Broyeur d'os III", "Death Punch V", "Berserk V",
		"Poids plume III", "Critique III", "Tueur du Néant IV", "Bloquer III", "Immolation III", "Permafrost V",
		"Tueur de bêtes IV", "Frappe double III", "Vol de la vie V", "Tueur de Raid I", "",
		"+10% Xp -> métier", "+10% Argent -> métier", "★ RUNE PROTECTRICE ★", "Slots disponibles: 2 ⓢ",
		"Mobs tués: 71081 🗡");
	private static final List<String> PICKAXE = List.of("Experience V", "Haste de bloc III", "Télépathie III",
		"Renforcé IV", "Mineur de filon III", "", "+10% Xp -> métier", "+10% Argent -> métier",
		"★ RUNE PROTECTRICE ★", "Slots disponibles: 0 ⓢ", "Blocs Cassés: 115820");
	private static final List<String> SHOVEL = List.of("Blocs Cassés: 23894");
	private static final List<String> AXE = List.of("Experience V", "Haste de bloc III", "Télépathie IV", "",
		"+10% Xp -> métier", "+10% Argent -> métier", "Slots disponibles: 2 ⓢ", "Blocs Cassés: 3180687");

	private static final String JSON = """
		{"version": "t", "market": [
		  {"material": "ENCHANTED_BOOK", "name": "Experience V", "price": 1000},
		  {"material": "ENCHANTED_BOOK", "name": "Haste de bloc III", "price": 2000},
		  {"material": "ENDER_EYE", "name": "⭐ Augmenteur de Slots ⭐", "price": 50000},
		  {"material": "NETHER_STAR", "name": "Rune Protectrice", "price": 30000}
		], "shops": []}
		""";

	@Test
	void swordWithTenAugmentersAndARune() {
		CustomEnchants sword = CustomEnchants.read(SWORD);
		assertEquals(13, sword.enchants().size());
		assertEquals(2, sword.freeSlots());
		assertEquals(15, sword.slots());
		assertEquals(10, sword.augmenters());
		assertTrue(sword.rune());
	}

	@Test
	void fiveEnchantsNeedNoAugmenter() {
		CustomEnchants pickaxe = CustomEnchants.read(PICKAXE);
		assertEquals(5, pickaxe.enchants().size());
		assertEquals(0, pickaxe.augmenters());
		assertTrue(pickaxe.rune());

		CustomEnchants axe = CustomEnchants.read(AXE);
		assertEquals(3, axe.enchants().size());
		assertEquals(0, axe.augmenters());
		assertFalse(axe.rune());
	}

	@Test
	void itemWithoutCustomEnchantsIsEmpty() {
		assertTrue(CustomEnchants.read(SHOVEL).isEmpty());
		assertTrue(CustomEnchants.read(List.of()).isEmpty());
	}

	@Test
	void vanillaEnchantsWithAnIconAreNotCounted() {
		CustomEnchants read = CustomEnchants.read(List.of(" Sharpness V", " Mending", "Berserk V"));
		assertEquals(List.of("Berserk V"), read.enchants());
	}

	@Test
	void appraisesBooksAugmentersAndRune() {
		PriceTable table = PriceTable.parse(JSON);
		CustomEnchants.Appraisal axe = CustomEnchants.read(AXE).appraise(table);
		assertEquals(3000.0, axe.enchants());
		assertEquals(1, axe.unpriced());

		CustomEnchants.Appraisal sword = CustomEnchants.read(SWORD).appraise(table);
		assertEquals(500_000.0, sword.augmenters(10));
		assertEquals(30_000.0, sword.rune().price());
		assertNull(CustomEnchants.read(SWORD).appraise(PriceTable.EMPTY).augmenter());
	}
}
