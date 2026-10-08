package fr.imero.mercuriale.client.screen;

import fr.imero.mercuriale.client.PriceService;
import fr.imero.mercuriale.config.MercurialeConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.function.BooleanSupplier;

public class ConfigScreen extends SettingsScreen {
	private static final String ENABLED = "mercuriale.config.enabled";
	private static final String RADAR_VALUES = "mercuriale.config.radar_values";
	private static final String CONTAINER_VALUE = "mercuriale.config.container_value";

	private final MercurialeConfig config = MercurialeConfig.get();

	public ConfigScreen(Screen parent) {
		super(parent, Component.translatable("mercuriale.config.title"), Items.EMERALD);
	}

	@Override
	protected void save() {
		config.save();
		PriceService.forceRefresh();
	}

	@Override
	protected void buildRows(int x, int w) {
		BooleanSupplier on = () -> config.enabled;

		section("mercuriale.config.section.prices");
		toggle(x, w, ENABLED, config.enabled, value -> config.enabled = value);
		toggle(x, w, "mercuriale.config.market", config.market, value -> config.market = value)
			.requires(ENABLED, on);
		toggle(x, w, "mercuriale.config.shops", config.shops, value -> config.shops = value)
			.requires(ENABLED, on);
		choice(x, w, "mercuriale.config.amount", () -> config.amount.translationKey(),
			() -> config.amount = config.amount.next()).requires(ENABLED, on);
		action(x, w, "mercuriale.config.reset_prices", () -> reset(config::resetPrices));

		section("mercuriale.config.section.bargains");
		toggle(x, w, "mercuriale.config.bargains", config.bargains, value -> config.bargains = value)
			.requires(ENABLED, on);
		text(x, w, "mercuriale.config.auction_title_pattern", config.auctionTitlePattern,
			value -> config.auctionTitlePattern = value, MercurialeConfig::patternError)
			.requires("mercuriale.config.bargains", () -> config.enabled && config.bargains);
		text(x, w, "mercuriale.config.listing_price_pattern", config.listingPricePattern,
			value -> config.listingPricePattern = value, MercurialeConfig::patternError)
			.requires("mercuriale.config.bargains", () -> config.enabled && config.bargains);
		action(x, w, "mercuriale.config.reset_bargains", () -> reset(config::resetBargains));

		section("mercuriale.config.section.stock");
		toggle(x, w, CONTAINER_VALUE, config.containerValue, value -> config.containerValue = value)
			.requires(ENABLED, on);
		text(x, w, "mercuriale.config.server_menu_pattern", config.serverMenuPattern,
			value -> config.serverMenuPattern = value, MercurialeConfig::patternError)
			.requires(CONTAINER_VALUE, () -> config.enabled && config.containerValue);
		toggle(x, w, RADAR_VALUES, config.radarValues, value -> config.radarValues = value)
			.requires(ENABLED, on)
			.dependsOn(() -> FabricLoader.getInstance().isModLoaded("chestradar"))
			.disabledSays("mercuriale.config.radar_values.requires");
		slider(x, w, "mercuriale.config.shop_undercut", MercurialeConfig.UNDERCUT_MIN, MercurialeConfig.UNDERCUT_MAX,
			() -> config.shopUndercutPercent, value -> config.shopUndercutPercent = (int) value, value -> value + " %")
			.requires(RADAR_VALUES, () -> config.enabled && config.radarValues);
		action(x, w, "mercuriale.config.reset_stock", () -> reset(config::resetChestRadar));

		section("mercuriale.config.section.source");
		text(x, w, "mercuriale.config.server", config.server, value -> config.server = value.strip(), null);
		text(x, w, "mercuriale.config.url", config.url, value -> config.url = value.strip(),
			MercurialeConfig::urlError);
	}

	private void reset(Runnable action) {
		action.run();
		config.save();
		refresh();
	}
}
