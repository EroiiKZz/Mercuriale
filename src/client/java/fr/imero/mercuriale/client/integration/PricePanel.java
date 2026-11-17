package fr.imero.mercuriale.client.integration;

import fr.imero.mercuriale.client.Bargains;
import fr.imero.mercuriale.client.PriceLines;
import fr.imero.mercuriale.client.PriceLookup;
import fr.imero.mercuriale.config.MercurialeConfig;
import fr.imero.mercuriale.config.PriceAmount;
import fr.imero.mercuriale.price.CustomEnchants;
import fr.imero.mercuriale.client.screen.Shape;
import fr.imero.mercuriale.client.screen.Theme;
import fr.imero.mercuriale.price.MarketPrice;
import fr.imero.mercuriale.price.PanelMath;
import fr.imero.mercuriale.price.PriceFormat;
import fr.imero.mercuriale.price.PriceTable;
import fr.imero.mercuriale.price.ShopPrice;
import fr.imero.vitrine.api.TooltipBlock;
import fr.imero.vitrine.api.TooltipPalette;
import fr.imero.vitrine.api.TooltipText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

final class PricePanel implements TooltipBlock {
	private static final int MIN_WIDTH = 140;
	private static final int PAD_X = 5;
	private static final int PAD_Y = 4;
	private static final int TILE_PAD = 3;
	private static final int TILE_GAP = 3;
	private static final int NOTCH_WIDTH = 5;
	private static final int NOTCH_HEIGHT = 3;
	private static final int NOTCH_GAP = 2;
	private static final int NOTCHES = 3;
	private static final int BAR = 4;
	private static final int SQUARE = 5;
	private static final int COLUMN_GAP = 6;
	private static final int LEGEND_LINE = 10;
	private static final int PILL_PAD = 3;
	private static final int PILL_GAP = 6;
	private static final int PILL_ALPHA = 0x24;
	private static final int AUGMENTERS = 0xFFB98CFF;
	private static final float BIG = 1.5f;
	private static final float MEDIUM = 1.0f;
	private static final String SEPARATOR = " · ";

	private final PanelMath.Headline headline;
	private final MarketPrice market;
	private final ShopPrice shop;
	private final List<PanelMath.Part> parts;
	private final Bargains.Gap bargain;
	private final double factor;

	PricePanel(PanelMath.Headline headline, MarketPrice market, ShopPrice shop, List<PanelMath.Part> parts,
			Bargains.Gap bargain, double factor) {
		this.headline = headline;
		this.market = market != null && market.price() > 0 ? market : null;
		this.shop = shop != null && !shop.isEmpty() ? shop : null;
		this.parts = parts;
		this.bargain = bargain;
		this.factor = factor;
	}

	static PricePanel of(ItemStack stack) {
		PriceTable.Quote quote = PriceLookup.shown(stack);
		CustomEnchants found = PriceLines.showsEnchants() ? PriceLookup.customEnchants(stack) : CustomEnchants.NONE;
		List<PanelMath.Part> parts = PriceLines.valuation(found, quote);
		PriceAmount amount = MercurialeConfig.get().amount;
		int count = stack.getCount();
		MarketPrice market = quote == null ? null : quote.market();
		ShopPrice shop = quote == null ? null : quote.shop();
		PricePanel panel = new PricePanel(PanelMath.headline(market, shop, parts, amount, count), market, shop, parts,
			quote == null ? null : Bargains.gap(stack, quote), amount.scalesToStack(count) ? count : 1.0);
		return panel.isEmpty() ? null : panel;
	}

	boolean isEmpty() {
		return headline == null && shop == null && parts.isEmpty();
	}

