package fr.imero.mercuriale.client.lang;

import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class LanguageOverride {
	private static final String[] NAMESPACES = {"mercuriale", "aeternum"};

	private record Table(Object source, String code, Map<String, String> entries) {
	}

	private static volatile Table table = new Table(null, null, Map.of());

	private LanguageOverride() {
	}

	public static String lookup(Object source, String key) {
		String code = AeternumLanguage.choice().code();
		if (code == null || key == null) {
			return null;
		}
		Table current = table;
		if (current.source() != source || !code.equals(current.code())) {
			current = load(source, code);
			if (current == null) {
				return null;
			}
			table = current;
		}
		return current.entries().get(key);
	}

	private static Table load(Object source, String code) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null) {
			return null;
		}
		ResourceManager resources = minecraft.getResourceManager();
		if (resources == null) {
			return null;
		}
		Map<String, String> entries = new HashMap<>();
		for (String namespace : NAMESPACES) {
			Identifier id = Identifier.fromNamespaceAndPath(namespace, "lang/" + code + ".json");
			for (Resource resource : resources.getResourceStack(id)) {
				try (InputStream in = resource.open()) {
					Language.loadFromJson(in, entries::put);
				} catch (Exception ignored) {
				}
			}
		}
		return new Table(source, code, Map.copyOf(entries));
	}
}
