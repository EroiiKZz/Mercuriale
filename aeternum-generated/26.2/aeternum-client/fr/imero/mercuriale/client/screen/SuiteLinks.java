package fr.imero.mercuriale.client.screen;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SuiteLinks {
	public record Member(String id, String name) {
	}

	private static final String PARENT = "aeternum";
	private static final String MOD_MENU = "modmenu";
	private static final String MOD_MENU_KEY = "modmenu";
	private static final String PARENT_KEY = "parent";
	private static final String ID_KEY = "id";

	private static List<Member> members;

	private SuiteLinks() {
	}

	public static List<Member> members() {
		if (members == null) {
			members = discover();
		}
		return members;
	}

	public static Screen open(String id, Screen parent) {
		return SuiteModMenu.config(id, parent);
	}

	private static List<Member> discover() {
		if (!FabricLoader.getInstance().isModLoaded(MOD_MENU)) {
			return List.of();
		}
		List<Member> found = new ArrayList<>();
		for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
			ModMetadata metadata = mod.getMetadata();
			if (PARENT.equals(parentOf(metadata)) && SuiteModMenu.hasConfig(metadata.getId())) {
				found.add(new Member(metadata.getId(), metadata.getName()));
			}
		}
		found.sort(Comparator.comparing(member -> member.name().toLowerCase(Locale.ROOT)));
		return List.copyOf(found);
	}

	private static String parentOf(ModMetadata metadata) {
		CustomValue modMenu = metadata.getCustomValue(MOD_MENU_KEY);
		if (modMenu == null || modMenu.getType() != CustomValue.CvType.OBJECT) {
			return null;
		}
		CustomValue parent = modMenu.getAsObject().get(PARENT_KEY);
		if (parent == null) {
			return null;
		}
		if (parent.getType() == CustomValue.CvType.STRING) {
			return parent.getAsString();
		}
		if (parent.getType() == CustomValue.CvType.OBJECT) {
			CustomValue id = parent.getAsObject().get(ID_KEY);
			return id != null && id.getType() == CustomValue.CvType.STRING ? id.getAsString() : null;
		}
		return null;
	}
}
