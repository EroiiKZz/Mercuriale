package fr.imero.mercuriale.price;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

public final class PriceTable {
	public static final PriceTable EMPTY = new PriceTable("", List.of(), List.of());

	private static final char SEPARATOR = '\n';

	private final String version;
	private final int marketSize;
	private final Map<String, MarketPrice> exact = new HashMap<>();
	private final Map<String, MarketPrice> loose = new HashMap<>();
	private final Set<String> ambiguous = new HashSet<>();
	private final Map<String, MarketPrice> vanilla = new HashMap<>();
	private final Map<String, ShopPrice> shops = new HashMap<>();
	// Par nom seul, pour les objets dont on ignore le matériau (la rune protectrice)
	private final Map<String, MarketPrice> anyMaterial = new HashMap<>();

	public record Quote(MarketPrice market, ShopPrice shop) {
		public boolean isEmpty() {
			return market == null && (shop == null || shop.isEmpty());
		}

		public Quote only(boolean keepMarket, boolean keepShop) {
			return new Quote(keepMarket ? market : null, keepShop ? shop : null);
		}

		public OptionalDouble reference() {
			if (market != null && market.price() > 0) {
				return OptionalDouble.of(market.price());
			}
			if (shop != null && shop.sellTo() != null) {
				return OptionalDouble.of(shop.sellTo());
			}
			if (shop != null && shop.buyFrom() != null) {
				return OptionalDouble.of(shop.buyFrom());
			}
			return OptionalDouble.empty();
		}
	}

	public PriceTable(String version, List<MarketPrice> market, List<ShopPrice> shopList) {
		this.version = version;
		this.marketSize = market.size();
		for (MarketPrice price : market) {
			String material = PriceKeys.material(price.material());
			exact.putIfAbsent(material + SEPARATOR + PriceKeys.exact(price.name()), price);
			String looseName = PriceKeys.loose(price.name());
			if (!looseName.isEmpty()) {
				String key = material + SEPARATOR + looseName;
				if (loose.putIfAbsent(key, price) != null) {
					ambiguous.add(key);
				}
				anyMaterial.putIfAbsent(looseName, price);
			}
			if (price.vanilla()) {
				vanilla.putIfAbsent(material, price);
			}
		}
		for (ShopPrice shop : shopList) {
			shops.putIfAbsent(PriceKeys.material(shop.material()), shop);
		}
	}

	public static PriceTable parse(String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		List<MarketPrice> market = new ArrayList<>();
		for (JsonElement element : array(root, "market")) {
			JsonObject item = element.getAsJsonObject();
			market.add(new MarketPrice(
				string(item, "material"),
				string(item, "name"),
				item.has("vanilla") && item.get("vanilla").getAsBoolean(),
				string(item, "slug"),
				number(item, "price", 0.0),
				MarketPrice.Tier.parse(string(item, "tier")),
				(int) number(item, "samples", 0.0),
				optionalInt(item, "windowDays"),
				(int) number(item, "onSale", 0.0),
				optionalDouble(item, "cheapest")));
		}
		List<ShopPrice> shopList = new ArrayList<>();
		for (JsonElement element : array(root, "shops")) {
			JsonObject shop = element.getAsJsonObject();
			shopList.add(new ShopPrice(
				string(shop, "material"),
				optionalDouble(shop, "buyFrom"),
				optionalDouble(shop, "sellTo"),
				(int) number(shop, "sellers", 0.0),
				(int) number(shop, "buyers", 0.0)));
		}
		return new PriceTable(string(root, "version"), market, shopList);
	}

	public String version() {
		return version;
	}

	public boolean isEmpty() {
		return marketSize == 0 && shops.isEmpty();
	}

	public Quote quote(String material, String customName) {
		String key = PriceKeys.material(material);
		if (customName == null || customName.isBlank()) {
			return new Quote(vanilla.get(key), shops.get(key));
		}
		MarketPrice market = exact.get(key + SEPARATOR + PriceKeys.exact(customName));
		if (market == null) {
			String looseKey = key + SEPARATOR + PriceKeys.loose(customName);
			if (!ambiguous.contains(looseKey)) {
				market = loose.get(looseKey);
			}
		}
		return new Quote(market, null);
	}

	public MarketPrice anyMaterial(String name) {
		return name == null ? null : anyMaterial.get(PriceKeys.loose(name));
	}

	private static JsonArray array(JsonObject object, String name) {
		JsonElement element = object.get(name);
		return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
	}

	private static String string(JsonObject object, String name) {
		JsonElement element = object.get(name);
		return element == null || element.isJsonNull() ? "" : element.getAsString();
	}

	private static double number(JsonObject object, String name, double fallback) {
		JsonElement element = object.get(name);
		return element == null || element.isJsonNull() ? fallback : element.getAsDouble();
	}

	private static Double optionalDouble(JsonObject object, String name) {
		JsonElement element = object.get(name);
		return element == null || element.isJsonNull() ? null : element.getAsDouble();
	}

	private static Integer optionalInt(JsonObject object, String name) {
		JsonElement element = object.get(name);
		return element == null || element.isJsonNull() ? null : element.getAsInt();
	}
}
