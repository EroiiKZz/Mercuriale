package fr.imero.mercuriale.client;

import fr.imero.mercuriale.client.screen.Theme;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BooleanSupplier;

public final class TooltipHost {
	private static final long HOVER_TTL_MS = 150L;

	private static volatile BooleanSupplier drawnElsewhere = () -> false;
	private static volatile ItemStack hovered = ItemStack.EMPTY;
	private static volatile long hoveredAt;

	private TooltipHost() {
	}

	public static void handOver(BooleanSupplier host) {
		drawnElsewhere = host;
	}

	public static ItemStack hovered(long now) {
		return now - hoveredAt <= HOVER_TTL_MS ? hovered : ItemStack.EMPTY;
	}

	public static void register() {
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			hovered = stack;
			hoveredAt = System.currentTimeMillis();
			if (!drawnElsewhere.getAsBoolean()) {
				append(stack, lines);
			}
		});
	}

	private static void append(ItemStack stack, List<Component> lines) {
		int muted = Theme.TEXT_SECONDARY;
		int bright = Theme.TEXT_PRIMARY;
		for (PriceLines.Line line : PriceLines.describe(stack, muted, bright)) {
			lines.add(Component.empty()
				.append(Component.empty().append(line.label()).withStyle(style -> style.withColor(Theme.rgb(muted))))
				.append(Component.literal("  "))
				.append(Component.empty().append(line.value()).withStyle(style -> style.withColor(Theme.rgb(bright)))));
		}
	}
}
