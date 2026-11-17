package fr.imero.mercuriale.price;

import fr.imero.mercuriale.config.PriceAmount;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;

public final class PanelMath {
	public static final double EVEN = 0.05;
	public static final int SEPARATOR = 1;

	public enum Role {
		GOOD,
		FAIR,
		BAD,
		MUTED
	}

	public enum Kind {
		UNIT,
		USUAL,
		STACK,
		SHOP,
		TOTAL
	}

	public enum PartKind {
		ITEM,
		RUNE,
		ENCHANTS,
		AUGMENTERS
	}

	public record Headline(Kind kind, double amount, boolean atLeast, int stack, double stackAmount) {
		public boolean hasStackLine() {
			return stack > 1;
		}
	}

	public record Gauge(int notches, Role role) {
	}

	public record Part(PartKind kind, double value, int unpriced, int count) {
	}

	public record Segment(Part part, int x, int width) {
	}

	private PanelMath() {
	}

	public static Headline headline(MarketPrice market, ShopPrice shop, List<Part> parts, PriceAmount amount,
			int count) {
		boolean marketPriced = market != null && market.price() > 0;
		boolean shopPriced = shop != null && shop.buyFrom() != null;
		if (!parts.isEmpty() && (parts.size() > 1 || !marketPriced && !shopPriced)) {
			return new Headline(Kind.TOTAL, total(parts), unpriced(parts) > 0, 0, 0.0);
		}
		int stack = amount.addsStackLine(count) ? count : 0;
		double factor = amount.scalesToStack(count) ? count : 1.0;
		if (marketPriced) {
			Kind kind = amount.scalesToStack(count) ? Kind.STACK : count > 1 ? Kind.UNIT : Kind.USUAL;
			return new Headline(kind, market.price() * factor, false, stack, market.price() * count);
		}
		if (shopPriced) {
			return new Headline(Kind.SHOP, shop.buyFrom() * factor, false, stack, shop.buyFrom() * count);
		}
		return null;
	}

	public static Gauge gauge(MarketPrice.Tier tier) {
		return switch (tier) {
			case SOLD -> new Gauge(3, Role.GOOD);
			case ASKED -> new Gauge(2, Role.FAIR);
			case THIN -> new Gauge(1, Role.BAD);
		};
	}

	public static Role gapRole(double gap) {
		if (gap <= -EVEN) {
			return Role.GOOD;
		}
		return gap >= EVEN ? Role.FAIR : Role.MUTED;
	}

	public static OptionalDouble shopDiscount(Double buyFrom, MarketPrice market) {
		if (buyFrom == null || market == null || market.price() <= 0) {
			return OptionalDouble.empty();
		}
		double gap = buyFrom / market.price() - 1.0;
		return gap <= -EVEN ? OptionalDouble.of(gap) : OptionalDouble.empty();
	}

	public static String signedPercent(double gap) {
		long percent = Math.round(gap * 100.0);
		return (percent > 0 ? "+" : "") + percent + " %";
	}

	public static double total(List<Part> parts) {
		double total = 0.0;
		for (Part part : parts) {
			total += part.value();
		}
		return total;
	}

	public static int unpriced(List<Part> parts) {
		int missing = 0;
		for (Part part : parts) {
			missing += part.unpriced();
		}
		return missing;
	}

	public static List<Part> byValue(List<Part> parts) {
		List<Part> sorted = new ArrayList<>(parts);
		sorted.sort(Comparator.comparingDouble(Part::value).reversed());
		return sorted;
	}

	public static List<Segment> segments(List<Part> parts, int width) {
		List<Part> priced = new ArrayList<>();
		for (Part part : byValue(parts)) {
			if (part.value() > 0) {
				priced.add(part);
			}
		}
		List<Segment> segments = new ArrayList<>(priced.size());
		int available = width - SEPARATOR * (priced.size() - 1);
		if (priced.isEmpty() || available < priced.size()) {
			return segments;
		}
		double total = total(priced);
		int x = 0;
		int left = available;
		for (int i = 0; i < priced.size(); i++) {
			int remaining = priced.size() - i - 1;
			int span = i == priced.size() - 1 ? left
				: Math.clamp(Math.round(available * priced.get(i).value() / total), 1, left - remaining);
			segments.add(new Segment(priced.get(i), x, span));
			x += span + SEPARATOR;
			left -= span;
		}
		return segments;
	}
}
