package com.covercompare.quote;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.covercompare.pricing.InsurerOffer;
import com.covercompare.pricing.PricingEngine;
import com.covercompare.pricing.PricingResult;
import com.covercompare.pricing.RiskProfile;
import com.covercompare.quote.api.DriverDetails;
import com.covercompare.quote.api.QuoteRequest;
import com.covercompare.quote.api.QuoteResponse;
import com.covercompare.quote.api.VehicleDetails;
import com.covercompare.quote.domain.AppliedRatingFactor;
import com.covercompare.quote.domain.InsurerQuote;
import com.covercompare.quote.domain.Quote;
import com.covercompare.quote.domain.QuoteRepository;

@Service
public class QuoteService {

	static final int MINIMUM_DRIVING_AGE = 17;

	private static final int MAXIMUM_DRIVER_AGE = 110;

	private final PricingEngine pricingEngine;

	private final QuoteRepository quoteRepository;

	private final QuoteProperties properties;

	private final Clock clock;

	public QuoteService(PricingEngine pricingEngine, QuoteRepository quoteRepository, QuoteProperties properties,
			Clock clock) {
		this.pricingEngine = pricingEngine;
		this.quoteRepository = quoteRepository;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	public QuoteResponse createQuote(QuoteRequest request) {
		LocalDate today = LocalDate.now(clock);
		DriverDetails driver = request.driver();
		VehicleDetails vehicle = request.vehicle();
		int driverAge = Period.between(driver.dateOfBirth(), today).getYears();
		rejectImpossibleDetails(driver, vehicle, driverAge, today);

		String postcode = UkPostcode.normalise(driver.postcode());
		RiskProfile profile = new RiskProfile(request.coverType(), driverAge, driver.licenceHeldYears(),
				driver.noClaimsYears(), UkPostcode.area(postcode), vehicle.insuranceGroup(),
				Math.max(0, today.getYear() - vehicle.year()), request.voluntaryExcess());
		PricingResult pricing = pricingEngine.price(profile);

		Instant now = clock.instant();
		Quote quote = new Quote(now, now.plus(properties.validity()), request.coverType(), driver.dateOfBirth(),
				driver.licenceHeldYears(), driver.noClaimsYears(), postcode, vehicle.make().strip(),
				vehicle.model().strip(), vehicle.year(), vehicle.insuranceGroup(), request.voluntaryExcess());
		pricing.adjustments()
			.forEach(adjustment -> quote.addRatingFactor(new AppliedRatingFactor(adjustment.factor(),
					adjustment.multiplier(), adjustment.reason())));
		pricing.offers().forEach(offer -> quote.addInsurerQuote(toInsurerQuote(offer)));

		return QuoteResponse.from(quoteRepository.save(quote));
	}

	@Transactional(readOnly = true)
	public QuoteResponse getQuote(UUID id) {
		return quoteRepository.findById(id).map(QuoteResponse::from).orElseThrow(() -> new QuoteNotFoundException(id));
	}

	private void rejectImpossibleDetails(DriverDetails driver, VehicleDetails vehicle, int driverAge,
			LocalDate today) {
		List<String> problems = new ArrayList<>();
		if (driverAge < MINIMUM_DRIVING_AGE) {
			problems.add("Driver must be at least " + MINIMUM_DRIVING_AGE + " years old");
		}
		if (driverAge > MAXIMUM_DRIVER_AGE) {
			problems.add("Driver date of birth is not plausible");
		}
		if (driver.licenceHeldYears() > Math.max(0, driverAge - MINIMUM_DRIVING_AGE)) {
			problems.add("Licence cannot have been held since before the driver turned " + MINIMUM_DRIVING_AGE);
		}
		if (driver.noClaimsYears() > driver.licenceHeldYears()) {
			problems.add("No claims years cannot exceed years the licence has been held");
		}
		if (vehicle.year() > today.getYear() + 1) {
			problems.add("Vehicle year cannot be more than one year in the future");
		}
		if (!problems.isEmpty()) {
			throw new InvalidQuoteRequestException(problems);
		}
	}

	private static InsurerQuote toInsurerQuote(InsurerOffer offer) {
		return switch (offer) {
			case InsurerOffer.Quoted quoted -> InsurerQuote.quoted(quoted.insurer().code(), quoted.insurer().name(),
					quoted.netPremium(), quoted.insurancePremiumTax(), quoted.totalAnnualPremium());
			case InsurerOffer.Declined declined ->
				InsurerQuote.declined(declined.insurer().code(), declined.insurer().name(), declined.reason());
		};
	}

}
