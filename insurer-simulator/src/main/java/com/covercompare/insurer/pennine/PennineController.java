package com.covercompare.insurer.pennine;

import java.math.BigDecimal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.covercompare.insurer.FaultInjector;
import com.covercompare.insurer.Underwriter;
import com.covercompare.insurer.Underwriter.Decision;

/**
 * Pennine Mutual: a modern JSON API that always answers 200 and puts the outcome in the body.
 */
@RestController
class PennineController {

	static final String CODE = "pennine";

	private final Underwriter underwriter;

	private final FaultInjector faults;

	PennineController(Underwriter underwriter, FaultInjector faults) {
		this.underwriter = underwriter;
		this.faults = faults;
	}

	@PostMapping(path = "/pennine/v1/quotes", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	QuoteResponse quote(@Valid @RequestBody QuoteRequest request) {
		faults.apply(CODE);
		return switch (underwriter.underwrite(CODE, request.driverAge(), request.vehicleGroup(),
				request.riskPremium())) {
			case Decision.Quoted quoted -> new QuoteResponse("QUOTED", quoted.netPremium(),
					quoted.insurancePremiumTax(), quoted.totalPremium(), null);
			case Decision.Declined declined -> new QuoteResponse("DECLINED", null, null, null, declined.reason());
		};
	}

	record QuoteRequest(
			@NotNull @Min(17) Integer driverAge,
			@NotNull @Min(1) @Max(50) Integer vehicleGroup,
			@NotNull @Positive BigDecimal riskPremium) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	record QuoteResponse(String outcome, BigDecimal netPremium, BigDecimal tax, BigDecimal totalPremium,
			String declineReason) {
	}

}
