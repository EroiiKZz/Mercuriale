package fr.imero.mercuriale.price;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomEnchantsTest {
	private static final String ONE = "{PublicBukkitValues:{\"advancedenchantments:repaircost\":31,"
		+ "\"advancedenchantments:slots\":1,\"advancedenchantments:ae_enchantment-smelting\":3}}";

	@Test
	void readsTheFontiFatouPickaxe() {
		CustomEnchants found = CustomEnchants.read(ONE, List.of("Fonte III", "Slots disponibles: 4 🎯"));
		assertEquals(1, found.enchants());
		assertEquals(List.of("Fonte III"), found.books());
		assertEquals(0, found.augmenters());
		assertFalse(found.rune());
	}

	@Test
	void slotsBeyondTheFiveFreeOnesAreAugmenters() {
		String seven = "{PublicBukkitValues:{" + "\"advancedenchantments:ae_enchantment-a\":1,".repeat(7) + "}}";
		List<String> lore = List.of("Aegis III", "Aimant I", "Allure II", "Archer III", "Berserk V", "Brillant I",
			"Chasseur IV", "Slots disponibles: 2", "Rune protectrice");
		CustomEnchants found = CustomEnchants.read(seven, lore);
		assertEquals(7, found.books().size());
		assertEquals(4, found.augmenters());
		assertTrue(found.rune());
	}

	@Test
	void augmentersStopAtTen() {
		assertEquals(10, CustomEnchants.read("", List.of("Slots disponibles: 40")).augmenters());
	}

	@Test
	void ignoresItemsWithoutAdvancedEnchantments() {
		assertSame(CustomEnchants.NONE, CustomEnchants.read("{}", List.of("Rune protectrice", "Louis XIV")));
		assertTrue(CustomEnchants.read("{}", List.of()).isEmpty());
	}
}
