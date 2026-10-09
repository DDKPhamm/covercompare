package com.covercompare.pricing.api;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.covercompare.pricing.Rating;
import com.covercompare.pricing.RatingAdjustment;

/**
 * @param riskPremium sent to 6 decimal places: exact multiplication of eight 2dp factors produces 16,
 * and anything beyond a millionth of a penny cannot change a rounded price
 */
public record RatingResponse(List<RatingAdjustment> adjustments, BigDecimal riskPremium) {

	static final int RISK_PREMIUM_SCALE = 6;

	static RatingResponse from(Rating rating) {
		return new RatingResponse(rating.adjustments(),
				rating.riskPremium().setScale(RISK_PREMIUM_SCALE, RoundingMode.HALF_UP));
	}

}
