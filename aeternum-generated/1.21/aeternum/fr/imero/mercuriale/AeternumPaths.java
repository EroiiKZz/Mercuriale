package fr.imero.mercuriale;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AeternumPaths {
	public static final String FOLDER = "aeternum";

	private static final Map<Path, Path> SETTLED = new ConcurrentHashMap<>();

	private AeternumPaths() {
	}

	public static Path config(String name) {
		return config(name, name);
	}

	public static Path config(String legacyName, String name) {
		Path root = FabricLoader.getInstance().getConfigDir();
		return settle(root.resolve(legacyName), root.resolve(FOLDER).resolve(name));
	}

	public static Path data(String name) {
		Path legacy = FabricLoader.getInstance().getConfigDir().resolve(name);
		return settle(legacy, FabricLoader.getInstance().getGameDir().resolve(FOLDER).resolve(name));
	}

	private static Path settle(Path legacy, Path target) {
		return SETTLED.computeIfAbsent(target, settled -> relocate(legacy, settled));
	}

	public static Path relocate(Path legacy, Path target) {
		if (Files.exists(target) || !Files.exists(legacy)) {
			return target;
		}
		try {
			Files.createDirectories(target.getParent());
			Files.move(legacy, target);
			Mercuriale.LOGGER.info("[mercuriale] {} déplacé vers {}", legacy, target);
			return target;
		} catch (IOException e) {
			Mercuriale.LOGGER.warn("[mercuriale] déplacement de {} vers {} impossible, ancien emplacement conservé",
				legacy, target, e);
			return legacy;
		}
	}
}