	@Override
	public int minWidth(Font font) {
		int need = font.width(Component.translatable("mercuriale.price.header")) + COLUMN_GAP
			+ font.width(Component.translatable("mercuriale.price.source"));
		if (headline != null) {
			int amount = TooltipText.width(font, headlineAmount(), BIG)
				+ (bargain == null ? 0 : PILL_GAP + pillWidth(font, bargainText()));
			int text = Math.max(amount, font.width(headlineLabel()));
			if (headline.hasStackLine()) {
				text = Math.max(text, font.width(stackLine()));
			}
			int gauge = market == null ? 0 : COLUMN_GAP + Math.max(notchesWidth(), font.width(basis()));
			need = Math.max(need, text + gauge);
		}
		if (!parts.isEmpty()) {
			int column = 0;
			for (PanelMath.Part part : parts) {
				column = Math.max(column, SQUARE + 3 + font.width(PriceLines.partLabel(part)) + COLUMN_GAP
					+ font.width(partValue(part)));
			}
			need = Math.max(need, column * 2 + COLUMN_GAP);
		}
		if (shop != null) {
			List<SourceTile> tiles = sourceTiles();
			int tile = 0;
			for (SourceTile source : tiles) {
				int text = Math.max(font.width(source.label()), font.width(source.note()));
				text = Math.max(text, TooltipText.width(font, money(source.amount()), MEDIUM));
				tile = Math.max(tile, text + (TILE_PAD + 1) * 2);
			}
			need = Math.max(need, tiles.size() * tile + (tiles.size() - 1) * TILE_GAP);
			Component summary = summary();
			if (summary != null) {
				need = Math.max(need, font.width(summary));
			}
		}
		if (market != null && market.onSale() == 0) {
			need = Math.max(need, font.width(Component.translatable("mercuriale.panel.none_listed")));
		} else if (market != null && market.cheapest() != null) {
			need = Math.max(need, (TILE_PAD + 1) * 2 + font.width(listingLabel()) + 4
				+ TooltipText.width(font, money(market.cheapest() * factor), MEDIUM) + PILL_GAP
				+ pillWidth(font, listingPercent()));
		}
		return Math.max(MIN_WIDTH, need + PAD_X * 2);
	}

	private Component headlineAmount() {
		return Component.literal((headline.atLeast() ? "≥ " : "") + PriceFormat.money(headline.amount()));
	}

	private Component stackLine() {
		return Component.literal(PriceFormat.money(headline.stackAmount()) + " ")
			.append(Component.translatable("mercuriale.panel.stack", headline.stack()));
	}

	private static Component partValue(PanelMath.Part part) {
		return part.value() <= 0 && part.unpriced() > 0
			? Component.translatable("mercuriale.price.unpriced", part.unpriced())
			: money(part.value());
	}

	private Component listingLabel() {
		return Component.translatable("mercuriale.panel.on_sale", market.onSale());
	}

	private double listingGap() {
		return market.cheapest() / market.price() - 1.0;
	}

	private Component listingPercent() {
		return Component.literal(PanelMath.signedPercent(listingGap()));
	}

	private static Component money(double amount) {
		return Component.literal(PriceFormat.money(amount));
	}

	private static int notchesWidth() {
		return NOTCHES * NOTCH_WIDTH + (NOTCHES - 1) * NOTCH_GAP;
	}

	private static int pillWidth(Font font, Component text) {
		return font.width(text) + PILL_PAD * 2;
	}

	@Override
	public int height(int width, Font font) {
		return run(null, font, 0, 0, width, null);
	}

	@Override
	public void draw(GuiGraphicsExtractor context, Font font, int x, int y, int width, TooltipPalette palette) {
		int height = run(null, font, x, y, width, palette);
		Shape.fill(context, x, y, x + width, y + height, Theme.RADIUS, palette.surface());
		run(context, font, x, y, width, palette);
	}

