package com.covercompare.pricing.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.covercompare.pricing.CoverType;
import com.covercompare.pricing.RiskProfile;

public record RatingRequest(
		@NotNull CoverType coverType,
		@NotNull @Min(17) @Max(110) Integer driverAge,
		@NotNull @Min(0) @Max(93) Integer licenceHeldYears,
		@NotNull @Min(0) @Max(93) Integer noClaimsYears,
		@NotBlank @Pattern(regexp = "[A-Z]{1,2}", message = "must be the letters of a UK postcode area") String postcodeArea,
		@NotNull @Min(1) @Max(50) Integer vehicleInsuranceGroup,
		@NotNull @Min(0) @Max(100) Integer vehicleAgeYears,
		@NotNull @Min(0) @Max(1500) Integer voluntaryExcess) {

	RiskProfile toRiskProfile() {
		return new RiskProfile(coverType, driverAge, licenceHeldYears, noClaimsYears, postcodeArea,
				vehicleInsuranceGroup, vehicleAgeYears, voluntaryExcess);
	}

}
