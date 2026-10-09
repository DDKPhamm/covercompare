package com.covercompare.insurer.lighthouse;

import java.math.BigDecimal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.covercompare.insurer.FaultInjector;
import com.covercompare.insurer.Underwriter;
import com.covercompare.insurer.Underwriter.Decision;

/**
 * Lighthouse Direct: snake_case JSON, prices as whole pence, and its own status vocabulary.
 */
@RestController
class LighthouseController {

	static final String CODE = "lighthouse";

	private final Underwriter underwriter;

	private final FaultInjector faults;

	LighthouseController(Underwriter underwriter, FaultInjector faults) {
		this.underwriter = underwriter;
		this.faults = faults;
	}

	@PostMapping(path = "/lighthouse/api/quote", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	QuoteResponse quote(@Valid @RequestBody QuoteRequest request) {
		faults.apply(CODE);
		return switch (underwriter.underwrite(CODE, request.driverAge(), request.vehicleGroup(),
				request.riskPremium())) {
			case Decision.Quoted quoted -> new QuoteResponse("ACCEPTED", pence(quoted.netPremium()),
					pence(quoted.insurancePremiumTax()), pence(quoted.totalPremium()), null);
			case Decision.Declined declined -> new QuoteResponse("REJECTED", null, null, null, declined.reason());
		};
	}

	private static Long pence(BigDecimal pounds) {
		return pounds.movePointRight(2).longValueExact();
	}

	record QuoteRequest(
			@JsonProperty("driver_age") @NotNull @Min(17) Integer driverAge,
			@JsonProperty("vehicle_group") @NotNull @Min(1) @Max(50) Integer vehicleGroup,
			@JsonProperty("risk_premium") @NotNull @Positive BigDecimal riskPremium) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	record QuoteResponse(
			String status,
			@JsonProperty("premium_pence") Long premiumPence,
			@JsonProperty("ipt_pence") Long iptPence,
			@JsonProperty("total_pence") Long totalPence,
			String message) {
	}

}
