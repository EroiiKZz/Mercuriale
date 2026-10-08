package fr.imero.mercuriale.client.integration;

import fr.imero.mercuriale.client.PriceLines;
import fr.imero.mercuriale.client.TooltipHost;
import fr.imero.vitrine.api.VitrineApi;
import fr.imero.vitrine.api.VitrinePlugin;

import java.util.List;

public final class VitrineHook implements VitrinePlugin {
	@Override
	public void register(VitrineApi api) {
		TooltipHost.handOver(api::drawsTooltips);
		api.addTooltipRows((stack, rows) -> {
			List<PriceLines.Line> lines = PriceLines.describe(stack, rows.labelColor(), rows.valueColor());
			if (lines.isEmpty()) {
				return;
			}
			rows.rule();
			for (PriceLines.Line line : lines) {
				rows.pair(line.label(), line.value());
			}
		});
	}
}
