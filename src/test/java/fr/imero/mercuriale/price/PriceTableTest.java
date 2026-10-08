package fr.imero.mercuriale.price;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceTableTest {
	private static final String JSON = """
		{
		  "version": "666b109571b655a2",
		  "updatedAt": "2026-10-05T12:14:33.856Z",
		  "market": [
		    {"material": "POTION", "name": "Potion de fly (15 minutes)", "vanilla": false,
		     "slug": "potion_potion_de_fly_15_minutes", "price": 6000, "tier": "sold", "samples": 55,
		     "windowDays": 60, "onSale": 12, "cheapest": 150},
		    {"material": "DIAMOND", "name": "Diamond", "vanilla": true, "slug": "diamond_diamond",
		     "price": 90, "tier": "asked", "samples": 7, "windowDays": null, "onSale": 0, "cheapest": null},
		    {"material": "PLAYER_HEAD", "name": "🎃 Citrouille maudite", "vanilla": false,
		     "slug": "player_head_citrouille_maudite_u1f383", "price": 2500, "tier": "thin", "samples": 2,
		     "windowDays": null, "onSale": 1, "cheapest": 2400},
		    {"material": "FISHING_ROD", "name": "✿ Canne à pêche Pâques ✿", "vanilla": false,
		     "slug": "a", "price": 100, "tier": "thin", "samples": 1, "windowDays": null, "onSale": 0,
		     "cheapest": null},
		    {"material": "FISHING_ROD", "name": "🐇 Canne à pêche Pâques 🐇", "vanilla": false,
		     "slug": "b", "price": 900, "tier": "thin", "samples": 1, "windowDays": null, "onSale": 0,
		     "cheapest": null}
		  ],
		  "shops": [
		    {"material": "OAK_LOG", "buyFrom": 14, "sellTo": 12.1, "sellers": 4, "buyers": 1},
		    {"material": "DIAMOND", "buyFrom": 79, "sellTo": 65, "sellers": 9, "buyers": 5},
		    {"material": "BEEF", "buyFrom": 0.19, "sellTo": null, "sellers": 2, "buyers": 0}
		  ]
		}
		""";

	private final PriceTable table = PriceTable.parse(JSON);

	@Test
	void readsVersionAndFields() {
		assertEquals("666b109571b655a2", table.version());
		MarketPrice fly = table.quote("potion", "Potion de fly (15 minutes)").market();
		assertEquals(6000.0, fly.price());
		assertEquals(MarketPrice.Tier.SOLD, fly.tier());
		assertEquals(55, fly.samples());
		assertEquals(60, fly.windowDays());
		assertEquals(150.0, fly.cheapest());
	}

	@Test
	void unnamedItemTakesVanillaEntryAndShops() {
		PriceTable.Quote quote = table.quote("diamond", null);
		assertEquals("Diamond", quote.market().name());
		assertNull(quote.market().windowDays());
		assertEquals(79.0, quote.shop().buyFrom());
		assertEquals(65.0, quote.shop().sellTo());
	}

	@Test
	void unnamedItemWithoutMarketEntryStillHasShops() {
		PriceTable.Quote quote = table.quote("oak_log", null);
		assertNull(quote.market());
		assertEquals(12.1, quote.shop().sellTo());
		assertFalse(quote.isEmpty());
	}

	@Test
	void renamedItemNeverTakesTheShopPriceOfItsMaterial() {
		PriceTable.Quote quote = table.quote("DIAMOND", "Diamant de Noël");
		assertNull(quote.shop());
		assertNull(quote.market());
		assertTrue(quote.isEmpty());
	}

	@Test
	void nameMatchIgnoresCaseSpacingAndFormatting() {
		assertEquals(6000.0, table.quote("POTION", "  §bpotion DE   fly (15 minutes) ").market().price());
	}

	@Test
	void decorationsDrawnDifferentlyInGameStillMatch() {
		assertEquals(2500.0, table.quote("PLAYER_HEAD", " Citrouille maudite").market().price());
	}

	@Test
	void exactDecorationsDecideBetweenLookalikes() {
		assertEquals(900.0, table.quote("FISHING_ROD", "🐇 Canne à pêche Pâques 🐇").market().price());
		assertEquals(100.0, table.quote("FISHING_ROD", "✿ Canne à pêche Pâques ✿").market().price());
	}

	@Test
	void ambiguousLookalikeShowsNothing() {
		assertNull(table.quote("FISHING_ROD", "Canne à pêche Pâques").market());
	}

	@Test
	void shopWithOnlyOneSideKeepsTheOtherNull() {
		ShopPrice beef = table.quote("beef", null).shop();
		assertEquals(0.19, beef.buyFrom());
		assertNull(beef.sellTo());
	}

	@Test
	void referencePrefersTheMarketThenResaleThenPurchase() {
		assertEquals(90.0, table.quote("diamond", null).reference().getAsDouble());
		assertEquals(65.0, table.quote("diamond", null).only(false, true).reference().getAsDouble());
		assertEquals(12.1, table.quote("oak_log", null).reference().getAsDouble());
		assertEquals(0.19, table.quote("beef", null).reference().getAsDouble());
		assertTrue(table.quote("diamond", null).only(false, false).reference().isEmpty());
	}

	@Test
	void emptyTableAnswersNothing() {
		assertTrue(PriceTable.EMPTY.isEmpty());
		assertTrue(PriceTable.EMPTY.quote("DIAMOND", null).isEmpty());
	}
}
