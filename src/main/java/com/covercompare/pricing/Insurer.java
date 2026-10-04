package com.covercompare.pricing;

import java.math.BigDecimal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * An insurer on the comparison panel and its "appetite": the risks it is willing to quote for.
 */
public record Insurer(
		@NotBlank String code,
		@NotBlank String name,
		@NotNull @Positive BigDecimal rateMultiplier,
		@Min(17) int minDriverAge,
		@Min(1) @Max(50) int maxVehicleGroup) {
}
