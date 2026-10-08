package fr.imero.mercuriale.price;

public record ShopPrice(String material, Double buyFrom, Double sellTo, int sellers, int buyers) {
	public boolean isEmpty() {
		return buyFrom == null && sellTo == null;
	}
}
