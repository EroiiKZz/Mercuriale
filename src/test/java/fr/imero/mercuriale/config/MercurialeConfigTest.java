package fr.imero.mercuriale.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MercurialeConfigTest {
	@Test
	void takesThePriceSettingsOverFromVitrine(@TempDir Path dir) throws Exception {
		Path vitrine = dir.resolve("vitrine.json");
		Files.writeString(vitrine, """
			{"enabled": true, "showPrices": true, "pricesMarket": false, "pricesShops": true,
			 "priceAmount": "BOTH", "pricesUrl": "http://127.0.0.1:8080/api/v1/prices",
			 "pricesServer": "play.vikingcraft.fr"}
			""");
		MercurialeConfig config = MercurialeConfig.fromVitrine(vitrine);
		assertTrue(config.enabled);
		assertFalse(config.market);
		assertEquals(PriceAmount.BOTH, config.amount);
		assertEquals("http://127.0.0.1:8080/api/v1/prices", config.url);
		assertEquals("play.vikingcraft.fr", config.server);
	}

	@Test
	void missingVitrineFileGivesDefaults(@TempDir Path dir) {
		MercurialeConfig config = MercurialeConfig.fromVitrine(dir.resolve("absent.json"));
		assertEquals(MercurialeConfig.DEFAULT_URL, config.url);
		assertEquals(PriceAmount.UNIT, config.amount);
	}

	@Test
	void clampKeepsTheUndercutInRange() {
		MercurialeConfig config = new MercurialeConfig();
		config.shopUndercutPercent = 400;
		config.url = " ";
		config.clamp();
		assertEquals(MercurialeConfig.UNDERCUT_MAX, config.shopUndercutPercent);
		assertEquals(MercurialeConfig.DEFAULT_URL, config.url);
	}

	@Test
	void serverMenuPatternMatchesVikingCraftMenus() {
		MercurialeConfig config = new MercurialeConfig();
		assertTrue(config.serverMenu().matcher("Menu | Shop - Achat").find());
		assertFalse(config.serverMenu().matcher("Large Chest").find());
		config.serverMenuPattern = "";
		assertNull(config.serverMenu());
	}
}
