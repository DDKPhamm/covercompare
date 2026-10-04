package com.covercompare.pricing.factors;

import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.covercompare.pricing.RatingAdjustment;
import com.covercompare.pricing.RatingFactor;
import com.covercompare.pricing.RiskProfile;

/**
 * Illustrative regional loadings keyed by postcode area (the leading letters, e.g. "SK" for
 * Stockport). Areas not listed are rated at 1.00.
 */
@Component
@Order(4)
class PostcodeAreaFactor implements RatingFactor {

	static final String NAME = "Postcode area";

	private static final Map<String, String> MULTIPLIER_BY_AREA = Map.ofEntries(
			Map.entry("E", "1.45"), Map.entry("EC", "1.45"), Map.entry("N", "1.45"), Map.entry("NW", "1.45"),
			Map.entry("SE", "1.45"), Map.entry("SW", "1.45"), Map.entry("W", "1.45"), Map.entry("WC", "1.45"),
			Map.entry("M", "1.35"), Map.entry("B", "1.30"), Map.entry("L", "1.30"), Map.entry("LS", "1.20"),
			Map.entry("S", "1.15"), Map.entry("SK", "1.10"), Map.entry("EX", "0.85"), Map.entry("TR", "0.85"),
			Map.entry("LD", "0.85"));

	@Override
	public RatingAdjustment assess(RiskProfile profile) {
		String area = profile.postcodeArea();
		String multiplier = MULTIPLIER_BY_AREA.getOrDefault(area, "1.00");
		return RatingAdjustment.of(NAME, multiplier, "Postcode area " + area);
	}

}
