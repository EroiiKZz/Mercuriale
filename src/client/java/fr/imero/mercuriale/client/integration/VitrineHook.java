package fr.imero.mercuriale.client.integration;

import fr.imero.mercuriale.client.PriceLines;
import fr.imero.mercuriale.client.TooltipHost;
import fr.imero.vitrine.api.TooltipRows;
import fr.imero.vitrine.api.VitrineApi;
import fr.imero.vitrine.api.VitrinePlugin;

import net.minecraft.network.chat.Component;

import java.util.List;

public final class VitrineHook implements VitrinePlugin {
	@Override
	public void register(VitrineApi api) {
		TooltipHost.handOver(api::drawsTooltips);
		api.addTooltipRows((stack, rows) -> {
			if (supportsBlocks(rows)) {
				PricePanel panel = PricePanel.of(stack);
				if (panel != null) {
					rows.block(panel);
				}
				return;
			}
			List<PriceLines.Line> lines = PriceLines.describe(stack, rows.labelColor(), rows.valueColor());
			if (lines.isEmpty()) {
				return;
			}
			rows.rule();
			rows.pair(Component.translatable("mercuriale.price.header")
					.withStyle(style -> style.withColor(fr.imero.mercuriale.client.screen.Theme.rgb(
						fr.imero.mercuriale.client.screen.Theme.ACCENT))),
				Component.translatable("mercuriale.price.source").withStyle(style -> style.withColor(rows.mutedColor())));
			for (PriceLines.Line line : lines) {
				rows.pair(line.label(), line.value());
			}
		});
	}

	private static boolean supportsBlocks(TooltipRows rows) {
		try {
			return rows.supportsBlocks();
		} catch (LinkageError e) {
			return false;
		}
	}
}
