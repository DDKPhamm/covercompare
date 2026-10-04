package com.covercompare.pricing;

import java.math.BigDecimal;

/**
 * What one insurer returned for a risk: either a price or a refusal to quote.
 */
public sealed interface InsurerOffer {

	Insurer insurer();

	record Quoted(Insurer insurer, BigDecimal netPremium, BigDecimal insurancePremiumTax,
			BigDecimal totalAnnualPremium) implements InsurerOffer {
	}

	record Declined(Insurer insurer, String reason) implements InsurerOffer {
	}

}
