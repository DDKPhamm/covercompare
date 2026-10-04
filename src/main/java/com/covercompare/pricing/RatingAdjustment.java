package com.covercompare.pricing;

import java.math.BigDecimal;

public record RatingAdjustment(String factor, BigDecimal multiplier, String reason) {

	public static RatingAdjustment of(String factor, String multiplier, String reason) {
		return new RatingAdjustment(factor, new BigDecimal(multiplier), reason);
	}

}
