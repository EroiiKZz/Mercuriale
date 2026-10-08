package fr.imero.mercuriale.client;

import fr.imero.mercuriale.Mercuriale;
import fr.imero.mercuriale.client.screen.Theme;
import fr.imero.mercuriale.config.MercurialeConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class MercurialeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MercurialeConfig.get();
		MercurialeKeybinds.register();
		TooltipHost.register();
		PageLink.register();
		ContainerValue.register();
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			Theme.sync();
			PriceService.tick(client, MercurialeConfig.get());
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> PriceService.forceRefresh());
		Mercuriale.LOGGER.info("[{}] client init", Mercuriale.MOD_ID);
	}
}
