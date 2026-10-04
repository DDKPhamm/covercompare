package com.covercompare.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class PricingEngine {

	private static final Comparator<InsurerOffer> CHEAPEST_FIRST_THEN_DECLINES = Comparator
		.comparing((InsurerOffer offer) -> offer instanceof InsurerOffer.Declined)
		.thenComparing(offer -> offer instanceof InsurerOffer.Quoted quoted ? quoted.totalAnnualPremium()
				: BigDecimal.ZERO);

	private final List<RatingFactor> ratingFactors;

	private final PricingProperties properties;

	public PricingEngine(List<RatingFactor> ratingFactors, PricingProperties properties) {
		this.ratingFactors = List.copyOf(ratingFactors);
		this.properties = properties;
	}

	public PricingResult price(RiskProfile profile) {
		List<RatingAdjustment> adjustments = ratingFactors.stream().map(factor -> factor.assess(profile)).toList();

		BigDecimal riskPremium = adjustments.stream()
			.map(RatingAdjustment::multiplier)
			.reduce(profile.coverType().baseAnnualPremium(), BigDecimal::multiply);

		List<InsurerOffer> offers = properties.insurers()
			.stream()
			.map(insurer -> offerFrom(insurer, profile, riskPremium))
			.sorted(CHEAPEST_FIRST_THEN_DECLINES)
			.toList();

		return new PricingResult(adjustments, offers);
	}

	private InsurerOffer offerFrom(Insurer insurer, RiskProfile profile, BigDecimal riskPremium) {
		return declineReason(insurer, profile).<InsurerOffer>map(reason -> new InsurerOffer.Declined(insurer, reason))
			.orElseGet(() -> quote(insurer, riskPremium));
	}

	private Optional<String> declineReason(Insurer insurer, RiskProfile profile) {
		if (profile.driverAge() < insurer.minDriverAge()) {
			return Optional.of("Driver must be at least " + insurer.minDriverAge());
		}
		if (profile.vehicleInsuranceGroup() > insurer.maxVehicleGroup()) {
			return Optional.of("Vehicle insurance group must be " + insurer.maxVehicleGroup() + " or below");
		}
		return Optional.empty();
	}

	private InsurerOffer.Quoted quote(Insurer insurer, BigDecimal riskPremium) {
		// Round each component to pence before adding, so net + tax always equals the total shown.
		BigDecimal net = riskPremium.multiply(insurer.rateMultiplier()).setScale(2, RoundingMode.HALF_UP);
		BigDecimal tax = net.multiply(properties.insurancePremiumTaxRate()).setScale(2, RoundingMode.HALF_UP);
		return new InsurerOffer.Quoted(insurer, net, tax, net.add(tax));
	}

}
