package com.covercompare.quote.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AppliedRatingFactor {

	@Column(nullable = false, length = 50)
	private String factor;

	@Column(nullable = false, precision = 6, scale = 4)
	private BigDecimal multiplier;

	@Column(nullable = false)
	private String reason;

	protected AppliedRatingFactor() {
	}

	public AppliedRatingFactor(String factor, BigDecimal multiplier, String reason) {
		this.factor = factor;
		this.multiplier = multiplier;
		this.reason = reason;
	}

	public String getFactor() {
		return factor;
	}

	public BigDecimal getMultiplier() {
		return multiplier;
	}

	public String getReason() {
		return reason;
	}

}
