package fr.imero.mercuriale.client;

import fr.imero.mercuriale.config.MercurialeConfig;
import fr.imero.mercuriale.price.Listing;
import fr.imero.mercuriale.price.PriceTable;
import fr.imero.mercuriale.theme.AeternumTheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.regex.Pattern;

public final class Bargains {
	private static final double EVEN = 0.05;
	private static final int PERCENT = 100;

	private Bargains() {
	}

	public static PriceLines.Line line(ItemStack stack, PriceTable.Quote quote, int muted) {
		MercurialeConfig config = MercurialeConfig.get();
		if (!config.bargains || !onAuctionScreen(config)) {
			return null;
		}
		OptionalDouble lot = Listing.price(lore(stack), config.listingPrice());
		if (lot.isEmpty()) {
			return null;
		}
		boolean market = quote.market() != null && quote.market().price() > 0;
		Double reference = market ? Double.valueOf(quote.market().price())
			: quote.shop() == null ? null : quote.shop().buyFrom();
		if (reference == null || reference <= 0) {
			return null;
		}
		double unit = lot.getAsDouble() / Math.max(1, stack.getCount());
		double gap = Listing.gap(unit, reference);
		Component source = Component.translatable(market ? "mercuriale.bargain.market" : "mercuriale.bargain.shops");
		Component value;
		if (Math.abs(gap) < EVEN) {
			value = Component.translatable(market ? "mercuriale.bargain.even_market" : "mercuriale.bargain.even_shops")
				.withStyle(style -> style.withColor(muted));
		} else {
			int rgb = gap < 0 ? AeternumTheme.palette().good() : AeternumTheme.palette().bad();
			long percent = Math.round(gap * PERCENT);
			MutableComponent amount = Component.literal((percent > 0 ? "+" : "") + percent + " %")
				.withStyle(style -> style.withColor(rgb));
			value = amount.append(Component.translatable("mercuriale.bargain.versus", source)
				.withStyle(style -> style.withColor(muted)));
		}
		return new PriceLines.Line(Component.translatable("mercuriale.bargain.label"), value);
	}

	private static boolean onAuctionScreen(MercurialeConfig config) {
		Pattern title = config.auctionTitle();
		Screen screen = GameBridge.screen();
		if (title == null || !(screen instanceof AbstractContainerScreen<?>)) {
			return false;
		}
		String text = ChatFormatting.stripFormatting(screen.getTitle().getString());
		return text != null && title.matcher(text).find();
	}

	private static List<String> lore(ItemStack stack) {
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore == null) {
			return List.of();
		}
		List<String> lines = new ArrayList<>();
		for (Component line : lore.lines()) {
			lines.add(ChatFormatting.stripFormatting(line.getString()));
		}
		return lines;
	}
}
