package com.covercompare.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class PricingEngineTest {

	private static final RatingFactor ONE_AND_A_HALF = profile -> RatingAdjustment.of("Test loading", "1.50", "test");

	@Test
	void multipliesBasePremiumByEveryFactorAndAddsInsurancePremiumTax() {
		PricingEngine engine = engine(List.of(ONE_AND_A_HALF), insurer("ACME", "1.00", 17, 50));

		PricingResult result = engine.price(RiskProfiles.standard().coverType(CoverType.COMPREHENSIVE).build());

		InsurerOffer.Quoted offer = (InsurerOffer.Quoted) result.offers().getFirst();
		assertThat(offer.netPremium()).isEqualTo(new BigDecimal("750.00"));
		assertThat(offer.insurancePremiumTax()).isEqualTo(new BigDecimal("90.00"));
		assertThat(offer.totalAnnualPremium()).isEqualTo(new BigDecimal("840.00"));
		assertThat(result.adjustments()).singleElement().extracting(RatingAdjustment::factor).isEqualTo("Test loading");
	}

	@Test
	void roundsHalfPenniesUpAndKeepsTotalEqualToNetPlusTax() {
		RatingFactor halfPenny = profile -> RatingAdjustment.of("Half penny", "1.0000125", "test");
		PricingEngine engine = engine(List.of(halfPenny), insurer("ACME", "1.00", 17, 50));

		PricingResult result = engine.price(RiskProfiles.standard().coverType(CoverType.THIRD_PARTY_ONLY).build());

		InsurerOffer.Quoted offer = (InsurerOffer.Quoted) result.offers().getFirst();
		assertThat(offer.netPremium()).isEqualTo(new BigDecimal("400.01"));
		assertThat(offer.insurancePremiumTax()).isEqualTo(new BigDecimal("48.00"));
		assertThat(offer.totalAnnualPremium()).isEqualTo(offer.netPremium().add(offer.insurancePremiumTax()));
	}

	@Test
	void appliesEachInsurersOwnRateMultiplier() {
		PricingEngine engine = engine(List.of(), insurer("CHEAP", "0.80", 17, 50));

		PricingResult result = engine.price(RiskProfiles.standard().coverType(CoverType.COMPREHENSIVE).build());

		assertThat(((InsurerOffer.Quoted) result.offers().getFirst()).netPremium())
			.isEqualTo(new BigDecimal("400.00"));
	}

	@Test
	void declinesDriversYoungerThanTheInsurersMinimumAge() {
		PricingEngine engine = engine(List.of(), insurer("OVER25", "1.00", 25, 50));

		PricingResult result = engine.price(RiskProfiles.standard().driverAge(22).build());

		assertThat(result.offers()).singleElement()
			.isInstanceOfSatisfying(InsurerOffer.Declined.class,
					declined -> assertThat(declined.reason()).isEqualTo("Driver must be at least 25"));
	}

	@Test
	void declinesVehiclesAboveTheInsurersMaximumGroup() {
		PricingEngine engine = engine(List.of(), insurer("SMALLCARS", "1.00", 17, 40));

		PricingResult result = engine.price(RiskProfiles.standard().vehicleInsuranceGroup(41).build());

		assertThat(result.offers()).singleElement()
			.isInstanceOfSatisfying(InsurerOffer.Declined.class, declined -> assertThat(declined.reason())
				.isEqualTo("Vehicle insurance group must be 40 or below"));
	}

	@Test
	void ordersQuotesCheapestFirstWithDeclinesLast() {
		PricingEngine engine = engine(List.of(), insurer("DECLINES", "0.50", 99, 50), insurer("PRICEY", "1.20", 17, 50),
				insurer("CHEAP", "0.90", 17, 50));

		PricingResult result = engine.price(RiskProfiles.standard().build());

		assertThat(result.offers()).extracting(offer -> offer.insurer().code())
			.containsExactly("CHEAP", "PRICEY", "DECLINES");
	}

	private static PricingEngine engine(List<RatingFactor> factors, Insurer... insurers) {
		return new PricingEngine(factors, new PricingProperties(new BigDecimal("0.12"), List.of(insurers)));
	}

	private static Insurer insurer(String code, String multiplier, int minDriverAge, int maxVehicleGroup) {
		return new Insurer(code, code + " Insurance", new BigDecimal(multiplier), minDriverAge, maxVehicleGroup);
	}

}
