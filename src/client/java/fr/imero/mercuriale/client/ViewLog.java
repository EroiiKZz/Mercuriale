package fr.imero.mercuriale.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.imero.mercuriale.Mercuriale;
import fr.imero.mercuriale.config.MercurialeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ViewLog {
	private static final int MAX_ITEMS = 200;
	private static final long SAME_HOVER_MS = 1000L;
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

	private record Seen(String id, String name, boolean priced) {
	}

	private static final Map<Seen, Integer> PENDING = new LinkedHashMap<>();
	private static Seen last;
	private static long lastAt;

	private ViewLog() {
	}

	public static synchronized void note(ItemStack stack, boolean priced) {
		if (stack == null || stack.isEmpty() || !PriceService.active(Minecraft.getInstance(), MercurialeConfig.get())) {
			return;
		}
		Seen seen = new Seen(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath(), PriceLookup.customName(stack), priced);
		long now = System.currentTimeMillis();
		boolean sameHover = seen.equals(last) && now - lastAt < SAME_HOVER_MS;
		last = seen;
		lastAt = now;
		if (sameHover || (PENDING.size() >= MAX_ITEMS && !PENDING.containsKey(seen))) {
			return;
		}
		PENDING.merge(seen, 1, Integer::sum);
	}

	static void flush(String url, String player) {
		JsonArray items = new JsonArray();
		synchronized (ViewLog.class) {
			for (Map.Entry<Seen, Integer> entry : PENDING.entrySet()) {
				JsonObject item = new JsonObject();
				item.addProperty("id", entry.getKey().id());
				item.addProperty("name", entry.getKey().name());
				item.addProperty("priced", entry.getKey().priced());
				item.addProperty("count", entry.getValue());
				items.add(item);
			}
			PENDING.clear();
		}
		if (items.isEmpty() || player == null || player.isBlank()) {
			return;
		}
		JsonObject body = new JsonObject();
		body.add("items", items);
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(REQUEST_TIMEOUT)
				.header("Content-Type", "application/json")
				.header("User-Agent", PriceService.USER_AGENT)
				.header(PriceService.PLAYER_HEADER, player)
				.POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
				.build();
			PriceService.HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding())
				.whenComplete((response, error) -> {
					if (error != null) {
						Mercuriale.LOGGER.debug("[{}] envoi des objets vus impossible", Mercuriale.MOD_ID, error);
					}
				});
		} catch (IllegalArgumentException e) {
			Mercuriale.LOGGER.debug("[{}] adresse invalide pour les objets vus : {}", Mercuriale.MOD_ID, url);
		}
	}
}
