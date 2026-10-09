package com.covercompare.pricing;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Applies every {@link RatingFactor} to the cover type's base premium. The result is the market
 * risk premium: what the risk costs before any insurer adds its own commercial loading and tax.
 */
@Service
public class RatingEngine {

	private final List<RatingFactor> ratingFactors;

	public RatingEngine(List<RatingFactor> ratingFactors) {
		this.ratingFactors = List.copyOf(ratingFactors);
	}

	public Rating rate(RiskProfile profile) {
		List<RatingAdjustment> adjustments = ratingFactors.stream().map(factor -> factor.assess(profile)).toList();
		BigDecimal riskPremium = adjustments.stream()
			.map(RatingAdjustment::multiplier)
			.reduce(profile.coverType().baseAnnualPremium(), BigDecimal::multiply);
		return new Rating(adjustments, riskPremium);
	}

}
