package com.covercompare.quote.insurer;

import java.math.BigDecimal;
import java.util.Comparator;

/**
 * What happened when the panel asked one insurer for a price.
 */
public sealed interface InsurerOutcome {

	/** Quotes cheapest first, then declines, then insurers that could not be reached. */
	Comparator<InsurerOutcome> DISPLAY_ORDER = Comparator.comparingInt(InsurerOutcome::rank)
		.thenComparing(outcome -> outcome instanceof Quoted quoted ? quoted.totalAnnualPremium() : BigDecimal.ZERO);

	String insurerCode();

	String insurerName();

	private static int rank(InsurerOutcome outcome) {
		return switch (outcome) {
			case Quoted quoted -> 0;
			case Declined declined -> 1;
			case Unavailable unavailable -> 2;
		};
	}

	record Quoted(String insurerCode, String insurerName, BigDecimal netPremium, BigDecimal insurancePremiumTax,
			BigDecimal totalAnnualPremium) implements InsurerOutcome {
	}

	record Declined(String insurerCode, String insurerName, String reason) implements InsurerOutcome {
	}

	/**
	 * @param reason safe to show a customer; technical detail goes to the logs instead
	 */
	record Unavailable(String insurerCode, String insurerName, String reason) implements InsurerOutcome {
	}

}
