package fr.imero.mercuriale.selftest;

import com.mojang.blaze3d.platform.InputConstants;
import fr.imero.mercuriale.client.screen.ConfigScreen;
import fr.imero.mercuriale.gametest.PriceFixture;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class SelfTest implements ClientModInitializer {
	private static final String ENABLED = "mercuriale.selftest";
	private static final String ADDRESS = "127.0.0.1:25599";
	private static final String RESULT = "mercuriale-selftest.txt";
	private static final int STEP_TIMEOUT = 2400;
	private static final int SETTLE = 5;
	private static final int SCROLL_ROWS = 40;

	private record Step(String name, int delay, Predicate<Minecraft> until, Consumer<Minecraft> action) {
	}

	private final Deque<Step> steps = new ArrayDeque<>();
	private PriceFixture fixture;
	private boolean started;
	private int waited;
	private int blocked;

	@Override
	public void onInitializeClient() {
		if (Boolean.getBoolean(ENABLED)) {
			ClientTickEvents.END_CLIENT_TICK.register(this::tick);
		}
	}

	private void tick(Minecraft client) {
		if (!started) {
			if (!(client.screen instanceof TitleScreen)) {
				return;
			}
			started = true;
			plan();
		}
		Step step = steps.peekFirst();
		if (step == null) {
			return;
		}
		if (waited < step.delay()) {
			waited++;
			return;
		}
		if (step.until() != null && !step.until().test(client)) {
			if (++blocked > STEP_TIMEOUT) {
				fixture.check(step.name(), false, "délai dépassé");
				steps.clear();
				finish(client);
			}
			return;
		}
		steps.removeFirst();
		waited = 0;
		blocked = 0;
		try {
			step.action().accept(client);
		} catch (RuntimeException | AssertionError e) {
			e.printStackTrace();
			fixture.check(step.name(), false, e.toString());
			steps.clear();
			finish(client);
		}
	}

	private void then(String name, int delay, Predicate<Minecraft> until, Consumer<Minecraft> action) {
		steps.addLast(new Step(name, delay, until, action));
	}

	private void plan() {
		then("préparation", 0, null, client -> {
			try {
				fixture = new PriceFixture();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			if (!client.getWindow().isFullscreen()) {
				client.getWindow().toggleFullScreen();
				client.options.fullscreen().set(true);
			}
			client.options.guiScale().set(PriceFixture.GUI_SCALE);
			client.resizeDisplay();
			fixture.configure();
		});
		then("écran de réglages", 10, null, client -> client.setScreen(new ConfigScreen(client.screen)));
		then("capture des réglages", 10, null, client -> shot(client, "mercuriale-reglages"));
		then("défilement des réglages", 0, null, client -> client.screen.mouseScrolled(
			client.getWindow().getGuiScaledWidth() / 2.0, client.getWindow().getGuiScaledHeight() / 2.0, 0, -SCROLL_ROWS));
		then("capture du bas des réglages", SETTLE, null, client -> shot(client, "mercuriale-reglages-bas"));
		then("fermeture des réglages", 0, null, client -> client.screen.onClose());
		then("connexion", SETTLE, client -> client.screen instanceof TitleScreen, client -> ConnectScreen.startConnecting(
			client.screen, client, ServerAddress.parseString(ADDRESS),
			new ServerData("Mercuriale test", ADDRESS, ServerData.Type.OTHER), false, null));
		then("monde et table de prix", 0,
			client -> client.player != null && client.level != null && client.screen == null && PriceFixture.loaded(),
			client -> {
				fixture.checkFetch(client);
				fixture.checkPrices();
			});
		then("coffre de test", 40, null, client ->
			client.setScreen(PriceFixture.chest(client, PriceFixture.CHEST_TITLE, PriceFixture.chestStacks())));
		then("souris hors des objets", SETTLE, null, client -> cursor(client, 2, 2));
		then("capture du coffre", SETTLE, null, client -> shot(client, "mercuriale-coffre"));
		then("survol du diamant", 0, null, client -> hover(client, 0));
		then("capture du diamant", SETTLE, null, client -> shot(client, "mercuriale-infobulle-diamant"));
		then("survol du netherite", 0, null, client -> hover(client, 1));
		then("capture du netherite", SETTLE, null, client -> shot(client, "mercuriale-infobulle-netherite"));
		then("souris hors des objets", 0, null, client -> cursor(client, 2, 2));
		then("écoute des touches", SETTLE, null, client -> fixture.watchKeys());
		then("Alt", 0, null, client -> key(client, InputConstants.KEY_LALT));
		then("B", 2, null, client -> key(client, InputConstants.KEY_B));
		then("touches lues", 2, null, client -> fixture.checkKeys());
		then("annonce de l'HDV", 0, null, client ->
			client.setScreen(PriceFixture.chest(client, PriceFixture.AUCTION_TITLE, List.of(PriceFixture.listing()))));
		then("survol de l'annonce", SETTLE, null, client -> hover(client, 0));
		then("capture de l'annonce", SETTLE, null, client -> {
			shot(client, "mercuriale-annonce-hdv");
			fixture.checkBargain();
		});
		then("fermeture", 0, null, client -> client.setScreen(null));
		then("arrêt du serveur", SETTLE, null, client -> client.player.connection.sendCommand("stop"));
		then("fin", 40, null, this::finish);
	}

	private void finish(Minecraft client) {
		List<String> failures = fixture == null ? List.of("préparation impossible") : fixture.failures();
		String verdict = failures.isEmpty() ? "OK" : "ECHEC " + failures;
		System.out.println(PriceFixture.LOG + (failures.isEmpty() ? "tout est passé" : verdict));
		try {
			Files.writeString(client.gameDirectory.toPath().resolve(RESULT), verdict, StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (fixture != null) {
			fixture.close();
		}
		client.stop();
	}

	private static void shot(Minecraft client, String name) {
		Screenshot.grab(client.gameDirectory, name + ".png", client.getMainRenderTarget(), message -> {
		});
	}

	private static void hover(Minecraft client, int slot) {
		double[] at = PriceFixture.slotCenter(client, slot);
		cursor(client, at[0], at[1]);
	}

	private static void cursor(Minecraft client, double x, double y) {
		try {
			Field xpos = MouseHandler.class.getDeclaredField("xpos");
			Field ypos = MouseHandler.class.getDeclaredField("ypos");
			xpos.setAccessible(true);
			ypos.setAccessible(true);
			xpos.setDouble(client.mouseHandler, x);
			ypos.setDouble(client.mouseHandler, y);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void key(Minecraft client, int key) {
		long window = client.getWindow().getWindow();
		int scancode = GLFW.glfwGetKeyScancode(key);
		client.keyboardHandler.keyPress(window, key, scancode, GLFW.GLFW_PRESS, 0);
		client.keyboardHandler.keyPress(window, key, scancode, GLFW.GLFW_RELEASE, 0);
	}
}
