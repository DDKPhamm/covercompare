package com.covercompare.pricing.factors;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.covercompare.pricing.RatingAdjustment;
import com.covercompare.pricing.RatingFactor;
import com.covercompare.pricing.RiskProfile;

@Component
@Order(1)
class DriverAgeFactor implements RatingFactor {

	static final String NAME = "Driver age";

	@Override
	public RatingAdjustment assess(RiskProfile profile) {
		int age = profile.driverAge();
		String multiplier;
		if (age < 21) {
			multiplier = "2.20";
		}
		else if (age < 25) {
			multiplier = "1.70";
		}
		else if (age < 30) {
			multiplier = "1.25";
		}
		else if (age < 60) {
			multiplier = "1.00";
		}
		else if (age < 70) {
			multiplier = "1.10";
		}
		else {
			multiplier = "1.35";
		}
		return RatingAdjustment.of(NAME, multiplier, "Driver aged " + age);
	}

}
