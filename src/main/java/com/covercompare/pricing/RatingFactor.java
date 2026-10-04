package com.covercompare.pricing;

/**
 * One rule that loads or discounts the base premium. Implementations are Spring beans, so adding a
 * new factor means adding a class, not editing {@link PricingEngine}. Use {@code @Order} to control
 * where it appears in the price breakdown.
 */
public interface RatingFactor {

	RatingAdjustment assess(RiskProfile profile);

}
