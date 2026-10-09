package com.covercompare.quote.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "insurer_quote")
public class InsurerQuote {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quote_id", nullable = false)
	private Quote quote;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(nullable = false, length = 30)
	private String insurerCode;

	@Column(nullable = false, length = 100)
	private String insurerName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private InsurerQuoteStatus status;

	@Column(precision = 10, scale = 2)
	private BigDecimal netPremium;

	@Column(precision = 10, scale = 2)
	private BigDecimal insurancePremiumTax;

	@Column(precision = 10, scale = 2)
	private BigDecimal totalAnnualPremium;

	private String reason;

	protected InsurerQuote() {
	}

	private InsurerQuote(String insurerCode, String insurerName, InsurerQuoteStatus status) {
		this.insurerCode = insurerCode;
		this.insurerName = insurerName;
		this.status = status;
	}

	public static InsurerQuote quoted(String insurerCode, String insurerName, BigDecimal netPremium,
			BigDecimal insurancePremiumTax, BigDecimal totalAnnualPremium) {
		InsurerQuote insurerQuote = new InsurerQuote(insurerCode, insurerName, InsurerQuoteStatus.QUOTED);
		insurerQuote.netPremium = netPremium;
		insurerQuote.insurancePremiumTax = insurancePremiumTax;
		insurerQuote.totalAnnualPremium = totalAnnualPremium;
		return insurerQuote;
	}

	public static InsurerQuote declined(String insurerCode, String insurerName, String reason) {
		InsurerQuote insurerQuote = new InsurerQuote(insurerCode, insurerName, InsurerQuoteStatus.DECLINED);
		insurerQuote.reason = reason;
		return insurerQuote;
	}

	public static InsurerQuote unavailable(String insurerCode, String insurerName, String reason) {
		InsurerQuote insurerQuote = new InsurerQuote(insurerCode, insurerName, InsurerQuoteStatus.UNAVAILABLE);
		insurerQuote.reason = reason;
		return insurerQuote;
	}

	void attachTo(Quote quote, int displayOrder) {
		this.quote = quote;
		this.displayOrder = displayOrder;
	}

	public UUID getId() {
		return id;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public String getInsurerCode() {
		return insurerCode;
	}

	public String getInsurerName() {
		return insurerName;
	}

	public InsurerQuoteStatus getStatus() {
		return status;
	}

	public BigDecimal getNetPremium() {
		return netPremium;
	}

	public BigDecimal getInsurancePremiumTax() {
		return insurancePremiumTax;
	}

	public BigDecimal getTotalAnnualPremium() {
		return totalAnnualPremium;
	}

	/** Why the insurer declined, or why it could not be reached; null for a quote. */
	public String getReason() {
		return reason;
	}

}
