package fr.imero.mercuriale.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.imero.mercuriale.Mercuriale;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public final class MercurialeKeybinds {
	private static final KeyMapping.Category CATEGORY =
		KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath(Mercuriale.MOD_ID, "main"));

	public static KeyMapping OPEN_PAGE;

	private MercurialeKeybinds() {
	}

	public static void register() {
		OPEN_PAGE = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key." + Mercuriale.MOD_ID + ".open_page", InputConstants.Type.KEYSYM, InputConstants.KEY_LALT, CATEGORY));
	}

	public static void watchOpenPage(Screen screen, Consumer<Boolean> pressed, Consumer<Boolean> released) {
		ScreenKeyboardEvents.afterKeyPress(screen).register((ignored, input) -> pressed.accept(opensPage(input)));
		ScreenKeyboardEvents.afterKeyRelease(screen).register((ignored, input) -> released.accept(opensPage(input)));
	}

	private static boolean opensPage(KeyEvent input) {
		return OPEN_PAGE != null && OPEN_PAGE.matches(input);
	}
}
