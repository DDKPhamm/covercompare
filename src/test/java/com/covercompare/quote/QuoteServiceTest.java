package com.covercompare.quote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.covercompare.pricing.CoverType;
import com.covercompare.pricing.Insurer;
import com.covercompare.pricing.InsurerOffer;
import com.covercompare.pricing.PricingEngine;
import com.covercompare.pricing.PricingResult;
import com.covercompare.pricing.RatingAdjustment;
import com.covercompare.pricing.RiskProfile;
import com.covercompare.quote.api.DriverDetails;
import com.covercompare.quote.api.QuoteResponse;
import com.covercompare.quote.api.VehicleDetails;
import com.covercompare.quote.domain.InsurerQuoteStatus;
import com.covercompare.quote.domain.Quote;
import com.covercompare.quote.domain.QuoteRepository;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-04T12:00:00Z");

	private static final Insurer ACME = new Insurer("ACME", "Acme Insurance", BigDecimal.ONE, 17, 50);

	@Mock
	private PricingEngine pricingEngine;

	@Mock
	private QuoteRepository quoteRepository;

	private QuoteService quoteService;

	@BeforeEach
	void setUp() {
		quoteService = new QuoteService(pricingEngine, quoteRepository, new QuoteProperties(Duration.ofDays(30)),
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void buildsTheRiskProfileFromTheRequest() {
		givenPricingReturns(new InsurerOffer.Declined(ACME, "No"));
		given(quoteRepository.save(any(Quote.class))).willAnswer(invocation -> invocation.getArgument(0));

		quoteService.createQuote(QuoteRequests.standard());

		ArgumentCaptor<RiskProfile> profile = ArgumentCaptor.forClass(RiskProfile.class);
		then(pricingEngine).should().price(profile.capture());
		assertThat(profile.getValue())
			.isEqualTo(new RiskProfile(CoverType.COMPREHENSIVE, 35, 10, 5, "SK", 20, 5, 250));
	}

	@Test
	void savesTheQuoteWithEveryInsurerResponseInOrder() {
		givenPricingReturns(
				new InsurerOffer.Quoted(ACME, new BigDecimal("100.00"), new BigDecimal("12.00"),
						new BigDecimal("112.00")),
				new InsurerOffer.Declined(new Insurer("PICKY", "Picky Ltd", BigDecimal.ONE, 25, 50), "Too young"));
		given(quoteRepository.save(any(Quote.class))).willAnswer(invocation -> invocation.getArgument(0));

		QuoteResponse response = quoteService.createQuote(QuoteRequests.standard());

		assertThat(response.createdAt()).isEqualTo(NOW);
		assertThat(response.validUntil()).isEqualTo(NOW.plus(Duration.ofDays(30)));
		assertThat(response.ratingFactors()).singleElement()
			.extracting(QuoteResponse.RatingFactorResponse::factor)
			.isEqualTo("Driver age");
		assertThat(response.insurerQuotes()).extracting(QuoteResponse.InsurerQuoteResponse::insurerCode,
				QuoteResponse.InsurerQuoteResponse::status, QuoteResponse.InsurerQuoteResponse::totalAnnualPremium)
			.containsExactly(tuple("ACME", InsurerQuoteStatus.QUOTED, new BigDecimal("112.00")),
					tuple("PICKY", InsurerQuoteStatus.DECLINED, null));
	}

	@Test
	void storesTheNormalisedPostcode() {
		givenPricingReturns(new InsurerOffer.Declined(ACME, "No"));
		given(quoteRepository.save(any(Quote.class))).willAnswer(invocation -> invocation.getArgument(0));

		quoteService.createQuote(QuoteRequests.standard());

		ArgumentCaptor<Quote> saved = ArgumentCaptor.forClass(Quote.class);
		then(quoteRepository).should().save(saved.capture());
		assertThat(saved.getValue().getPostcode()).isEqualTo("SK1 3AB");
	}

	@Test
	void rejectsDriversUnderSeventeen() {
		DriverDetails sixteenYearOld = new DriverDetails(LocalDate.of(2010, 1, 1), 0, 0, "SK1 3AB");

		assertThatThrownBy(() -> quoteService.createQuote(QuoteRequests.withDriver(sixteenYearOld)))
			.isInstanceOfSatisfying(InvalidQuoteRequestException.class, ex -> assertThat(ex.getProblems())
				.containsExactly("Driver must be at least 17 years old"));
		then(quoteRepository).should(never()).save(any());
	}

	@Test
	void rejectsLicenceHeldLongerThanPossibleAndNoClaimsLongerThanLicence() {
		DriverDetails twentyYearOld = new DriverDetails(LocalDate.of(2006, 1, 1), 5, 6, "SK1 3AB");

		assertThatThrownBy(() -> quoteService.createQuote(QuoteRequests.withDriver(twentyYearOld)))
			.isInstanceOfSatisfying(InvalidQuoteRequestException.class,
					ex -> assertThat(ex.getProblems()).containsExactly(
							"Licence cannot have been held since before the driver turned 17",
							"No claims years cannot exceed years the licence has been held"));
	}

	@Test
	void rejectsImplausibleDatesOfBirthAndFutureVehicles() {
		DriverDetails ancient = new DriverDetails(LocalDate.of(1900, 1, 1), 10, 5, "SK1 3AB");
		VehicleDetails futureCar = new VehicleDetails("Ford", "Fiesta", 2030, 20);

		assertThatThrownBy(() -> quoteService.createQuote(QuoteRequests.withDriver(ancient)))
			.isInstanceOfSatisfying(InvalidQuoteRequestException.class,
					ex -> assertThat(ex.getProblems()).containsExactly("Driver date of birth is not plausible"));
		assertThatThrownBy(() -> quoteService.createQuote(QuoteRequests.withVehicle(futureCar)))
			.isInstanceOfSatisfying(InvalidQuoteRequestException.class, ex -> assertThat(ex.getProblems())
				.containsExactly("Vehicle year cannot be more than one year in the future"));
	}

	@Test
	void throwsNotFoundForUnknownQuote() {
		UUID id = UUID.randomUUID();
		given(quoteRepository.findById(id)).willReturn(Optional.empty());

		assertThatThrownBy(() -> quoteService.getQuote(id)).isInstanceOf(QuoteNotFoundException.class)
			.hasMessageContaining(id.toString());
	}

	private void givenPricingReturns(InsurerOffer... offers) {
		given(pricingEngine.price(any())).willReturn(
				new PricingResult(List.of(RatingAdjustment.of("Driver age", "1.00", "Driver aged 35")), List.of(offers)));
	}

}
