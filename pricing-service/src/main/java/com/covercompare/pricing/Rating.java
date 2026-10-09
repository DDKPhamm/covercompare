package com.covercompare.pricing;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param adjustments every factor applied, in breakdown order
 * @param riskPremium unrounded, so insurers round only once, after adding their own loading
 */
public record Rating(List<RatingAdjustment> adjustments, BigDecimal riskPremium) {
}
