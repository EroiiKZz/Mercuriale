package fr.imero.mercuriale.client;

import fr.imero.mercuriale.config.MercurialeConfig;
import fr.imero.mercuriale.config.PriceAmount;
import fr.imero.mercuriale.price.MarketPrice;
import fr.imero.mercuriale.price.PriceFormat;
import fr.imero.mercuriale.price.PriceTable;
import fr.imero.mercuriale.price.ShopPrice;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

public final class PriceLines {
	private static final String SEPARATOR = " · ";

	public record Line(Component label, Component value) {
	}

	private PriceLines() {
	}

	public static List<Line> describe(ItemStack stack, int muted, int bright) {
		PriceTable.Quote quote = PriceLookup.shown(stack);
		if (quote == null) {
			return List.of();
		}
		PriceAmount amount = MercurialeConfig.get().amount;
		int count = stack.getCount();
		double factor = amount.scalesToStack(count) ? count : 1.0;

		List<Line> lines = new ArrayList<>();
		Line bargain = Bargains.line(stack, quote, muted);
		if (bargain != null) {
			lines.add(bargain);
		}
		if (quote.market() != null) {
			lines.add(new Line(label("mercuriale.price.market", amount, count), market(quote.market(), factor, muted)));
		}
		if (quote.shop() != null && !quote.shop().isEmpty()) {
			lines.add(new Line(label("mercuriale.price.shops", amount, count), shops(quote.shop(), factor, muted, bright)));
		}
		OptionalDouble reference = quote.reference();
		if (amount.addsStackLine(count) && reference.isPresent()) {
			lines.add(new Line(Component.translatable("mercuriale.price.stack", count),
				Component.literal(PriceFormat.money(reference.getAsDouble() * count))));
		}
		return lines;
	}

	private static Component label(String key, PriceAmount amount, int count) {
		MutableComponent label = Component.translatable(key);
		if (amount.scalesToStack(count)) {
			label.append(" ×" + count);
		}
		return label;
	}

	private static Component market(MarketPrice market, double factor, int muted) {
		return Component.literal(PriceFormat.money(market.price() * factor))
			.append(Component.literal(SEPARATOR).append(basis(market))
				.withStyle(style -> style.withColor(muted)));
	}

	private static Component basis(MarketPrice market) {
		int samples = market.samples();
		if (market.tier() == MarketPrice.Tier.SOLD) {
			return Component.translatable("mercuriale.price.basis_sold", samples);
		}
		return samples <= 1
			? Component.translatable("mercuriale.price.basis_one")
			: Component.translatable("mercuriale.price.basis_listed", samples);
	}

	private static Component shops(ShopPrice shop, double factor, int muted, int bright) {
		MutableComponent value = Component.empty();
		if (shop.buyFrom() != null) {
			value.append(side("mercuriale.price.buy", shop.buyFrom() * factor, muted, bright));
		}
		if (shop.sellTo() != null) {
			if (shop.buyFrom() != null) {
				value.append(Component.literal(SEPARATOR).withStyle(style -> style.withColor(muted)));
			}
			value.append(side("mercuriale.price.sell", shop.sellTo() * factor, muted, bright));
		}
		return value;
	}

	private static Component side(String key, double amount, int muted, int bright) {
		Component money = Component.literal(PriceFormat.money(amount)).withStyle(style -> style.withColor(bright));
		return Component.translatable(key, money).withStyle(style -> style.withColor(muted));
	}
}
