package com.covercompare.pricing.factors;

import java.math.BigDecimal;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.covercompare.pricing.RatingAdjustment;
import com.covercompare.pricing.RatingFactor;
import com.covercompare.pricing.RiskProfile;

@Component
@Order(3)
class NoClaimsDiscountFactor implements RatingFactor {

	static final String NAME = "No claims discount";

	/** Discount percentage indexed by claim-free years; 9 or more years earns the maximum. */
	private static final int[] DISCOUNT_PERCENT_BY_YEARS = { 0, 25, 35, 45, 50, 55, 60, 62, 64, 65 };

	@Override
	public RatingAdjustment assess(RiskProfile profile) {
		int years = profile.noClaimsYears();
		int discount = DISCOUNT_PERCENT_BY_YEARS[Math.min(years, DISCOUNT_PERCENT_BY_YEARS.length - 1)];
		BigDecimal multiplier = BigDecimal.valueOf(100 - discount, 2);
		return new RatingAdjustment(NAME, multiplier, years + " year(s) no claims: " + discount + "% discount");
	}

}
