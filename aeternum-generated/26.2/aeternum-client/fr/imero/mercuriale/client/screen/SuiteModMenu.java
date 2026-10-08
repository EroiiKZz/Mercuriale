package fr.imero.mercuriale.client.screen;

import com.terraformersmc.modmenu.ModMenu;
import net.minecraft.client.gui.screens.Screen;

final class SuiteModMenu {
	private SuiteModMenu() {
	}

	static boolean hasConfig(String id) {
		return ModMenu.hasConfigScreen(id);
	}

	static Screen config(String id, Screen parent) {
		Screen screen = ModMenu.getConfigScreen(id, parent);
		return screen != null ? screen : parent;
	}
}
