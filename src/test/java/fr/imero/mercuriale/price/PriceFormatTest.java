package fr.imero.mercuriale.price;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceFormatTest {
	@Test
	void groupsThousandsWithSpaces() {
		assertEquals("14 $", PriceFormat.money(14));
		assertEquals("6 000 $", PriceFormat.money(6000));
		assertEquals("1 500 000 $", PriceFormat.money(1_500_000));
		assertEquals("999 $", PriceFormat.money(999));
	}

	@Test
	void keepsTwoDecimalsAtMostWithAComma() {
		assertEquals("0,19 $", PriceFormat.money(0.19));
		assertEquals("12,1 $", PriceFormat.money(12.1));
		assertEquals("3,33 $", PriceFormat.money(10.0 / 3.0));
		assertEquals("2 $", PriceFormat.money(1.999));
	}
}