	private int run(GuiGraphicsExtractor context, Font font, int x, int y, int width, TooltipPalette palette) {
		int left = x + PAD_X;
		int inner = width - PAD_X * 2;
		int cursor = y + PAD_Y;
		if (context != null) {
			context.text(font, Component.translatable("mercuriale.price.header"), left, cursor, palette.accent(), false);
			Component source = Component.translatable("mercuriale.price.source");
			context.text(font, source, left + inner - font.width(source), cursor, palette.muted(), false);
		}
		cursor += font.lineHeight;
		if (headline != null || market != null) {
			cursor = headline(context, font, left, cursor + Theme.SPACE_2, inner, palette);
		}
		if (!parts.isEmpty()) {
			cursor = composition(context, font, left, cursor + Theme.SPACE_2, inner, palette);
		}
		if (shop != null) {
			cursor = sources(context, font, left, cursor + Theme.SPACE_2, inner, palette);
		}
		if (market != null && (market.onSale() == 0 || market.cheapest() != null)) {
			cursor = listings(context, font, left, cursor + Theme.SPACE_2, inner, palette);
		}
		return cursor + PAD_Y - y;
	}

	private int headline(GuiGraphicsExtractor context, Font font, int left, int top, int inner,
			TooltipPalette palette) {
		int cursor = top;
		if (headline != null) {
			Component amount = headlineAmount();
			int amountTop = cursor + font.lineHeight + 2;
			int amountHeight = TooltipText.height(font, BIG);
			if (context != null) {
				context.text(font, headlineLabel(), left, cursor, palette.muted(), false);
				TooltipText.scaled(context, font, amount, left, amountTop, palette.text(), BIG);
				if (bargain != null) {
					int pillX = left + TooltipText.width(font, amount, BIG) + PILL_GAP;
					pill(context, font, bargainText(), pillX, amountTop + (amountHeight - font.lineHeight - 2) / 2,
						bargainRole(), palette);
				}
			}
			cursor = amountTop + amountHeight;
			if (headline.hasStackLine()) {
				if (context != null) {
					context.text(font, stackLine(), left, cursor + 2, palette.muted(), false);
				}
				cursor += 2 + font.lineHeight;
			}
		}
		if (market != null) {
			PanelMath.Gauge gauge = PanelMath.gauge(market.tier());
			int right = left + inner;
			int notchesWidth = notchesWidth();
			if (context != null) {
				for (int i = 0; i < NOTCHES; i++) {
					int nx = right - notchesWidth + i * (NOTCH_WIDTH + NOTCH_GAP);
					int color = i < gauge.notches() ? role(gauge.role(), palette) : palette.border();
					context.fill(nx, top + 1, nx + NOTCH_WIDTH, top + 1 + NOTCH_HEIGHT, color);
				}
				Component basis = basis();
				context.text(font, basis, right - font.width(basis), top + NOTCH_HEIGHT + 4, palette.muted(), false);
			}
			cursor = Math.max(cursor, top + NOTCH_HEIGHT + 4 + font.lineHeight);
		}
		return cursor;
	}

	private int composition(GuiGraphicsExtractor context, Font font, int left, int top, int inner,
			TooltipPalette palette) {
		if (context != null) {
			for (PanelMath.Segment segment : PanelMath.segments(parts, inner)) {
				context.fill(left + segment.x(), top, left + segment.x() + segment.width(), top + BAR,
					partColor(segment.part().kind(), palette));
			}
		}
		int legendTop = top + BAR + 4;
		List<PanelMath.Part> ordered = PanelMath.byValue(parts);
		int column = (inner - COLUMN_GAP) / 2;
		if (context != null) {
			for (int i = 0; i < ordered.size(); i++) {
				PanelMath.Part part = ordered.get(i);
				int cx = left + (i % 2) * (column + COLUMN_GAP);
				int cy = legendTop + (i / 2) * LEGEND_LINE;
				context.fill(cx, cy + 2, cx + SQUARE, cy + 2 + SQUARE, partColor(part.kind(), palette));
				context.text(font, PriceLines.partLabel(part), cx + SQUARE + 3, cy, palette.muted(), false);
				boolean unpriced = part.value() <= 0 && part.unpriced() > 0;
				Component value = partValue(part);
				context.text(font, value, cx + column - font.width(value), cy,
					unpriced ? palette.muted() : palette.text(), false);
			}
		}
		int rows = (ordered.size() + 1) / 2;
		return legendTop + rows * LEGEND_LINE - 1;
	}

