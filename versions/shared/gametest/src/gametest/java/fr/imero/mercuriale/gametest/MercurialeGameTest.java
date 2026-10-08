package fr.imero.mercuriale.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import fr.imero.mercuriale.client.GameBridge;
import fr.imero.mercuriale.client.screen.ConfigScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Properties;

public class MercurialeGameTest implements FabricClientGameTest {
	private static final int SETTLE_TICKS = 5;
	private static final int HOVER_TICKS = 4;
	private static final int TABLE_TIMEOUT = 600;
	private static final int SCROLL_ROWS = 40;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (PriceFixture fixture = new PriceFixture()) {
			context.runOnClient(client -> {
				if (!client.getWindow().isFullscreen()) {
					client.getWindow().toggleFullScreen();
					client.options.fullscreen().set(true);
				}
				client.options.guiScale().set(PriceFixture.GUI_SCALE);
				client.resizeDisplay();
				fixture.configure();
			});
			context.waitTicks(10);

			context.setScreen(() -> new ConfigScreen(GameBridge.screen()));
			context.waitForScreen(ConfigScreen.class);
			context.waitTicks(SETTLE_TICKS);
			context.takeScreenshot("mercuriale-reglages");
			double[] middle = context.computeOnClient(client ->
				new double[] {client.getWindow().getWidth() / 2.0, client.getWindow().getHeight() / 2.0});
			context.getInput().setCursorPos(middle[0], middle[1]);
			context.getInput().scroll(-SCROLL_ROWS);
			context.waitTicks(SETTLE_TICKS);
			context.takeScreenshot("mercuriale-reglages-bas");
			context.runOnClient(client -> GameBridge.screen().onClose());
			context.waitForScreen(TitleScreen.class);

			Properties properties = new Properties();
			properties.setProperty("enforce-secure-chat", "false");
			properties.setProperty("level-type", "minecraft:flat");
			properties.setProperty("generate-structures", "false");
			properties.setProperty("view-distance", "4");
			try (TestDedicatedServerContext server = context.worldBuilder().setUseConsistentSettings(false)
					.createServer(properties);
				TestServerConnection connection = server.connect()) {
				connection.getClientWorld().waitForChunksRender();
				context.waitFor(client -> PriceFixture.loaded(), TABLE_TIMEOUT);
				context.runOnClient(client -> {
					fixture.checkFetch(client);
					fixture.checkPrices();
				});

				context.setScreen(() -> PriceFixture.chest(Minecraft.getInstance(),
					PriceFixture.CHEST_TITLE, PriceFixture.chestStacks()));
				context.waitForScreen(ContainerScreen.class);
				context.waitTicks(SETTLE_TICKS);
				context.getInput().setCursorPos(2, 2);
				context.waitTicks(HOVER_TICKS);
				context.takeScreenshot("mercuriale-coffre");
				hover(context, 0);
				context.takeScreenshot("mercuriale-infobulle-diamant");
				hover(context, 1);
				context.takeScreenshot("mercuriale-infobulle-netherite");

				context.getInput().setCursorPos(2, 2);
				context.waitTicks(HOVER_TICKS);
				context.runOnClient(client -> fixture.watchKeys());
				context.getInput().pressKey(InputConstants.KEY_LALT);
				context.waitTicks(2);
				context.getInput().pressKey(InputConstants.KEY_B);
				context.waitTicks(2);
				fixture.checkKeys();

				context.setScreen(() -> PriceFixture.chest(Minecraft.getInstance(),
					PriceFixture.AUCTION_TITLE, List.of(PriceFixture.listing())));
				context.waitTicks(SETTLE_TICKS);
				hover(context, 0);
				context.takeScreenshot("mercuriale-annonce-hdv");
				context.runOnClient(client -> fixture.checkBargain());
				context.setScreen(() -> null);
				context.waitTicks(SETTLE_TICKS);
			}

			List<String> failures = fixture.failures();
			if (!failures.isEmpty()) {
				throw new AssertionError("Mercuriale : " + failures);
			}
			System.out.println(PriceFixture.LOG + "tout est passé");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void hover(ClientGameTestContext context, int slot) {
		double[] at = context.computeOnClient(client -> PriceFixture.slotCenter(client, slot));
		context.getInput().setCursorPos(at[0], at[1]);
		context.waitTicks(HOVER_TICKS);
	}
}
