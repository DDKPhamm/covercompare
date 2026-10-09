package com.covercompare.quote.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.covercompare.quote.domain.AppliedRatingFactor;
import com.covercompare.quote.domain.CoverType;
import com.covercompare.quote.domain.InsurerQuote;
import com.covercompare.quote.domain.InsurerQuoteStatus;
import com.covercompare.quote.domain.Quote;

public record QuoteResponse(
		UUID id,
		Instant createdAt,
		Instant validUntil,
		CoverType coverType,
		List<RatingFactorResponse> ratingFactors,
		List<InsurerQuoteResponse> insurerQuotes) {

	public static QuoteResponse from(Quote quote) {
		return new QuoteResponse(quote.getId(), quote.getCreatedAt(), quote.getValidUntil(), quote.getCoverType(),
				quote.getRatingFactors().stream().map(RatingFactorResponse::from).toList(),
				quote.getInsurerQuotes().stream().map(InsurerQuoteResponse::from).toList());
	}

	public record RatingFactorResponse(String factor, BigDecimal multiplier, String reason) {

		static RatingFactorResponse from(AppliedRatingFactor factor) {
			return new RatingFactorResponse(factor.getFactor(), factor.getMultiplier(), factor.getReason());
		}

	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record InsurerQuoteResponse(
			String insurerCode,
			String insurerName,
			InsurerQuoteStatus status,
			BigDecimal netPremium,
			BigDecimal insurancePremiumTax,
			BigDecimal totalAnnualPremium,
			String declineReason,
			String unavailableReason) {

		static InsurerQuoteResponse from(InsurerQuote insurerQuote) {
			InsurerQuoteStatus status = insurerQuote.getStatus();
			return new InsurerQuoteResponse(insurerQuote.getInsurerCode(), insurerQuote.getInsurerName(), status,
					insurerQuote.getNetPremium(), insurerQuote.getInsurancePremiumTax(),
					insurerQuote.getTotalAnnualPremium(),
					status == InsurerQuoteStatus.DECLINED ? insurerQuote.getReason() : null,
					status == InsurerQuoteStatus.UNAVAILABLE ? insurerQuote.getReason() : null);
		}

	}

}
