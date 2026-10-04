package com.covercompare.pricing.factors;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.covercompare.pricing.RatingAdjustment;
import com.covercompare.pricing.RatingFactor;
import com.covercompare.pricing.RiskProfile;

/**
 * Agreeing to pay more towards a claim yourself (a higher voluntary excess) lowers the premium.
 */
@Component
@Order(7)
class VoluntaryExcessFactor implements RatingFactor {

	static final String NAME = "Voluntary excess";

	@Override
	public RatingAdjustment assess(RiskProfile profile) {
		int excess = profile.voluntaryExcess();
		String multiplier;
		if (excess >= 1000) {
			multiplier = "0.85";
		}
		else if (excess >= 500) {
			multiplier = "0.90";
		}
		else if (excess >= 250) {
			multiplier = "0.95";
		}
		else {
			multiplier = "1.00";
		}
		return RatingAdjustment.of(NAME, multiplier, "Voluntary excess of £" + excess);
	}

}
