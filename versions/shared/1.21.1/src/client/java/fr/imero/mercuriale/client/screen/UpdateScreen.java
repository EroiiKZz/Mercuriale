package fr.imero.mercuriale.client.screen;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class UpdateScreen {
	private UpdateScreen() {
	}

	public static Screen ask(String version, String notes, BooleanConsumer answer) {
		return new ConfirmScreen(answer,
			Component.translatable("mercuriale.update.title", version),
			Component.translatable("mercuriale.update.message", notes),
			Component.translatable("mercuriale.update.yes"),
			Component.translatable("mercuriale.update.later"));
	}
}
