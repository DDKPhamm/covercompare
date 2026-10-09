package com.covercompare.pricing.factors;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.covercompare.pricing.RatingAdjustment;
import com.covercompare.pricing.RatingFactor;
import com.covercompare.pricing.RiskProfile;

/**
 * New cars cost more to repair; very old cars are more likely to be written off and harder to source
 * parts for.
 */
@Component
@Order(6)
class VehicleAgeFactor implements RatingFactor {

	static final String NAME = "Vehicle age";

	@Override
	public RatingAdjustment assess(RiskProfile profile) {
		int years = profile.vehicleAgeYears();
		String multiplier;
		if (years < 3) {
			multiplier = "1.10";
		}
		else if (years <= 10) {
			multiplier = "1.00";
		}
		else if (years <= 15) {
			multiplier = "1.05";
		}
		else {
			multiplier = "1.15";
		}
		return RatingAdjustment.of(NAME, multiplier, "Vehicle is " + years + " year(s) old");
	}

}
