package fr.imero.mercuriale.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.net.URI;

public final class GameBridge {
	public interface TextPainter {
		void text(Font font, Component text, int x, int y, int color, boolean shadow);

		void pill(int x0, int y0, int x1, int y1, int color);
	}

	public interface Overlay {
		void draw(TextPainter painter);
	}

	private GameBridge() {
	}

	public static Screen screen() {
		return Minecraft.getInstance().screen;
	}

	public static void setScreen(Screen screen) {
		Minecraft.getInstance().setScreen(screen);
	}

	public static void openUri(URI uri) {
		Util.getPlatform().openUri(uri);
	}

	public static void afterDraw(Screen screen, Overlay overlay) {
		ScreenEvents.afterRender(screen).register((s, context, mouseX, mouseY, tickDelta) ->
			overlay.draw(new TextPainter() {
				@Override
				public void text(Font font, Component text, int x, int y, int color, boolean shadow) {
					context.drawString(font, text, x, y, color, shadow);
				}

				@Override
				public void pill(int x0, int y0, int x1, int y1, int color) {
					context.fill(x0, y0, x1, y1, color);
				}
			}));
	}
}
