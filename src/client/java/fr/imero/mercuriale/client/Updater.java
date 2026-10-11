package fr.imero.mercuriale.client;

import fr.imero.mercuriale.AeternumPaths;
import fr.imero.mercuriale.Mercuriale;
import fr.imero.mercuriale.config.MercurialeConfig;
import fr.imero.mercuriale.update.Swap;
import fr.imero.mercuriale.update.UpdateJar;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;

public final class Updater {
	private static final String FOLDER = Mercuriale.MOD_ID + "/update";
	private static final String ETAG_FILE = "etag";
	private static final String DOWNLOAD_FILE = "download.part";
	private static final String AETERNUM_PACK_MOD = "vernis";
	private static final String SECURE_SCHEME = "https";
	private static final Duration TIMEOUT = Duration.ofSeconds(60);
	private static final int OK = 200;

	private record Update(Path jar, String version, String notes) {
	}

	private static volatile Update ready;
	private static boolean asked;

	private Updater() {
	}

	public static void register() {
		Thread.ofPlatform().daemon().name("Mercuriale mise a jour").start(Updater::check);
	}

	public static void tick(Minecraft client) {
		Update update = ready;
		if (update == null || asked || !(GameBridge.screen() instanceof TitleScreen title)) {
			return;
		}
		asked = true;
		GameBridge.setScreen(new ConfirmScreen(
			yes -> GameBridge.setScreen(yes ? install(client, update, title) : title),
			Component.translatable("mercuriale.update.title", update.version()),
			Component.translatable("mercuriale.update.message", update.notes()),
			Component.translatable("mercuriale.update.yes"),
			Component.translatable("mercuriale.update.later")));
	}

	private static Screen install(Minecraft client, Update update, Screen back) {
		try {
			Path current = currentJar();
			Path target = current.resolveSibling(update.jar().getFileName());
			Path java = Path.of(System.getProperty("java.home"), "bin", "java");
			new ProcessBuilder(java.toString(), "-cp", update.jar().toString(), Swap.class.getName(),
				Long.toString(ProcessHandle.current().pid()), current.toString(), update.jar().toString(), target.toString())
				.redirectOutput(ProcessBuilder.Redirect.DISCARD)
				.redirectError(ProcessBuilder.Redirect.DISCARD)
				.start();
		} catch (IOException | RuntimeException e) {
			Mercuriale.LOGGER.warn("[{}] mise a jour impossible", Mercuriale.MOD_ID, e);
			return new AlertScreen(() -> GameBridge.setScreen(back),
				Component.translatable("mercuriale.update.failed.title"),
				Component.translatable("mercuriale.update.failed.message"));
		}
		return new ConfirmScreen(
			quit -> {
				if (quit) {
					client.stop();
				} else {
					GameBridge.setScreen(back);
				}
			},
			Component.translatable("mercuriale.update.ready.title", update.version()),
			Component.translatable("mercuriale.update.ready.message"),
			Component.translatable("mercuriale.update.quit"),
			Component.translatable("mercuriale.update.later"));
	}

	private static void check() {
		try {
			if (currentJar() == null || FabricLoader.getInstance().isModLoaded(AETERNUM_PACK_MOD)) {
				return;
			}
			Path dir = Files.createDirectories(AeternumPaths.data(FOLDER));
			URI base = URI.create(MercurialeConfig.get().url);
			if (SECURE_SCHEME.equals(base.getScheme())) {
				download(dir, base.resolve("/downloads/" + Mercuriale.MOD_ID + "/" + minecraftVersion()));
			}
			ready = staged(dir);
			if (ready != null) {
				Mercuriale.LOGGER.info("[{}] version {} prete a installer", Mercuriale.MOD_ID, ready.version());
			}
		} catch (IOException | RuntimeException e) {
			Mercuriale.LOGGER.info("[{}] verification de mise a jour impossible : {}", Mercuriale.MOD_ID, e.toString());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static void download(Path dir, URI uri) throws IOException, InterruptedException {
		Path etag = dir.resolve(ETAG_FILE);
		HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(TIMEOUT).header("User-Agent", PriceService.USER_AGENT);
		if (Files.isRegularFile(etag)) {
			request.header("If-None-Match", Files.readString(etag).strip());
		}
		Path part = dir.resolve(DOWNLOAD_FILE);
		try {
			HttpResponse<Path> response = PriceService.HTTP.send(request.build(), HttpResponse.BodyHandlers.ofFile(part));
			if (response.statusCode() != OK) {
				return;
			}
			String name = response.headers().firstValue("content-disposition").map(UpdateJar::fileName).orElse(null);
			String version = UpdateJar.version(name);
			if (version != null && newer(version)) {
				UpdateJar.notes(part, version);
				Files.move(part, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
			} else {
				response.headers().firstValue("etag").ifPresent(tag -> write(etag, tag));
			}
		} finally {
			Files.deleteIfExists(part);
		}
	}

	private static void write(Path file, String text) {
		try {
			Files.writeString(file, text);
		} catch (IOException e) {
			Mercuriale.LOGGER.debug("[{}] etag non garde", Mercuriale.MOD_ID, e);
		}
	}

	private static Update staged(Path dir) throws IOException {
		Update best = null;
		try (DirectoryStream<Path> jars = Files.newDirectoryStream(dir, "*.jar")) {
			for (Path jar : jars) {
				String version = UpdateJar.version(jar.getFileName().toString());
				if (version == null || !newer(version) || (best != null && !newer(version, best.version()))) {
					Files.deleteIfExists(jar);
					continue;
				}
				try {
					Update update = new Update(jar, version, UpdateJar.notes(jar, version));
					if (best != null) {
						Files.deleteIfExists(best.jar());
					}
					best = update;
				} catch (IOException | RuntimeException broken) {
					Files.deleteIfExists(jar);
				}
			}
		}
		return best;
	}

	private static boolean newer(String version) {
		return newer(version, own().getMetadata().getVersion().getFriendlyString());
	}

	private static boolean newer(String version, String than) {
		try {
			return Version.parse(version).compareTo(Version.parse(than)) > 0;
		} catch (VersionParsingException e) {
			return false;
		}
	}

	private static Path currentJar() {
		ModOrigin origin = own().getOrigin();
		if (origin.getKind() != ModOrigin.Kind.PATH) {
			return null;
		}
		List<Path> paths = origin.getPaths();
		Path jar = paths.size() == 1 ? paths.get(0) : null;
		return jar != null && Files.isRegularFile(jar) && jar.getFileName().toString().endsWith(".jar") ? jar : null;
	}

	private static ModContainer own() {
		return FabricLoader.getInstance().getModContainer(Mercuriale.MOD_ID).orElseThrow();
	}

	private static String minecraftVersion() {
		return FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString();
	}
}
