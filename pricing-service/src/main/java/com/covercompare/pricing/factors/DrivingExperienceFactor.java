package com.covercompare.pricing.factors;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.covercompare.pricing.RatingAdjustment;
import com.covercompare.pricing.RatingFactor;
import com.covercompare.pricing.RiskProfile;

@Component
@Order(2)
class DrivingExperienceFactor implements RatingFactor {

	static final String NAME = "Driving experience";

	@Override
	public RatingAdjustment assess(RiskProfile profile) {
		int years = profile.licenceHeldYears();
		String multiplier;
		if (years < 1) {
			multiplier = "1.40";
		}
		else if (years < 3) {
			multiplier = "1.20";
		}
		else if (years < 5) {
			multiplier = "1.10";
		}
		else {
			multiplier = "1.00";
		}
		return RatingAdjustment.of(NAME, multiplier, "Licence held for " + years + " year(s)");
	}

}
