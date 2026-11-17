package fr.imero.mercuriale.config;

import java.util.Locale;

public enum PriceAmount {
	UNIT,
	STACK,
	BOTH;

	public boolean scalesToStack(int count) {
		return this == STACK && count > 1;
	}

	public boolean addsStackLine(int count) {
		return this == BOTH && count > 1;
	}

	public PriceAmount next() {
		PriceAmount[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public String translationKey() {
		return "mercuriale.amount." + name().toLowerCase(Locale.ROOT);
	}
}
