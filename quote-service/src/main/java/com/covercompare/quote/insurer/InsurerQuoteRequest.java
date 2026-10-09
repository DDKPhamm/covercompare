package com.covercompare.quote.insurer;

import java.math.BigDecimal;

/**
 * The only facts an insurer needs: the two it uses to decide whether to cover the risk, and the
 * market risk premium it prices from. No names, dates of birth or postcodes leave this service.
 */
public record InsurerQuoteRequest(int driverAge, int vehicleInsuranceGroup, BigDecimal riskPremium) {
}
