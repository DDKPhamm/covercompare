package com.covercompare.quote.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.covercompare.pricing.CoverType;

public record QuoteRequest(
		@NotNull @Valid DriverDetails driver,
		@NotNull @Valid VehicleDetails vehicle,
		@NotNull CoverType coverType,
		@NotNull @Min(0) @Max(1500) Integer voluntaryExcess) {
}
