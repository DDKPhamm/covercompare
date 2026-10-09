package com.covercompare.pricing;

import java.math.BigDecimal;

public enum CoverType {

	COMPREHENSIVE(new BigDecimal("500.00")),
	THIRD_PARTY_FIRE_AND_THEFT(new BigDecimal("450.00")),
	THIRD_PARTY_ONLY(new BigDecimal("400.00"));

	private final BigDecimal baseAnnualPremium;

	CoverType(BigDecimal baseAnnualPremium) {
		this.baseAnnualPremium = baseAnnualPremium;
	}

	public BigDecimal baseAnnualPremium() {
		return baseAnnualPremium;
	}

}
