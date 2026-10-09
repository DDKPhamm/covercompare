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

import com.covercompare.quote.api.DriverDetails;
import com.covercompare.quote.api.QuoteResponse;
import com.covercompare.quote.api.QuoteResponse.InsurerQuoteResponse;
import com.covercompare.quote.api.VehicleDetails;
import com.covercompare.quote.domain.CoverType;
import com.covercompare.quote.domain.InsurerQuoteStatus;
import com.covercompare.quote.domain.Quote;
import com.covercompare.quote.domain.QuoteRepository;
import com.covercompare.quote.insurer.InsurerOutcome;
import com.covercompare.quote.insurer.InsurerPanel;
import com.covercompare.quote.insurer.InsurerQuoteRequest;
import com.covercompare.quote.rating.Rating;
import com.covercompare.quote.rating.RatingClient;
import com.covercompare.quote.rating.RatingRequest;
import com.covercompare.quote.rating.RatingUnavailableException;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-04T12:00:00Z");

	private static final BigDecimal RISK_PREMIUM = new BigDecimal("322.121250");

	@Mock
	private RatingClient ratingClient;

	@Mock
	private InsurerPanel insurerPanel;

	@Mock
	private QuoteRepository quoteRepository;

	private QuoteService quoteService;

	@BeforeEach
	void setUp() {
		quoteService = new QuoteService(ratingClient, insurerPanel, quoteRepository,
				new QuoteProperties(Duration.ofDays(30)), Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void sendsOnlyDerivedRiskDetailsToPricingService() {
		givenRating();
		givenPanelReturns(new InsurerOutcome.Declined("acme", "Acme Insurance", "No"));
		givenSaveReturnsTheQuote();

		quoteService.createQuote(QuoteRequests.standard());

		then(ratingClient).should()
			.rate(new RatingRequest(CoverType.COMPREHENSIVE, 35, 10, 5, "SK", 20, 5, 250));
	}

	@Test
	void asksTheInsurersToPriceTheRatedRisk() {
		givenRating();
		givenPanelReturns(new InsurerOutcome.Declined("acme", "Acme Insurance", "No"));
		givenSaveReturnsTheQuote();

		quoteService.createQuote(QuoteRequests.standard());

		then(insurerPanel).should().requestQuotes(new InsurerQuoteRequest(35, 20, RISK_PREMIUM));
	}

	@Test
	void savesEveryInsurerOutcomeInPanelOrder() {
		givenRating();
		givenPanelReturns(
				new InsurerOutcome.Quoted("acme", "Acme Insurance", new BigDecimal("100.00"), new BigDecimal("12.00"),
						new BigDecimal("112.00")),
				new InsurerOutcome.Declined("picky", "Picky Ltd", "Too young"),
				new InsurerOutcome.Unavailable("slow", "Slow plc", "Insurer did not respond in time"));
		givenSaveReturnsTheQuote();

		QuoteResponse response = quoteService.createQuote(QuoteRequests.standard());

		assertThat(response.createdAt()).isEqualTo(NOW);
		assertThat(response.validUntil()).isEqualTo(NOW.plus(Duration.ofDays(30)));
		assertThat(response.ratingFactors()).singleElement()
			.extracting(QuoteResponse.RatingFactorResponse::factor)
			.isEqualTo("Driver age");
		assertThat(response.insurerQuotes())
			.extracting(InsurerQuoteResponse::insurerCode, InsurerQuoteResponse::status,
					InsurerQuoteResponse::totalAnnualPremium, InsurerQuoteResponse::declineReason,
					InsurerQuoteResponse::unavailableReason)
			.containsExactly(tuple("acme", InsurerQuoteStatus.QUOTED, new BigDecimal("112.00"), null, null),
					tuple("picky", InsurerQuoteStatus.DECLINED, null, "Too young", null),
					tuple("slow", InsurerQuoteStatus.UNAVAILABLE, null, null, "Insurer did not respond in time"));
	}

	@Test
	void storesTheNormalisedPostcode() {
		givenRating();
		givenPanelReturns(new InsurerOutcome.Declined("acme", "Acme Insurance", "No"));
		givenSaveReturnsTheQuote();

		quoteService.createQuote(QuoteRequests.standard());

		ArgumentCaptor<Quote> saved = ArgumentCaptor.forClass(Quote.class);
		then(quoteRepository).should().save(saved.capture());
		assertThat(saved.getValue().getPostcode()).isEqualTo("SK1 3AB");
	}

	@Test
	void savesNothingAndAsksNoInsurerWhenPricingIsUnavailable() {
		given(ratingClient.rate(any())).willThrow(new RatingUnavailableException(new RuntimeException("down")));

		assertThatThrownBy(() -> quoteService.createQuote(QuoteRequests.standard()))
			.isInstanceOf(RatingUnavailableException.class);
		then(insurerPanel).shouldHaveNoInteractions();
		then(quoteRepository).shouldHaveNoInteractions();
	}

	@Test
	void rejectsDriversUnderSeventeenWithoutCallingOtherServices() {
		DriverDetails sixteenYearOld = new DriverDetails(LocalDate.of(2010, 1, 1), 0, 0, "SK1 3AB");

		assertThatThrownBy(() -> quoteService.createQuote(QuoteRequests.withDriver(sixteenYearOld)))
			.isInstanceOfSatisfying(InvalidQuoteRequestException.class, ex -> assertThat(ex.getProblems())
				.containsExactly("Driver must be at least 17 years old"));
		then(ratingClient).shouldHaveNoInteractions();
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

	private void givenRating() {
		given(ratingClient.rate(any())).willReturn(new Rating(
				List.of(new Rating.Adjustment("Driver age", new BigDecimal("1.00"), "Driver aged 35")), RISK_PREMIUM));
	}

	private void givenPanelReturns(InsurerOutcome... outcomes) {
		given(insurerPanel.requestQuotes(any())).willReturn(List.of(outcomes));
	}

	private void givenSaveReturnsTheQuote() {
		given(quoteRepository.save(any(Quote.class))).willAnswer(invocation -> invocation.getArgument(0));
	}

}
