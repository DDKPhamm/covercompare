package com.covercompare.quote.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.covercompare.pricing.CoverType;
import com.covercompare.quote.InvalidQuoteRequestException;
import com.covercompare.quote.QuoteNotFoundException;
import com.covercompare.quote.QuoteService;
import com.covercompare.quote.domain.InsurerQuoteStatus;

@WebMvcTest(QuoteController.class)
class QuoteControllerTest {

	private static final String VALID_REQUEST = """
			{
			  "driver": {
			    "dateOfBirth": "1991-03-15",
			    "licenceHeldYears": 10,
			    "noClaimsYears": 5,
			    "postcode": "SK1 3AB"
			  },
			  "vehicle": { "make": "Ford", "model": "Fiesta", "year": 2021, "insuranceGroup": 20 },
			  "coverType": "COMPREHENSIVE",
			  "voluntaryExcess": 250
			}
			""";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private QuoteService quoteService;

	@Test
	void createReturns201WithLocationOfTheNewQuote() {
		UUID id = UUID.fromString("7d0f7e4e-2f7a-4c0e-9a51-1f5c6a3b9e10");
		given(quoteService.createQuote(any())).willReturn(sampleResponse(id));

		var result = mvc.post().uri("/api/v1/quotes").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST);

		assertThat(result).hasStatus(HttpStatus.CREATED)
			.headers()
			.hasValue("Location", "http://localhost/api/v1/quotes/" + id);
		assertThat(result).bodyJson().extractingPath("$.insurerQuotes[0].totalAnnualPremium").isEqualTo(342.74);
	}

	@Test
	void omitsPriceFieldsForDeclinedQuotes() {
		given(quoteService.createQuote(any())).willReturn(sampleResponse(UUID.randomUUID()));

		var result = mvc.post().uri("/api/v1/quotes").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST);

		assertThat(result).bodyJson().extractingPath("$.insurerQuotes[1]").asMap().doesNotContainKey("totalAnnualPremium")
			.containsEntry("declineReason", "Driver must be at least 25");
	}

	@Test
	void createReturns400ListingEachInvalidField() {
		String invalid = VALID_REQUEST.replace("\"SK1 3AB\"", "\"NOT A POSTCODE\"")
			.replace("\"insuranceGroup\": 20", "\"insuranceGroup\": 51");

		var result = mvc.post().uri("/api/v1/quotes").contentType(MediaType.APPLICATION_JSON).content(invalid);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errors").asMap()
			.containsEntry("driver.postcode", "must be a valid UK postcode")
			.containsEntry("vehicle.insuranceGroup", "must be less than or equal to 50");
	}

	@Test
	void createReturns400ForUnknownCoverType() {
		String invalid = VALID_REQUEST.replace("COMPREHENSIVE", "EVERYTHING");

		var result = mvc.post().uri("/api/v1/quotes").contentType(MediaType.APPLICATION_JSON).content(invalid);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void createReturns422WhenDetailsAreImpossible() {
		given(quoteService.createQuote(any()))
			.willThrow(new InvalidQuoteRequestException(List.of("Driver must be at least 17 years old")));

		var result = mvc.post().uri("/api/v1/quotes").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST);

		assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(result).bodyJson().extractingPath("$.problems[0]").isEqualTo("Driver must be at least 17 years old");
	}

	@Test
	void getReturns404ProblemForUnknownQuote() {
		UUID id = UUID.randomUUID();
		given(quoteService.getQuote(id)).willThrow(new QuoteNotFoundException(id));

		var result = mvc.get().uri("/api/v1/quotes/{id}", id);

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Quote not found");
	}

	@Test
	void getReturns400ForMalformedId() {
		var result = mvc.get().uri("/api/v1/quotes/not-a-uuid");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
	}

	private static QuoteResponse sampleResponse(UUID id) {
		Instant now = Instant.parse("2026-10-04T12:00:00Z");
		return new QuoteResponse(id, now, now.plusSeconds(30 * 24 * 3600), CoverType.COMPREHENSIVE,
				List.of(new QuoteResponse.RatingFactorResponse("Driver age", new BigDecimal("1.00"), "Driver aged 35")),
				List.of(new QuoteResponse.InsurerQuoteResponse("PENNINE", "Pennine Mutual", InsurerQuoteStatus.QUOTED,
						new BigDecimal("306.02"), new BigDecimal("36.72"), new BigDecimal("342.74"), null),
						new QuoteResponse.InsurerQuoteResponse("YOUNGONLY", "Young Only", InsurerQuoteStatus.DECLINED,
								null, null, null, "Driver must be at least 25")));
	}

}
