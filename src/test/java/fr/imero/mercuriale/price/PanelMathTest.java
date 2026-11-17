package fr.imero.mercuriale.price;

import fr.imero.mercuriale.config.PriceAmount;
import fr.imero.mercuriale.price.PanelMath.Kind;
import fr.imero.mercuriale.price.PanelMath.Part;
import fr.imero.mercuriale.price.PanelMath.PartKind;
import fr.imero.mercuriale.price.PanelMath.Role;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanelMathTest {
	private static MarketPrice market(double price, MarketPrice.Tier tier) {
		return new MarketPrice("diamond", "Diamant", true, "diamant", price, tier, 34, 60, 3, price * 0.8, false);
	}

	private static ShopPrice shop(Double buy, Double sell) {
		return new ShopPrice("diamond", buy, sell, 2, 1);
	}

	@Test
	void marketPriceLeadsPerUnitOrUsual() {
		PanelMath.Headline single = PanelMath.headline(market(100, MarketPrice.Tier.SOLD), null, List.of(),
			PriceAmount.BOTH, 1);
		assertEquals(Kind.USUAL, single.kind());
		assertEquals(100.0, single.amount());
		assertFalse(single.hasStackLine());

		PanelMath.Headline both = PanelMath.headline(market(100, MarketPrice.Tier.SOLD), null, List.of(),
			PriceAmount.BOTH, 64);
		assertEquals(Kind.UNIT, both.kind());
		assertEquals(100.0, both.amount());
		assertEquals(64, both.stack());
		assertEquals(6400.0, both.stackAmount());

		PanelMath.Headline stack = PanelMath.headline(market(100, MarketPrice.Tier.SOLD), null, List.of(),
			PriceAmount.STACK, 64);
		assertEquals(Kind.STACK, stack.kind());
		assertEquals(6400.0, stack.amount());
		assertFalse(stack.hasStackLine());
	}

	@Test
	void shopLeadsWithoutMarketAndNothingWithoutEither() {
		PanelMath.Headline shop = PanelMath.headline(null, shop(40.0, 10.0), List.of(), PriceAmount.UNIT, 3);
		assertEquals(Kind.SHOP, shop.kind());
		assertEquals(40.0, shop.amount());
		assertNull(PanelMath.headline(null, shop(null, 10.0), List.of(), PriceAmount.UNIT, 1));
	}

	@Test
	void enchantValueLeadsWithTotalAndAtLeastWhenAPriceIsMissing() {
		List<Part> parts = List.of(new Part(PartKind.ITEM, 500, 0, 1), new Part(PartKind.ENCHANTS, 300, 1, 3));
		PanelMath.Headline total = PanelMath.headline(market(500, MarketPrice.Tier.SOLD), null, parts,
			PriceAmount.UNIT, 1);
		assertEquals(Kind.TOTAL, total.kind());
		assertEquals(800.0, total.amount());
		assertTrue(total.atLeast());

		List<Part> booksOnly = List.of(new Part(PartKind.ENCHANTS, 300, 0, 2));
		assertEquals(Kind.TOTAL, PanelMath.headline(null, null, booksOnly, PriceAmount.UNIT, 1).kind());
		assertFalse(PanelMath.headline(null, null, booksOnly, PriceAmount.UNIT, 1).atLeast());
	}

	@Test
	void gaugeFollowsTheTier() {
		assertEquals(new PanelMath.Gauge(3, Role.GOOD), PanelMath.gauge(MarketPrice.Tier.SOLD));
		assertEquals(new PanelMath.Gauge(2, Role.FAIR), PanelMath.gauge(MarketPrice.Tier.ASKED));
		assertEquals(new PanelMath.Gauge(1, Role.BAD), PanelMath.gauge(MarketPrice.Tier.THIN));
	}

	@Test
	void fivePercentThresholdForShopsAndPills() {
		MarketPrice reference = market(100, MarketPrice.Tier.SOLD);
		assertTrue(PanelMath.shopDiscount(72.0, reference).isPresent());
		assertEquals("-28 %", PanelMath.signedPercent(PanelMath.shopDiscount(72.0, reference).getAsDouble()));
		assertTrue(PanelMath.shopDiscount(95.0, reference).isPresent());
		assertFalse(PanelMath.shopDiscount(96.0, reference).isPresent());
		assertFalse(PanelMath.shopDiscount(72.0, null).isPresent());

		assertEquals(Role.GOOD, PanelMath.gapRole(-0.05));
		assertEquals(Role.MUTED, PanelMath.gapRole(-0.049));
		assertEquals(Role.MUTED, PanelMath.gapRole(0.049));
		assertEquals(Role.FAIR, PanelMath.gapRole(0.05));
		assertEquals("+12 %", PanelMath.signedPercent(0.12));
		assertEquals("0 %", PanelMath.signedPercent(0.001));
	}

	@Test
	void compositionIsProportionalSortedAndSkipsUnpricedParts() {
		List<Part> parts = List.of(new Part(PartKind.ITEM, 100, 0, 1), new Part(PartKind.ENCHANTS, 300, 0, 3),
			new Part(PartKind.RUNE, 0, 1, 1));
		List<PanelMath.Segment> segments = PanelMath.segments(parts, 101);
		assertEquals(2, segments.size());
		assertEquals(PartKind.ENCHANTS, segments.get(0).part().kind());
		assertEquals(0, segments.get(0).x());
		assertEquals(75, segments.get(0).width());
		assertEquals(76, segments.get(1).x());
		assertEquals(25, segments.get(1).width());
		assertEquals(PartKind.RUNE, PanelMath.byValue(parts).getLast().kind());
	}

	@Test
	void tinyPartsKeepAVisibleSegment() {
		List<Part> parts = List.of(new Part(PartKind.ITEM, 10000, 0, 1), new Part(PartKind.AUGMENTERS, 1, 0, 2));
		List<PanelMath.Segment> segments = PanelMath.segments(parts, 50);
		assertEquals(48, segments.get(0).width());
		assertEquals(1, segments.get(1).width());
		assertEquals(49, segments.get(1).x());
	}
}
