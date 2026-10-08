package fr.imero.mercuriale.client.lang;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.imero.mercuriale.AeternumFiles;
import fr.imero.mercuriale.AeternumPaths;
import fr.imero.mercuriale.Mercuriale;

import java.nio.file.Files;
import java.nio.file.Path;

public final class AeternumLanguage {
	public static final String FILE_NAME = "language.json";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public enum Choice {
		AUTO("auto", null),
		FRENCH("fr_fr", "fr_fr"),
		ENGLISH("en_us", "en_us");

		private final String key;
		private final String code;

		Choice(String key, String code) {
			this.key = key;
			this.code = code;
		}

		public String key() {
			return key;
		}

		public String code() {
			return code;
		}

		public String translationKey() {
			return "aeternum.language." + key;
		}

		public Choice next() {
			Choice[] all = values();
			return all[(ordinal() + 1) % all.length];
		}

		public static Choice of(String key) {
			if (key != null) {
				for (Choice choice : values()) {
					if (choice.key.equalsIgnoreCase(key)) {
						return choice;
					}
				}
			}
			return AUTO;
		}
	}

	private static final class Data {
		String language;
	}

	private static volatile Choice choice = Choice.AUTO;
	private static volatile Path path;
	private static volatile long stamp;
	private static volatile boolean loaded;

	private AeternumLanguage() {
	}

	public static Path file() {
		return AeternumPaths.config(FILE_NAME);
	}

	public static Choice choice() {
		ensureLoaded();
		return choice;
	}

	public static void cycle() {
		ensureLoaded();
		choice = choice.next();
		save();
	}

	public static void refresh() {
		ensureLoaded();
	}

	private static void ensureLoaded() {
		if (loaded) {
			return;
		}
		synchronized (AeternumLanguage.class) {
			if (loaded) {
				return;
			}
			Path target = file();
			path = target;
			stamp = AeternumFiles.modified(target);
			choice = read(target);
			loaded = true;
		}
		AeternumFiles.watch(AeternumLanguage::poll);
	}

	private static void poll() {
		Path target = path;
		long modified = AeternumFiles.modified(target);
		if (modified == stamp) {
			return;
		}
		stamp = modified;
		choice = read(target);
	}

	private static Choice read(Path target) {
		if (!Files.isRegularFile(target)) {
			return Choice.FRENCH;
		}
		try {
			Data data = GSON.fromJson(Files.readString(target), Data.class);
			return data == null ? Choice.AUTO : Choice.of(data.language);
		} catch (Exception e) {
			Mercuriale.LOGGER.warn("[mercuriale] {} illisible, langue de Minecraft conservée", FILE_NAME, e);
			return Choice.AUTO;
		}
	}

	private static void save() {
		Path target = path;
		Data data = new Data();
		data.language = choice.key();
		String json = GSON.toJson(data);
		AeternumFiles.run(() -> {
			try {
				Files.createDirectories(target.getParent());
				Files.writeString(target, json);
				stamp = AeternumFiles.modified(target);
			} catch (Exception e) {
				Mercuriale.LOGGER.warn("[mercuriale] écriture de {} impossible", FILE_NAME, e);
			}
		});
	}
}
