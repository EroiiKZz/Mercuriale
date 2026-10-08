package fr.imero.mercuriale.client;

import fr.imero.mercuriale.config.MercurialeConfig;
import fr.imero.mercuriale.price.PriceTable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.List;
import java.util.Objects;

public final class PriceLookup {
	private static final String VANILLA = "minecraft";
	private static final List<DataComponentType<?>> VARIANT_DATA = List.of(
		DataComponents.STORED_ENCHANTMENTS,
		DataComponents.POTION_CONTENTS,
		DataComponents.PROFILE,
		DataComponents.MAP_ID,
		DataComponents.WRITTEN_BOOK_CONTENT,
		DataComponents.INSTRUMENT,
		DataComponents.SUSPICIOUS_STEW_EFFECTS,
		DataComponents.BLOCK_ENTITY_DATA,
		DataComponents.CONTAINER,
		DataComponents.BUNDLE_CONTENTS);

	private PriceLookup() {
	}

	public static PriceTable.Quote quote(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return null;
		}
		var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (!VANILLA.equals(id.getNamespace())) {
			return null;
		}
		String name = customName(stack);
		if (name == null && definedByItsData(stack)) {
			return null;
		}
		PriceTable.Quote quote = PriceService.table().quote(id.getPath(), name);
		return quote.isEmpty() ? null : quote;
	}

	public static PriceTable.Quote shown(ItemStack stack) {
		MercurialeConfig config = MercurialeConfig.get();
		if (!PriceService.active(Minecraft.getInstance(), config)) {
			return null;
		}
		PriceTable.Quote found = quote(stack);
		if (found == null) {
			return null;
		}
		PriceTable.Quote quote = found.only(config.market, config.shops);
		return quote.isEmpty() ? null : quote;
	}

	private static boolean definedByItsData(ItemStack stack) {
		if (stack.getItem() instanceof BlockItem block && block.getBlock() instanceof ShulkerBoxBlock) {
			return true;
		}
		for (DataComponentType<?> type : VARIANT_DATA) {
			if (stack.has(type)) {
				return true;
			}
		}
		return false;
	}

	private static String customName(ItemStack stack) {
		Component custom = stack.get(DataComponents.CUSTOM_NAME);
		if (custom != null) {
			return ChatFormatting.stripFormatting(custom.getString());
		}
		Component itemName = stack.get(DataComponents.ITEM_NAME);
		Component standard = stack.getItem().components().get(DataComponents.ITEM_NAME);
		if (itemName != null && !Objects.equals(itemName, standard)) {
			return ChatFormatting.stripFormatting(itemName.getString());
		}
		return null;
	}
}
