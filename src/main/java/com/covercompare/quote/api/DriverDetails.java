package com.covercompare.quote.api;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import com.covercompare.quote.UkPostcode;

public record DriverDetails(
		@NotNull @Past LocalDate dateOfBirth,
		@NotNull @Min(0) @Max(80) Integer licenceHeldYears,
		@NotNull @Min(0) @Max(80) Integer noClaimsYears,
		@NotBlank @Pattern(regexp = UkPostcode.PATTERN, message = "must be a valid UK postcode") String postcode) {
}
