package fr.imero.mercuriale.price;

import fr.imero.mercuriale.config.MercurialeConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListingTest {
	private static final Pattern PRICE = Pattern.compile(MercurialeConfig.DEFAULT_LISTING_PRICE_PATTERN);

	@Test
	void readsTheLotPriceOfAVikingCraftListing() {
		List<String> lore = List.of("• Vendeur Dydark93", "• Prix 15 000$", "• Expire dans 01d 22h 33m 37s",
			"", "➤ Clic pour acheter cet item");
		assertEquals(15000.0, Listing.price(lore, PRICE).getAsDouble());
	}

	@Test
	void readsSmallAndLargePrices() {
		assertEquals(5.0, Listing.price(List.of("• Prix 5$"), PRICE).getAsDouble());
		assertEquals(1_000_000.0, Listing.price(List.of("• Prix 1 000 000$"), PRICE).getAsDouble());
		assertEquals(2.5, Listing.price(List.of("• Prix 2.5$"), PRICE).getAsDouble());
	}

	@Test
	void ignoresLinesThatAreNotAPrice() {
		assertTrue(Listing.price(List.of("• Prix unité 0.65$/u", "• Vendeur Prix"), PRICE).isEmpty());
		assertTrue(Listing.price(List.of(), PRICE).isEmpty());
	}

	@Test
	void separatorsAreReadAsThousandsUnlessFollowedByCents() {
		assertEquals(1000.0, Listing.number("1.000").getAsDouble());
		assertEquals(10_000_000.0, Listing.number("10.000.000").getAsDouble());
		assertEquals(1.5, Listing.number("1,5").getAsDouble());
		assertEquals(1234.56, Listing.number("1 234,56").getAsDouble());
		assertEquals(15000.0, Listing.number("15 000").getAsDouble());
	}

	@Test
	void gapIsRelativeToTheReference() {
		assertEquals(3.0, Listing.gap(1_000_000, 250_000));
		assertEquals(-0.4, Listing.gap(60, 100), 1e-9);
	}
}
