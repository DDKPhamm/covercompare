package com.covercompare.pricing.factors;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.covercompare.pricing.Rating;
import com.covercompare.pricing.RatingAdjustment;
import com.covercompare.pricing.RatingEngine;
import com.covercompare.pricing.RiskProfiles;

/**
 * Rates the standard profile with every real rating factor and checks the result against a
 * calculation worked out by hand:
 *
 * <pre>
 * 500.00 base (comprehensive)
 *   x 1.00 age 35   x 1.00 licence 10 years   x 0.45 five years no claims
 *   x 1.10 SK area  x 1.37 group 20           x 1.00 vehicle 5 years old   x 0.95 £250 excess
 * = 322.12125 risk premium
 * </pre>
 */
class FullRatingCalculationTest {

	private final RatingEngine engine = new RatingEngine(List.of(new DriverAgeFactor(), new DrivingExperienceFactor(),
			new NoClaimsDiscountFactor(), new PostcodeAreaFactor(), new VehicleGroupFactor(), new VehicleAgeFactor(),
			new VoluntaryExcessFactor()));

	@Test
	void ratesTheStandardProfileExactly() {
		Rating rating = engine.rate(RiskProfiles.standard().build());

		assertThat(rating.adjustments()).extracting(RatingAdjustment::factor)
			.containsExactly(DriverAgeFactor.NAME, DrivingExperienceFactor.NAME, NoClaimsDiscountFactor.NAME,
					PostcodeAreaFactor.NAME, VehicleGroupFactor.NAME, VehicleAgeFactor.NAME,
					VoluntaryExcessFactor.NAME);
		assertThat(rating.riskPremium()).isEqualByComparingTo("322.12125");
	}

}
