package fr.imero.mercuriale.price;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PriceFormat {
	private static final String CURRENCY = " $";
	private static final int DECIMALS = 2;

	private PriceFormat() {
	}

	public static String money(double value) {
		BigDecimal rounded = BigDecimal.valueOf(Math.abs(value)).setScale(DECIMALS, RoundingMode.HALF_UP)
			.stripTrailingZeros();
		String plain = rounded.toPlainString();
		int dot = plain.indexOf('.');
		String whole = dot < 0 ? plain : plain.substring(0, dot);
		String fraction = dot < 0 ? "" : "," + plain.substring(dot + 1);
		String sign = value < 0 && rounded.signum() != 0 ? "-" : "";
		return sign + grouped(whole) + fraction + CURRENCY;
	}

	private static String grouped(String digits) {
		StringBuilder out = new StringBuilder(digits.length() + digits.length() / 3);
		int lead = digits.length() % 3;
		for (int i = 0; i < digits.length(); i++) {
			if (i > 0 && (i - lead) % 3 == 0) {
				out.append(' ');
			}
			out.append(digits.charAt(i));
		}
		return out.toString();
	}
}
