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

	public static KeyMapping SHOW_ENCHANTS;

	public static void register() {
		SHOW_ENCHANTS = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key." + Mercuriale.MOD_ID + ".show_enchants", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));
		OPEN_PAGE = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key." + Mercuriale.MOD_ID + ".open_page", InputConstants.Type.KEYSYM, InputConstants.KEY_LALT, CATEGORY));
	}

	public static void watch(Screen screen, KeyMapping mapping, Consumer<Boolean> pressed, Consumer<Boolean> released) {
		ScreenKeyboardEvents.afterKeyPress(screen).register((ignored, key, scancode, modifiers) ->
			pressed.accept(matches(mapping, key, scancode)));
		ScreenKeyboardEvents.afterKeyRelease(screen).register((ignored, key, scancode, modifiers) ->
			released.accept(matches(mapping, key, scancode)));
	}

	private static boolean matches(KeyMapping mapping, int key, int scancode) {
		return mapping != null && mapping.matches(key, scancode);
	}
}
