package com.covercompare.insurer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * @param insurers keyed by the insurer's code as used in its URL, for example {@code pennine}
 */
@Validated
@ConfigurationProperties("simulator")
public record SimulatorProperties(
		@NotNull @DecimalMin("0.00") @DecimalMax("1.00") BigDecimal insurancePremiumTaxRate,
		@NotEmpty Map<String, @Valid InsurerSettings> insurers) {

	public InsurerSettings insurer(String code) {
		InsurerSettings settings = insurers.get(code);
		if (settings == null) {
			throw new IllegalArgumentException("No simulated insurer configured with code " + code);
		}
		return settings;
	}

	/**
	 * @param rateMultiplier the insurer's commercial loading on top of the market risk premium
	 */
	public record InsurerSettings(
			@NotBlank String name,
			@NotNull @Positive BigDecimal rateMultiplier,
			@Min(17) int minDriverAge,
			@Min(1) @Max(50) int maxVehicleGroup,
			@Valid @DefaultValue Faults faults) {
	}

	/**
	 * Misbehaviour to simulate, so the quote service's timeouts, retries and circuit breakers can be
	 * seen working.
	 * @param latency added to every response
	 * @param jitter up to this much extra latency, chosen at random per request
	 * @param failureRate fraction of requests, from 0 to 1, that fail with 503 Service Unavailable
	 */
	public record Faults(
			@DefaultValue("0ms") Duration latency,
			@DefaultValue("0ms") Duration jitter,
			@DefaultValue("0") @DecimalMin("0") @DecimalMax("1") double failureRate) {
	}

}
