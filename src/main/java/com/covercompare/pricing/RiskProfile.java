package com.covercompare.pricing;

/**
 * The facts about a driver and vehicle that affect price. Deliberately holds derived values
 * (age, postcode area) rather than personal data (date of birth, full postcode).
 */
public record RiskProfile(
		CoverType coverType,
		int driverAge,
		int licenceHeldYears,
		int noClaimsYears,
		String postcodeArea,
		int vehicleInsuranceGroup,
		int vehicleAgeYears,
		int voluntaryExcess) {
}
