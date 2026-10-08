package fr.imero.mercuriale.client;

import fr.imero.mercuriale.price.CustomEnchants;
import fr.imero.mercuriale.price.MarketPrice;
import fr.imero.mercuriale.price.PriceFormat;
import fr.imero.mercuriale.price.PriceTable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.OptionalDouble;

public final class EnchantLines {
	private static final String SEPARATOR = " · ";

	private EnchantLines() {
	}

	public static void append(ItemStack stack, PriceTable.Quote quote, List<PriceLines.Line> lines, int muted) {
		CustomEnchants custom = CustomEnchants.read(Bargains.lore(stack));
		if (custom.isEmpty()) {
			return;
		}
		PriceTable table = PriceService.table();
		CustomEnchants.Appraisal appraisal = custom.appraise(table);
		boolean complete = true;

		OptionalDouble base = base(stack, quote, table);
		double total = base.orElse(0.0);
		complete &= base.isPresent();

		int count = custom.enchants().size();
		if (count > 0) {
			MutableComponent value = money(appraisal.enchants(), count > appraisal.unpriced());
			if (appraisal.unpriced() > 0) {
				value.append(note(Component.translatable("mercuriale.enchants.unpriced", appraisal.unpriced()), muted));
				complete = false;
			}
			lines.add(new PriceLines.Line(Component.translatable("mercuriale.enchants.custom", count), value));
			total += appraisal.enchants();
		}

		int augmenters = custom.augmenters();
		if (augmenters > 0) {
			MutableComponent value = money(appraisal.augmenters(augmenters), appraisal.augmenter() != null);
			value.append(note(Component.translatable("mercuriale.enchants.slots", custom.freeSlots(), custom.slots()), muted));
			lines.add(new PriceLines.Line(Component.translatable("mercuriale.enchants.augmenters", augmenters), value));
			total += appraisal.augmenters(augmenters);
			complete &= appraisal.augmenter() != null;
		}

		if (custom.rune()) {
			MarketPrice rune = appraisal.rune();
			lines.add(new PriceLines.Line(Component.translatable("mercuriale.enchants.rune"),
				money(rune == null ? 0.0 : rune.price(), rune != null)));
			if (rune != null) {
				total += rune.price();
			}
			complete &= rune != null;
		}

		// Un prix manquant ne casse pas le total : il devient un plancher, comme sur VkAnalyzer
		String money = PriceFormat.money(total);
		Component value = complete ? Component.literal(money) : Component.translatable("mercuriale.enchants.incomplete", money);
		lines.add(new PriceLines.Line(Component.translatable("mercuriale.enchants.total"), value));
	}

	// Prix de l'objet nu : son propre prix, sinon celui du matériau non renommé
	private static OptionalDouble base(ItemStack stack, PriceTable.Quote quote, PriceTable table) {
		if (quote != null && quote.reference().isPresent()) {
			return quote.reference();
		}
		String material = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		return table.quote(material, null).reference();
	}

	private static MutableComponent money(double amount, boolean known) {
		return known ? Component.literal(PriceFormat.money(amount)) : Component.translatable("mercuriale.enchants.no_price");
	}

	private static Component note(Component text, int muted) {
		return Component.literal(SEPARATOR).append(text).withStyle(style -> style.withColor(muted));
	}
}