	private record SourceTile(Component label, double amount, Component note, boolean highlight) {
	}

	private int sources(GuiGraphicsExtractor context, Font font, int left, int top, int inner,
			TooltipPalette palette) {
		int tileHeight = TILE_PAD * 2 + font.lineHeight * 2 + 2 + TooltipText.height(font, MEDIUM);
		return sourcesBody(context, font, left, top, inner, palette, sourceTiles(), tileHeight);
	}

	private List<SourceTile> sourceTiles() {
		List<SourceTile> tiles = new ArrayList<>(3);
		if (market != null) {
			tiles.add(new SourceTile(Component.translatable("mercuriale.panel.tile.market"), market.price() * factor,
				Component.translatable("mercuriale.panel.usual_short"), false));
		}
		if (shop.buyFrom() != null) {
			OptionalDouble discount = PanelMath.shopDiscount(shop.buyFrom(), market);
			tiles.add(new SourceTile(Component.translatable("mercuriale.panel.tile.shop"), shop.buyFrom() * factor,
				discount.isPresent() ? Component.literal(PanelMath.signedPercent(discount.getAsDouble()))
					: Component.translatable("mercuriale.panel.shop_short"), discount.isPresent()));
		}
		if (shop.sellTo() != null) {
			tiles.add(new SourceTile(Component.translatable("mercuriale.panel.tile.buyback"), shop.sellTo() * factor,
				Component.translatable("mercuriale.panel.highest"), false));
		}
		return tiles;
	}

	private int sourcesBody(GuiGraphicsExtractor context, Font font, int left, int top, int inner,
			TooltipPalette palette, List<SourceTile> tiles, int tileHeight) {
		int tileWidth = (inner - (tiles.size() - 1) * TILE_GAP) / Math.max(1, tiles.size());
		if (context != null) {
			for (int i = 0; i < tiles.size(); i++) {
				SourceTile tile = tiles.get(i);
				int tx = left + i * (tileWidth + TILE_GAP);
				int accent = tile.highlight() ? palette.good() : palette.text();
				Shape.fill(context, tx, top, tx + tileWidth, top + tileHeight, Theme.TILE_RADIUS, palette.background());
				Shape.stroke(context, tx, top, tx + tileWidth, top + tileHeight, Theme.TILE_RADIUS, 1,
					tile.highlight() ? palette.good() : palette.border());
				int ty = top + TILE_PAD;
				context.text(font, tile.label(), tx + TILE_PAD + 1, ty, palette.muted(), false);
				ty += font.lineHeight + 1;
				TooltipText.scaled(context, font, Component.literal(PriceFormat.money(tile.amount())), tx + TILE_PAD + 1,
					ty, accent, MEDIUM);
				ty += TooltipText.height(font, MEDIUM) + 1;
				context.text(font, tile.note(), tx + TILE_PAD + 1, ty, tile.highlight() ? palette.good() : palette.muted(),
					false);
			}
		}
		int cursor = top + tileHeight;
		Component summary = summary();
		if (summary != null) {
			if (context != null) {
				context.text(font, summary, left, cursor + 3, palette.muted(), false);
			}
			cursor += 3 + font.lineHeight;
		}
		return cursor;
	}

