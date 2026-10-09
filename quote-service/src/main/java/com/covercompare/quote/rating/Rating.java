package com.covercompare.quote.rating;

import java.math.BigDecimal;
import java.util.List;

/**
 * What pricing-service returns. This service keeps its own copy of the shape rather than sharing a
 * library with pricing-service, so the two can be built and released independently. The shared
 * contract files keep the copies in step.
 */
public record Rating(List<Adjustment> adjustments, BigDecimal riskPremium) {

	public record Adjustment(String factor, BigDecimal multiplier, String reason) {
	}

}
