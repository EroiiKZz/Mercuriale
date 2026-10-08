package fr.imero.mercuriale.gametest;

import com.sun.net.httpserver.HttpServer;
import fr.imero.mercuriale.client.GameBridge;
import fr.imero.mercuriale.client.MercurialeKeybinds;
import fr.imero.mercuriale.client.PriceLines;
import fr.imero.mercuriale.client.PriceLookup;
import fr.imero.mercuriale.client.PriceService;
import fr.imero.mercuriale.client.StockValue;
import fr.imero.mercuriale.config.MercurialeConfig;
import fr.imero.mercuriale.config.PriceAmount;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class PriceFixture implements AutoCloseable {
	public static final String LOG = "[mercuriale-gametest] ";
	public static final String CHEST_TITLE = "Coffre de test";
	public static final String AUCTION_TITLE = "Menu | Hotel des Ventes - 1/3";
	public static final int GUI_SCALE = 3;
	public static final int MENU_ID = 100;
	public static final int CHEST_SLOTS = 27;

	private static final String TABLE_ENV = "MERCURIALE_PRICES";
	private static final String DEFAULT_TABLE = "D:/Jeux/Modrinth/profiles/Aeternum/aeternum/mercuriale/prices.json";
	private static final String PATH = "/api/v1/prices";
	private static final String ETAG = "\"gametest\"";
	private static final String PLAYER_HEADER = "X-Mercuriale-Player";
	private static final String LISTING = "• Prix 2 000$";
	private static final int LISTING_COUNT = 16;
	private static final int MUTED = 0xFFAAAAAA;
	private static final int BRIGHT = 0xFFFFFFFF;
	private static final int OK = 200;
	private static final int NOT_MODIFIED = 304;

	private final HttpServer server;
	private final AtomicInteger served = new AtomicInteger();
	private final AtomicInteger unchanged = new AtomicInteger();
	private final List<String> failures = new CopyOnWriteArrayList<>();
	private volatile String player = "";

	public final List<Boolean> pressed = new CopyOnWriteArrayList<>();
	public final List<Boolean> released = new CopyOnWriteArrayList<>();

	public PriceFixture() throws IOException {
		String source = System.getenv(TABLE_ENV);
		byte[] body = Files.readAllBytes(Path.of(source == null || source.isBlank() ? DEFAULT_TABLE : source));
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext(PATH, exchange -> {
			String header = exchange.getRequestHeaders().getFirst(PLAYER_HEADER);
			player = header == null ? "" : header;
			if (ETAG.equals(exchange.getRequestHeaders().getFirst("If-None-Match"))) {
				unchanged.incrementAndGet();
				exchange.sendResponseHeaders(NOT_MODIFIED, -1);
				exchange.close();
				return;
			}
			served.incrementAndGet();
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.getResponseHeaders().set("ETag", ETAG);
			exchange.sendResponseHeaders(OK, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.start();
		System.out.println(LOG + "table servie sur " + url() + " (" + body.length + " octets)");
	}

	public String url() {
		return "http://127.0.0.1:" + server.getAddress().getPort() + PATH;
	}

	public void configure() {
		MercurialeConfig config = MercurialeConfig.get();
		config.enabled = true;
		config.market = true;
		config.shops = true;
		config.amount = PriceAmount.UNIT;
		config.bargains = true;
		config.containerValue = true;
		config.resetBargains();
		config.server = "";
		config.url = url();
		config.save();
		PriceService.forceRefresh();
	}

	public static boolean loaded() {
		return !PriceService.table().version().isEmpty();
	}

	public static Screen chest(Minecraft client, String title, List<ItemStack> stacks) {
		SimpleContainer container = new SimpleContainer(CHEST_SLOTS);
		for (int i = 0; i < stacks.size(); i++) {
			container.setItem(i, stacks.get(i));
		}
		return new ContainerScreen(ChestMenu.threeRows(MENU_ID, client.player.getInventory(), container),
			client.player.getInventory(), Component.literal(title));
	}

	public static List<ItemStack> chestStacks() {
		return List.of(new ItemStack(Items.DIAMOND, 64), new ItemStack(Items.NETHERITE_INGOT, 3),
			new ItemStack(Items.IRON_INGOT, 32), new ItemStack(Items.BEDROCK, 5));
	}

	public static ItemStack listing() {
		ItemStack stack = new ItemStack(Items.DIAMOND, LISTING_COUNT);
		stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(LISTING))));
		return stack;
	}

	public static double[] slotCenter(Minecraft client, int index) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) GameBridge.screen();
		Slot slot = screen.getMenu().slots.get(index);
		double scale = client.getWindow().getGuiScale();
		return new double[] {(screen.leftPos + slot.x + 8) * scale, (screen.topPos + slot.y + 8) * scale};
	}

	public void watchKeys() {
		pressed.clear();
		released.clear();
		MercurialeKeybinds.watchOpenPage(GameBridge.screen(), pressed::add, released::add);
	}

	public void checkPrices() {
		ItemStack diamonds = new ItemStack(Items.DIAMOND, 64);
		List<PriceLines.Line> lines = PriceLines.describe(diamonds, MUTED, BRIGHT);
		System.out.println(LOG + "diamant x64 : " + describe(lines));
		check("lignes de prix du diamant", lines.size() >= 2, describe(lines));
		check("valeur d'une pile de diamants", StockValue.total(diamonds, 64).isPresent(), "aucune valeur");
		check("bedrock sans prix", PriceLookup.quote(new ItemStack(Items.BEDROCK)) == null, "un prix trouvé");
		ItemStack renamed = new ItemStack(Items.DIAMOND);
		renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Objet inconnu de test"));
		check("objet renommé inconnu sans prix", PriceLookup.quote(renamed) == null, "un prix trouvé");
		check("touche Alt enregistrée", MercurialeKeybinds.OPEN_PAGE != null, "OPEN_PAGE nul");
	}

	public void checkBargain() {
		List<PriceLines.Line> lines = PriceLines.describe(listing(), MUTED, BRIGHT);
		String label = Component.translatable("mercuriale.bargain.label").getString();
		System.out.println(LOG + "annonce diamant x16 : " + describe(lines));
		check("ligne d'annonce HDV", !lines.isEmpty() && lines.get(0).label().getString().equals(label), describe(lines));
	}

	public void checkKeys() {
		check("Alt lu par MercurialeKeybinds", pressed.equals(List.of(true, false)) && released.equals(List.of(true, false)),
			"appuis " + pressed + ", relâchés " + released);
	}

	public void checkFetch(Minecraft client) {
		check("table téléchargée", served.get() > 0, "aucune requête");
		check("pseudo annoncé", player.equals(client.getUser().getName()), "en-tête « " + player + " »");
		System.out.println(LOG + "requêtes : " + served.get() + " complètes, " + unchanged.get() + " en 304, version "
			+ PriceService.table().version());
	}

	public void check(String name, boolean ok, String detail) {
		System.out.println(LOG + (ok ? "OK     " : "ECHEC  ") + name + (ok ? "" : " : " + detail));
		if (!ok) {
			failures.add(name + " : " + detail);
		}
	}

	public List<String> failures() {
		return new ArrayList<>(failures);
	}

	private static String describe(List<PriceLines.Line> lines) {
		return lines.stream().map(line -> line.label().getString() + " = " + line.value().getString()).toList().toString();
	}

	@Override
	public void close() {
		server.stop(0);
	}
}
