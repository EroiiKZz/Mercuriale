package fr.imero.mercuriale.client;

import fr.imero.mercuriale.config.MercurialeConfig;
import fr.imero.mercuriale.price.PriceFormat;
import fr.imero.mercuriale.price.PriceTable;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

public final class StockValue {
	public record Segment(String label, String amount) {
	}

	private StockValue() {
	}

	public static List<Segment> segments(ItemStack stack, long count) {
		MercurialeConfig config = MercurialeConfig.get();
		PriceTable.Quote quote = count <= 0 ? null : PriceLookup.shown(stack);
		if (quote == null) {
			return List.of();
		}
		List<Segment> segments = new ArrayList<>();
		if (quote.shop() != null && quote.shop().sellTo() != null) {
			segments.add(new Segment(I18n.get("mercuriale.stock.resale"),
				PriceFormat.money(quote.shop().sellTo() * count)));
		}
		if (quote.shop() != null && quote.shop().buyFrom() != null) {
			double unit = quote.shop().buyFrom() * (100 - config.shopUndercutPercent) / 100.0;
			segments.add(new Segment(I18n.get("mercuriale.stock.own_shop", PriceFormat.money(unit)),
				PriceFormat.money(unit * count)));
		}
		if (segments.isEmpty() && quote.market() != null) {
			segments.add(new Segment(I18n.get("mercuriale.stock.market", PriceFormat.money(quote.market().price())),
				PriceFormat.money(quote.market().price() * count)));
		}
		return segments;
	}

	public static OptionalDouble total(ItemStack stack, long count) {
		PriceTable.Quote quote = count <= 0 ? null : PriceLookup.shown(stack);
		if (quote == null) {
			return OptionalDouble.empty();
		}
		OptionalDouble unit = quote.reference();
		return unit.isPresent() ? OptionalDouble.of(unit.getAsDouble() * count) : OptionalDouble.empty();
	}
}