	private int listings(GuiGraphicsExtractor context, Font font, int left, int top, int inner,
			TooltipPalette palette) {
		if (market.onSale() == 0) {
			if (context != null) {
				context.text(font, Component.translatable("mercuriale.panel.none_listed"), left, top, palette.muted(),
					false);
			}
			return top + font.lineHeight;
		}
		int amountHeight = TooltipText.height(font, MEDIUM);
		int height = TILE_PAD * 2 + amountHeight;
		if (context != null) {
			Shape.fill(context, left, top, left + inner, top + height, Theme.TILE_RADIUS, palette.background());
			Shape.stroke(context, left, top, left + inner, top + height, Theme.TILE_RADIUS, 1, palette.border());
			Component label = listingLabel();
			int textY = top + TILE_PAD + (amountHeight - font.lineHeight) / 2;
			context.text(font, label, left + TILE_PAD + 1, textY, palette.muted(), false);
			TooltipText.scaled(context, font, money(market.cheapest() * factor),
				left + TILE_PAD + 1 + font.width(label) + 4, top + TILE_PAD, palette.text(), MEDIUM);
			Component percent = listingPercent();
			pill(context, font, percent, left + inner - TILE_PAD - pillWidth(font, percent),
				top + (height - font.lineHeight - 2) / 2, PanelMath.gapRole(listingGap()), palette);
		}
		return top + height;
	}

	private void pill(GuiGraphicsExtractor context, Font font, Component text, int x, int y, PanelMath.Role role,
			TooltipPalette palette) {
		int color = role(role, palette);
		int width = font.width(text) + PILL_PAD * 2;
		Shape.fill(context, x, y, x + width, y + font.lineHeight + 2, (font.lineHeight + 2) / 2,
			(PILL_ALPHA << 24) | (color & 0xFFFFFF));
		context.text(font, text, x + PILL_PAD, y + 2, color, false);
	}

	private Component headlineLabel() {
		return switch (headline.kind()) {
			case TOTAL -> Component.translatable("mercuriale.panel.total");
			case UNIT -> Component.translatable("mercuriale.panel.unit");
			case USUAL -> Component.translatable("mercuriale.panel.usual");
			case STACK -> Component.translatable("mercuriale.panel.stack_label", Math.round(factor));
			case SHOP -> Component.translatable("mercuriale.panel.shop");
		};
	}

	private Component basis() {
		int samples = market.samples();
		Component count = market.tier() == MarketPrice.Tier.SOLD
			? Component.translatable("mercuriale.price.basis_sold", samples)
			: samples <= 1 ? Component.translatable("mercuriale.price.basis_one")
			: Component.translatable("mercuriale.price.basis_listed", samples);
		Integer days = market.windowDays();
		if (days == null || days <= 0) {
			return count;
		}
		return Component.empty().append(count).append(SEPARATOR)
			.append(Component.translatable("mercuriale.panel.window", days));
	}

	private Component summary() {
		Component sellers = shop.sellers() > 0 ? Component.translatable("mercuriale.panel.sellers", shop.sellers()) : null;
		Component buyers = shop.buyers() > 0 ? Component.translatable("mercuriale.panel.buyers", shop.buyers()) : null;
		if (sellers != null && buyers != null) {
			return Component.empty().append(sellers).append(SEPARATOR).append(buyers);
		}
		return sellers != null ? sellers : buyers;
	}

	private Component bargainText() {
		Component source = Component.translatable(bargain.market() ? "mercuriale.bargain.market" : "mercuriale.bargain.shops");
		if (bargain.even()) {
			return Component.translatable(bargain.market() ? "mercuriale.bargain.even_market" : "mercuriale.bargain.even_shops");
		}
		return Component.literal(PanelMath.signedPercent(bargain.gap()))
			.append(Component.translatable("mercuriale.bargain.versus", source));
	}

	private PanelMath.Role bargainRole() {
		if (bargain.even()) {
			return PanelMath.Role.MUTED;
		}
		return bargain.gap() < 0 ? PanelMath.Role.GOOD : PanelMath.Role.BAD;
	}

	private static int role(PanelMath.Role role, TooltipPalette palette) {
		return switch (role) {
			case GOOD -> palette.good();
			case FAIR -> palette.fair();
			case BAD -> palette.bad();
			case MUTED -> palette.muted();
		};
	}

	private static int partColor(PanelMath.PartKind kind, TooltipPalette palette) {
		return switch (kind) {
			case ITEM -> palette.accent();
			case RUNE -> palette.fair();
			case ENCHANTS -> palette.secondary();
			case AUGMENTERS -> AUGMENTERS;
		};
	}
}
