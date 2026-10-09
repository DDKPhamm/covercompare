package com.covercompare.quote.insurer;

import java.math.BigDecimal;

/**
 * An insurer's answer, translated out of its own API format. Technical failures are not responses:
 * adapters throw, and {@link InsurerPanel} decides what that means.
 */
public sealed interface InsurerResponse {

	record Quoted(BigDecimal netPremium, BigDecimal insurancePremiumTax, BigDecimal totalAnnualPremium)
			implements InsurerResponse {
	}

	record Declined(String reason) implements InsurerResponse {
	}

}
