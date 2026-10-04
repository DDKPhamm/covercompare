package com.covercompare.pricing;

import java.util.List;

/**
 * @param adjustments the rating factors applied to every insurer's price, in breakdown order
 * @param offers quoted offers cheapest first, followed by declines
 */
public record PricingResult(List<RatingAdjustment> adjustments, List<InsurerOffer> offers) {
}
