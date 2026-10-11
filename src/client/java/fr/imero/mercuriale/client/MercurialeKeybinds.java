package fr.imero.mercuriale.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.imero.mercuriale.Mercuriale;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

public final class MercurialeKeybinds {
	private static final KeyMapping.Category CATEGORY =
		KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Mercuriale.MOD_ID, "main"));

	public static KeyMapping OPEN_PAGE;

	private MercurialeKeybinds() {
	}

	public static KeyMapping SHOW_ENCHANTS;

	public static void register() {
		SHOW_ENCHANTS = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key." + Mercuriale.MOD_ID + ".show_enchants", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));
		OPEN_PAGE = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key." + Mercuriale.MOD_ID + ".open_page", InputConstants.Type.KEYSYM, InputConstants.KEY_LALT, CATEGORY));
	}

	public static void watch(Screen screen, KeyMapping key, Consumer<Boolean> pressed, Consumer<Boolean> released) {
		ScreenKeyboardEvents.afterKeyPress(screen).register((ignored, input) -> pressed.accept(matches(key, input)));
		ScreenKeyboardEvents.afterKeyRelease(screen).register((ignored, input) -> released.accept(matches(key, input)));
	}

	private static boolean matches(KeyMapping key, KeyEvent input) {
		return key != null && key.matches(input);
	}
}
