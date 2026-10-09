package com.covercompare.quote.rating;

import com.covercompare.quote.domain.CoverType;

/**
 * What quote-service sends to pricing-service. Holds derived values (age, postcode area) rather
 * than personal data (date of birth, full postcode). See contracts/pricing/rating-request.json.
 */
public record RatingRequest(
		CoverType coverType,
		int driverAge,
		int licenceHeldYears,
		int noClaimsYears,
		String postcodeArea,
		int vehicleInsuranceGroup,
		int vehicleAgeYears,
		int voluntaryExcess) {
}
