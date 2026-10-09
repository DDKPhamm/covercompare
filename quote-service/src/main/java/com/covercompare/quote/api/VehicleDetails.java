package com.covercompare.quote.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VehicleDetails(
		@NotBlank @Size(max = 50) String make,
		@NotBlank @Size(max = 50) String model,
		@NotNull @Min(1950) Integer year,
		@NotNull @Min(1) @Max(50) Integer insuranceGroup) {
}
