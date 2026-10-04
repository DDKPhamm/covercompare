package com.covercompare.pricing.factors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.covercompare.pricing.Insurer;
import com.covercompare.pricing.InsurerOffer;
import com.covercompare.pricing.PricingEngine;
import com.covercompare.pricing.PricingProperties;
import com.covercompare.pricing.PricingResult;
import com.covercompare.pricing.RatingAdjustment;
import com.covercompare.pricing.RiskProfiles;

/**
 * Prices the standard profile with every real rating factor and checks the result against a
 * calculation worked out by hand:
 *
 * <pre>
 * 500.00 base (comprehensive)
 *   x 1.00 age 35   x 1.00 licence 10 years   x 0.45 five years no claims
 *   x 1.10 SK area  x 1.37 group 20           x 1.00 vehicle 5 years old   x 0.95 £250 excess
 * = 322.12125 risk premium
 *
 * Pennine    x 0.95 = 306.02 net + 36.72 IPT = 342.74
 * Lighthouse x 1.00 = 322.12 net + 38.65 IPT = 360.77
 * Redbrick   x 1.12 = 360.78 net + 43.29 IPT = 404.07
 * </pre>
 */
class FullPricingCalculationTest {

	private final PricingEngine engine = new PricingEngine(
			List.of(new DriverAgeFactor(), new DrivingExperienceFactor(), new NoClaimsDiscountFactor(),
					new PostcodeAreaFactor(), new VehicleGroupFactor(), new VehicleAgeFactor(),
					new VoluntaryExcessFactor()),
			new PricingProperties(new BigDecimal("0.12"),
					List.of(insurer("REDBRICK", "1.12"), insurer("PENNINE", "0.95"), insurer("LIGHTHOUSE", "1.00"))));

	@Test
	void pricesTheStandardProfileExactly() {
		PricingResult result = engine.price(RiskProfiles.standard().build());

		assertThat(result.adjustments()).extracting(RatingAdjustment::factor)
			.containsExactly(DriverAgeFactor.NAME, DrivingExperienceFactor.NAME, NoClaimsDiscountFactor.NAME,
					PostcodeAreaFactor.NAME, VehicleGroupFactor.NAME, VehicleAgeFactor.NAME,
					VoluntaryExcessFactor.NAME);
		assertThat(result.offers()).map(InsurerOffer.Quoted.class::cast)
			.extracting(offer -> offer.insurer().code(), InsurerOffer.Quoted::netPremium,
					InsurerOffer.Quoted::insurancePremiumTax, InsurerOffer.Quoted::totalAnnualPremium)
			.containsExactly(
					tuple("PENNINE", new BigDecimal("306.02"), new BigDecimal("36.72"), new BigDecimal("342.74")),
					tuple("LIGHTHOUSE", new BigDecimal("322.12"), new BigDecimal("38.65"), new BigDecimal("360.77")),
					tuple("REDBRICK", new BigDecimal("360.78"), new BigDecimal("43.29"), new BigDecimal("404.07")));
	}

	private static Insurer insurer(String code, String multiplier) {
		return new Insurer(code, code, new BigDecimal(multiplier), 17, 50);
	}

}
