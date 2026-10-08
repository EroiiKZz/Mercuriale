package fr.imero.mercuriale.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.net.URI;

public final class GameBridge {
	public interface TextPainter {
		void text(Font font, Component text, int x, int y, int color, boolean shadow);
	}

	public interface Overlay {
		void draw(TextPainter painter);
	}

	private GameBridge() {
	}

	public static Screen screen() {
		return Minecraft.getInstance().gui.screen();
	}

	public static void openUri(URI uri) {
		Util.getPlatform().openUri(uri);
	}

	public static void afterDraw(Screen screen, Overlay overlay) {
		ScreenEvents.afterExtract(screen).register((s, context, mouseX, mouseY, tickDelta) ->
			overlay.draw(context::text));
	}
}
