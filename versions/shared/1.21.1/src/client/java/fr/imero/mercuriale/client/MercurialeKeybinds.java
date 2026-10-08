package fr.imero.mercuriale.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.imero.mercuriale.Mercuriale;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Consumer;

public final class MercurialeKeybinds {
	private static final String CATEGORY = "key.category." + Mercuriale.MOD_ID + ".main";

	public static KeyMapping OPEN_PAGE;

	private MercurialeKeybinds() {
	}

	public static void register() {
		OPEN_PAGE = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key." + Mercuriale.MOD_ID + ".open_page", InputConstants.Type.KEYSYM, InputConstants.KEY_LALT, CATEGORY));
	}

	public static void watchOpenPage(Screen screen, Consumer<Boolean> pressed, Consumer<Boolean> released) {
		ScreenKeyboardEvents.afterKeyPress(screen).register((ignored, key, scancode, modifiers) ->
			pressed.accept(opensPage(key, scancode)));
		ScreenKeyboardEvents.afterKeyRelease(screen).register((ignored, key, scancode, modifiers) ->
			released.accept(opensPage(key, scancode)));
	}

	private static boolean opensPage(int key, int scancode) {
		return OPEN_PAGE != null && OPEN_PAGE.matches(key, scancode);
	}
}
