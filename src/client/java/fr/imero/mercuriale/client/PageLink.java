package fr.imero.mercuriale.client;

import fr.imero.mercuriale.Mercuriale;
import fr.imero.mercuriale.config.MercurialeConfig;
import fr.imero.mercuriale.price.PriceTable;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

public final class PageLink {
	private static final long MAX_TAP_MS = 1000L;
	private static final Set<Screen> EQUIPPED = Collections.newSetFromMap(new WeakHashMap<>());

	private static long pressedAt = Long.MIN_VALUE;

	private PageLink() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!EQUIPPED.add(screen)) {
				return;
			}
			MercurialeKeybinds.watchOpenPage(screen, PageLink::pressed, openPage -> released(client, openPage));
		});
	}

	private static void pressed(boolean openPage) {
		pressedAt = openPage ? System.currentTimeMillis() : Long.MIN_VALUE;
	}

	private static void released(Minecraft client, boolean openPage) {
		if (!openPage || pressedAt == Long.MIN_VALUE) {
			return;
		}
		long now = System.currentTimeMillis();
		boolean tap = now - pressedAt <= MAX_TAP_MS;
		pressedAt = Long.MIN_VALUE;
		if (!tap || !client.isWindowActive()) {
			return;
		}
		URI page = page(client, TooltipHost.hovered(now));
		if (page != null) {
			GameBridge.openUri(page);
		}
	}

	private static URI page(Minecraft client, ItemStack stack) {
		MercurialeConfig config = MercurialeConfig.get();
		if (stack.isEmpty() || !PriceService.active(client, config)) {
			return null;
		}
		PriceTable.Quote quote = PriceLookup.quote(stack);
		if (quote == null) {
			return null;
		}
		try {
			URI site = URI.create(config.url);
			if (quote.market() != null && !quote.market().slug().isEmpty()) {
				return site.resolve("/item/" + quote.market().slug());
			}
			if (quote.shop() != null) {
				return site.resolve("/shops/" + quote.shop().material().toLowerCase(Locale.ROOT));
			}
		} catch (IllegalArgumentException e) {
			Mercuriale.LOGGER.warn("[{}] adresse du site invalide : {}", Mercuriale.MOD_ID, config.url);
		}
		return null;
	}
}
