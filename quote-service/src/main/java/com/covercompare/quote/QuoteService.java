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

import com.covercompare.quote.api.DriverDetails;
import com.covercompare.quote.api.QuoteRequest;
import com.covercompare.quote.api.QuoteResponse;
import com.covercompare.quote.api.VehicleDetails;
import com.covercompare.quote.domain.AppliedRatingFactor;
import com.covercompare.quote.domain.InsurerQuote;
import com.covercompare.quote.domain.Quote;
import com.covercompare.quote.domain.QuoteRepository;
import com.covercompare.quote.insurer.InsurerOutcome;
import com.covercompare.quote.insurer.InsurerPanel;
import com.covercompare.quote.insurer.InsurerQuoteRequest;
import com.covercompare.quote.rating.Rating;
import com.covercompare.quote.rating.RatingClient;
import com.covercompare.quote.rating.RatingRequest;

@Service
public class QuoteService {

	static final int MINIMUM_DRIVING_AGE = 17;

	private static final int MAXIMUM_DRIVER_AGE = 110;

	private final RatingClient ratingClient;

	private final InsurerPanel insurerPanel;

	private final QuoteRepository quoteRepository;

	private final QuoteProperties properties;

	private final Clock clock;

	public QuoteService(RatingClient ratingClient, InsurerPanel insurerPanel, QuoteRepository quoteRepository,
			QuoteProperties properties, Clock clock) {
		this.ratingClient = ratingClient;
		this.insurerPanel = insurerPanel;
		this.quoteRepository = quoteRepository;
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * Deliberately not {@code @Transactional}: the remote calls take up to a few seconds, and holding
	 * a database connection open while waiting on other services would exhaust the connection pool
	 * under load. Only the final save, which is transactional on its own, touches the database.
	 */
	public QuoteResponse createQuote(QuoteRequest request) {
		LocalDate today = LocalDate.now(clock);
		DriverDetails driver = request.driver();
		VehicleDetails vehicle = request.vehicle();
		int driverAge = Period.between(driver.dateOfBirth(), today).getYears();
		rejectImpossibleDetails(driver, vehicle, driverAge, today);

		String postcode = UkPostcode.normalise(driver.postcode());
		Rating rating = ratingClient.rate(new RatingRequest(request.coverType(), driverAge,
				driver.licenceHeldYears(), driver.noClaimsYears(), UkPostcode.area(postcode),
				vehicle.insuranceGroup(), Math.max(0, today.getYear() - vehicle.year()), request.voluntaryExcess()));
		List<InsurerOutcome> outcomes = insurerPanel
			.requestQuotes(new InsurerQuoteRequest(driverAge, vehicle.insuranceGroup(), rating.riskPremium()));

		Instant now = clock.instant();
		Quote quote = new Quote(now, now.plus(properties.validity()), request.coverType(), driver.dateOfBirth(),
				driver.licenceHeldYears(), driver.noClaimsYears(), postcode, vehicle.make().strip(),
				vehicle.model().strip(), vehicle.year(), vehicle.insuranceGroup(), request.voluntaryExcess());
		rating.adjustments()
			.forEach(adjustment -> quote.addRatingFactor(new AppliedRatingFactor(adjustment.factor(),
					adjustment.multiplier(), adjustment.reason())));
		outcomes.forEach(outcome -> quote.addInsurerQuote(toInsurerQuote(outcome)));

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

	private static InsurerQuote toInsurerQuote(InsurerOutcome outcome) {
		return switch (outcome) {
			case InsurerOutcome.Quoted quoted -> InsurerQuote.quoted(quoted.insurerCode(), quoted.insurerName(),
					quoted.netPremium(), quoted.insurancePremiumTax(), quoted.totalAnnualPremium());
			case InsurerOutcome.Declined declined ->
				InsurerQuote.declined(declined.insurerCode(), declined.insurerName(), declined.reason());
			case InsurerOutcome.Unavailable unavailable ->
				InsurerQuote.unavailable(unavailable.insurerCode(), unavailable.insurerName(), unavailable.reason());
		};
	}

}
