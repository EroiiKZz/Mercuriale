package fr.imero.mercuriale.price;

public record MarketPrice(String material, String name, boolean vanilla, String slug, double price, Tier tier,
		int samples, Integer windowDays, int onSale, Double cheapest) {
	public enum Tier {
		SOLD,
		ASKED,
		THIN;

		public static Tier parse(String value) {
			if ("sold".equals(value)) {
				return SOLD;
			}
			if ("asked".equals(value)) {
				return ASKED;
			}
			return THIN;
		}
	}
}
