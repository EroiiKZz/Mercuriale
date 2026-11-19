package fr.imero.mercuriale.client;

import fr.imero.mercuriale.config.MercurialeConfig;
import fr.imero.mercuriale.config.PriceAmount;
import fr.imero.mercuriale.price.CustomEnchants;
import fr.imero.mercuriale.price.MarketPrice;
import fr.imero.mercuriale.price.PanelMath;
import fr.imero.mercuriale.price.PriceFormat;
import fr.imero.mercuriale.price.PriceTable;
import fr.imero.mercuriale.price.ShopPrice;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.WeakHashMap;

public final class PriceLines {
	private static final String SEPARATOR = " · ";
	private static final Set<Screen> WATCHED = Collections.newSetFromMap(new WeakHashMap<>());

	private static boolean enchantKeyHeld;

	public record Line(Component label, Component value) {
	}

	private PriceLines() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			enchantKeyHeld = false;
			if (WATCHED.add(screen)) {
				MercurialeKeybinds.watch(screen, MercurialeKeybinds.SHOW_ENCHANTS,
					pressed -> enchantKeyHeld |= pressed, released -> enchantKeyHeld &= !released);
			}
		});
	}

	public static List<Line> describe(ItemStack stack, int muted, int bright) {
		PriceTable.Quote quote = PriceLookup.shown(stack);
		ViewLog.note(stack, quote != null);
		List<Line> lines = quote == null ? new ArrayList<>() : quoted(stack, quote, muted, bright);
		CustomEnchants found = showsEnchants() ? PriceLookup.customEnchants(stack) : CustomEnchants.NONE;
		if (!found.isEmpty()) {
			enchanted(lines, found, quote, muted);
		}
		return lines;
	}

	public static boolean showsEnchants() {
		return MercurialeConfig.get().enchantValue || enchantKeyHeld;
	}

	public static List<PanelMath.Part> valuation(CustomEnchants found, PriceTable.Quote quote) {
		List<PanelMath.Part> parts = new ArrayList<>();
		if (found.isEmpty()) {
			return parts;
		}
		PriceTable table = PriceService.table();
		double item = quote == null ? 0.0 : quote.reference().orElse(0.0);
		if (item > 0) {
			parts.add(new PanelMath.Part(PanelMath.PartKind.ITEM, item, 0, 1));
		}
		if (found.enchants() > 0) {
			double books = 0.0;
			int missing = found.enchants() - found.books().size();
			for (String book : found.books()) {
				MarketPrice price = table.quote(CustomEnchants.BOOK_MATERIAL, book).market();
				if (price == null || price.ambiguous()) {
					missing++;
				} else {
					books += price.price();
				}
			}
			parts.add(new PanelMath.Part(PanelMath.PartKind.ENCHANTS, books, missing, found.enchants()));
		}
		if (found.augmenters() > 0) {
			MarketPrice unit = table.quote(CustomEnchants.AUGMENTER_MATERIAL, CustomEnchants.AUGMENTER_NAME).market();
			double cost = unit == null ? 0.0 : unit.price() * found.augmenters();
			parts.add(new PanelMath.Part(PanelMath.PartKind.AUGMENTERS, cost, unit == null ? found.augmenters() : 0,
				found.augmenters()));
		}
		if (found.rune()) {
			MarketPrice rune = table.quote(CustomEnchants.RUNE_MATERIAL, CustomEnchants.RUNE_NAME).market();
			parts.add(new PanelMath.Part(PanelMath.PartKind.RUNE, rune == null ? 0.0 : rune.price(), rune == null ? 1 : 0, 1));
		}
		return parts;
	}

	public static Component partLabel(PanelMath.Part part) {
		return switch (part.kind()) {
			case ITEM -> Component.translatable("mercuriale.panel.item");
			case ENCHANTS -> Component.translatable("mercuriale.price.enchants", part.count());
			case AUGMENTERS -> Component.translatable("mercuriale.price.augmenters", part.count());
			case RUNE -> Component.translatable("mercuriale.price.rune");
		};
	}

	private static void enchanted(List<Line> lines, CustomEnchants found, PriceTable.Quote quote, int muted) {
		List<PanelMath.Part> parts = valuation(found, quote);
		for (PanelMath.Part part : parts) {
			if (part.kind() != PanelMath.PartKind.ITEM) {
				lines.add(new Line(partLabel(part), addOn(part.value(), part.unpriced(), muted)));
			}
		}
		if (parts.size() > 1) {
			lines.add(new Line(Component.translatable("mercuriale.price.total"),
				Component.literal((PanelMath.unpriced(parts) > 0 ? "≥ " : "") + PriceFormat.money(PanelMath.total(parts)))));
		}
	}

	private static Component addOn(double value, int missing, int muted) {
		Component unpriced = Component.translatable("mercuriale.price.unpriced", missing).withStyle(style -> style.withColor(muted));
		if (missing > 0 && value <= 0) {
			return unpriced;
		}
		MutableComponent text = Component.literal(PriceFormat.money(value));
		return missing > 0 ? text.append(Component.literal(SEPARATOR).append(unpriced).withStyle(style -> style.withColor(muted))) : text;
	}

	private static List<Line> quoted(ItemStack stack, PriceTable.Quote quote, int muted, int bright) {
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
