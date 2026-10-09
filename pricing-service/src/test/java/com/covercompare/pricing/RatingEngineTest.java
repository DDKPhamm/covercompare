package com.covercompare.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class RatingEngineTest {

	@Test
	void multipliesTheBasePremiumByEveryFactor() {
		RatingFactor oneAndAHalf = profile -> RatingAdjustment.of("Loading", "1.50", "test");
		RatingFactor tenPercentOff = profile -> RatingAdjustment.of("Discount", "0.90", "test");
		RatingEngine engine = new RatingEngine(List.of(oneAndAHalf, tenPercentOff));

		Rating rating = engine.rate(RiskProfiles.standard().coverType(CoverType.COMPREHENSIVE).build());

		assertThat(rating.riskPremium()).isEqualByComparingTo("675.00");
		assertThat(rating.adjustments()).extracting(RatingAdjustment::factor).containsExactly("Loading", "Discount");
	}

	@Test
	void keepsFullPrecisionSoInsurersRoundOnlyOnce() {
		RatingFactor fine = profile -> RatingAdjustment.of("Fine", "1.0000125", "test");
		RatingEngine engine = new RatingEngine(List.of(fine));

		Rating rating = engine.rate(RiskProfiles.standard().coverType(CoverType.THIRD_PARTY_ONLY).build());

		assertThat(rating.riskPremium()).isEqualByComparingTo("400.005");
	}

	@Test
	void usesTheCoverTypesBasePremiumWhenNoFactorsApply() {
		RatingEngine engine = new RatingEngine(List.of());

		assertThat(engine.rate(RiskProfiles.standard().coverType(CoverType.THIRD_PARTY_FIRE_AND_THEFT).build())
			.riskPremium()).isEqualByComparingTo("450.00");
	}

}
