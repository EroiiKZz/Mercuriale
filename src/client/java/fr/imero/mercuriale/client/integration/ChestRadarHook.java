package fr.imero.mercuriale.client.integration;

import fr.imero.chestradar.api.AnnotationSegment;
import fr.imero.chestradar.api.ChestRadarApi;
import fr.imero.chestradar.api.ChestRadarPlugin;
import fr.imero.chestradar.api.ItemAnnotation;
import fr.imero.mercuriale.client.PriceService;
import fr.imero.mercuriale.client.StockValue;
import fr.imero.mercuriale.config.MercurialeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ChestRadarHook implements ChestRadarPlugin {
	private static final String OFF = "off";

	@Override
	public void register(ChestRadarApi api) {
		api.addItemAnnotation(new ItemAnnotation() {
			@Override
			public String revision() {
				MercurialeConfig config = MercurialeConfig.get();
				if (!config.radarValues) {
					return OFF;
				}
				return PriceService.revision(Minecraft.getInstance(), config) + "#" + config.shopUndercutPercent
					+ "#" + config.market + "#" + config.shops;
			}

			@Override
			public List<AnnotationSegment> annotate(ItemStack stack, long count) {
				if (!MercurialeConfig.get().radarValues) {
					return List.of();
				}
				return StockValue.segments(stack, count).stream()
					.map(segment -> new AnnotationSegment(segment.label(), segment.amount()))
					.toList();
			}
		});
	}
}
