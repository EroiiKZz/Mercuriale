package fr.imero.mercuriale.client;

import fr.imero.mercuriale.AeternumFiles;
import fr.imero.mercuriale.AeternumPaths;
import fr.imero.mercuriale.Mercuriale;
import fr.imero.mercuriale.config.MercurialeConfig;
import fr.imero.mercuriale.price.PriceTable;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class PriceService {
	private static final long REFRESH_MS = 5L * 60L * 1000L;
	private static final long RETRY_MS = 60L * 1000L;
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
	private static final int OK = 200;
	private static final int NOT_MODIFIED = 304;
	private static final String CACHE_FILE = Mercuriale.MOD_ID + "/prices.json";
	private static final String PLAYER_HEADER = "X-Mercuriale-Player";
	private static final String USER_AGENT = "Mercuriale/" + FabricLoader.getInstance().getModContainer(Mercuriale.MOD_ID)
		.map(container -> container.getMetadata().getVersion().getFriendlyString())
		.orElse("dev") + " (Minecraft; " + System.getProperty("os.name", "?") + ")";

	private static final HttpClient HTTP = HttpClient.newBuilder()
		.version(HttpClient.Version.HTTP_1_1)
		.connectTimeout(CONNECT_TIMEOUT)
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build();

	private static final AtomicReference<PriceTable> TABLE = new AtomicReference<>(PriceTable.EMPTY);
	private static final AtomicBoolean FETCHING = new AtomicBoolean();
	private static final AtomicBoolean CACHE_REQUESTED = new AtomicBoolean();
	private static volatile long nextFetchAt;
	private static volatile String fetchedFrom = "";

	private PriceService() {
	}

	public static PriceTable table() {
		return TABLE.get();
	}

	public static boolean active(Minecraft client, MercurialeConfig config) {
		return config.enabled && (config.market || config.shops) && onPricedServer(client, config.server);
	}

	public static String revision(Minecraft client, MercurialeConfig config) {
		return active(client, config) ? TABLE.get().version() : "";
	}

	public static void tick(Minecraft client, MercurialeConfig config) {
		if (!active(client, config)) {
			return;
		}
		if (CACHE_REQUESTED.compareAndSet(false, true)) {
			AeternumFiles.run(PriceService::loadCache);
		}
		String url = config.url;
		if (!url.equals(fetchedFrom)) {
			nextFetchAt = 0L;
		}
		if (System.currentTimeMillis() < nextFetchAt || !FETCHING.compareAndSet(false, true)) {
			return;
		}
		fetch(url, client.getUser().getName());
	}

	public static void forceRefresh() {
		nextFetchAt = 0L;
	}

	private static boolean onPricedServer(Minecraft client, String filter) {
		if (client.hasSingleplayerServer()) {
			return false;
		}
		ServerData server = client.getCurrentServer();
		if (server == null || server.ip == null) {
			return false;
		}
		return filter.isEmpty() || server.ip.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
	}

	private static void fetch(String url, String player) {
		HttpRequest request;
		try {
			HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
				.timeout(REQUEST_TIMEOUT)
				.header("Accept", "application/json")
				.header("User-Agent", USER_AGENT);
			if (player != null && !player.isBlank()) {
				builder.header(PLAYER_HEADER, player);
			}
			String version = TABLE.get().version();
			if (!version.isEmpty() && url.equals(fetchedFrom)) {
				builder.header("If-None-Match", "\"" + version + "\"");
			}
			request = builder.GET().build();
		} catch (IllegalArgumentException e) {
			Mercuriale.LOGGER.warn("[{}] adresse de l'API des prix invalide : {}", Mercuriale.MOD_ID, url);
			finish(url, RETRY_MS);
			return;
		}
		HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
			.whenComplete((response, error) -> {
				if (error != null) {
					Mercuriale.LOGGER.debug("[{}] prix injoignables", Mercuriale.MOD_ID, error);
					finish(url, RETRY_MS);
					return;
				}
				if (response.statusCode() == NOT_MODIFIED) {
					finish(url, REFRESH_MS);
					return;
				}
				if (response.statusCode() != OK) {
					Mercuriale.LOGGER.debug("[{}] prix : réponse {}", Mercuriale.MOD_ID, response.statusCode());
					finish(url, RETRY_MS);
					return;
				}
				try {
					PriceTable table = PriceTable.parse(response.body());
					TABLE.set(table);
					String body = response.body();
					AeternumFiles.run(() -> saveCache(body));
					finish(url, REFRESH_MS);
				} catch (RuntimeException e) {
					Mercuriale.LOGGER.warn("[{}] table de prix illisible", Mercuriale.MOD_ID, e);
					finish(url, RETRY_MS);
				}
			});
	}

	private static void finish(String url, long delay) {
		fetchedFrom = url;
		nextFetchAt = System.currentTimeMillis() + delay;
		FETCHING.set(false);
	}

	private static Path cacheFile() {
		return AeternumPaths.data(CACHE_FILE);
	}

	private static void loadCache() {
		Path file = cacheFile();
		if (!Files.isRegularFile(file)) {
			return;
		}
		try {
			PriceTable cached = PriceTable.parse(Files.readString(file, StandardCharsets.UTF_8));
			TABLE.compareAndSet(PriceTable.EMPTY, cached);
		} catch (Exception e) {
			Mercuriale.LOGGER.warn("[{}] cache des prix illisible, ignoré", Mercuriale.MOD_ID, e);
		}
	}

	private static void saveCache(String body) {
		Path file = cacheFile();
		try {
			Files.createDirectories(file.getParent());
			Path temp = file.resolveSibling(file.getFileName() + ".tmp");
			Files.writeString(temp, body, StandardCharsets.UTF_8);
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (Exception e) {
			Mercuriale.LOGGER.warn("[{}] écriture du cache des prix impossible", Mercuriale.MOD_ID, e);
		}
	}
}
