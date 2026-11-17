package fr.imero.mercuriale.price;

import java.util.List;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Listing {
	private static final Pattern SPACING = Pattern.compile("[\\s\\u00a0\\u202f']");
	private static final int MAX_DECIMALS = 2;

	private Listing() {
	}

	public static OptionalDouble price(List<String> lore, Pattern pattern) {
		if (lore == null || pattern == null) {
			return OptionalDouble.empty();
		}
		for (String line : lore) {
			Matcher matcher = pattern.matcher(line);
			if (matcher.find() && matcher.groupCount() >= 1) {
				OptionalDouble value = number(matcher.group(1));
				if (value.isPresent()) {
					return value;
				}
			}
		}
		return OptionalDouble.empty();
	}

	public static OptionalDouble number(String raw) {
		if (raw == null) {
			return OptionalDouble.empty();
		}
		String compact = SPACING.matcher(raw).replaceAll("");
		int last = Math.max(compact.lastIndexOf('.'), compact.lastIndexOf(','));
		String digits;
		if (last >= 0 && compact.length() - last - 1 >= 1 && compact.length() - last - 1 <= MAX_DECIMALS) {
			digits = compact.substring(0, last).replaceAll("[.,]", "") + "." + compact.substring(last + 1);
		} else {
			digits = compact.replaceAll("[.,]", "");
		}
		try {
			return digits.isEmpty() ? OptionalDouble.empty() : OptionalDouble.of(Double.parseDouble(digits));
		} catch (NumberFormatException e) {
			return OptionalDouble.empty();
		}
	}

	public static double gap(double asked, double reference) {
		return asked / reference - 1.0;
	}
}
