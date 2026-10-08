package fr.imero.mercuriale.client;

import fr.imero.mercuriale.client.screen.Theme;
import fr.imero.mercuriale.config.MercurialeConfig;
import fr.imero.mercuriale.price.PriceFormat;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

public final class ContainerValue {
	private static final int GAP = 3;
	private static final long REFRESH_MS = 1000L;
	private static final Set<Screen> EQUIPPED = Collections.newSetFromMap(new WeakHashMap<>());

	private ContainerValue() {
	}

	private static final class Cache {
		int stateId = -1;
		String revision = "";
		long at;
		Component text;
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof AbstractContainerScreen<?> handled) || screen instanceof InventoryScreen
				|| screen instanceof CreativeModeInventoryScreen || !EQUIPPED.add(screen)) {
				return;
			}
			Cache cache = new Cache();
			GameBridge.afterDraw(screen, painter -> render(client, handled, painter, cache));
		});
	}

	private static void render(Minecraft client, AbstractContainerScreen<?> screen, GameBridge.TextPainter painter,
			Cache cache) {
		MercurialeConfig config = MercurialeConfig.get();
		if (!config.containerValue || serverMenu(screen, config)) {
			return;
		}
		String revision = PriceService.revision(client, config);
		if (revision.isEmpty()) {
			return;
		}
		long now = System.currentTimeMillis();
		int state = screen.getMenu().getStateId();
		if (state != cache.stateId || !revision.equals(cache.revision) || now - cache.at > REFRESH_MS) {
			cache.stateId = state;
			cache.revision = revision;
			cache.at = now;
			cache.text = describe(screen);
		}
		int y = screen.topPos - client.font.lineHeight - GAP;
		if (cache.text == null || y < 0) {
			return;
		}
		painter.text(client.font, cache.text, screen.leftPos, y, Theme.TEXT_SECONDARY, Theme.panelShadow(true));
	}

	private static Component describe(AbstractContainerScreen<?> screen) {
		double total = 0.0;
		int priced = 0;
		int unpriced = 0;
		for (Slot slot : screen.getMenu().slots) {
			if (slot.container instanceof Inventory) {
				continue;
			}
			ItemStack stack = slot.getItem();
			if (stack.isEmpty()) {
				continue;
			}
			OptionalDouble value = StockValue.total(stack, stack.getCount());
			if (value.isPresent()) {
				total += value.getAsDouble();
				priced++;
			} else {
				unpriced++;
			}
		}
		if (priced == 0) {
			return null;
		}
		Component amount = Component.literal(PriceFormat.money(total))
			.withStyle(style -> style.withColor(Theme.rgb(Theme.ACCENT)));
		MutableComponent text = Component.translatable("mercuriale.container.value", amount);
		if (unpriced > 0) {
			text.append(Component.translatable("mercuriale.container.unpriced", unpriced));
		}
		return text;
	}

	private static boolean serverMenu(AbstractContainerScreen<?> screen, MercurialeConfig config) {
		Pattern pattern = config.serverMenu();
		if (pattern == null) {
			return false;
		}
		String title = ChatFormatting.stripFormatting(screen.getTitle().getString());
		return title != null && pattern.matcher(title).find();
	}
}
