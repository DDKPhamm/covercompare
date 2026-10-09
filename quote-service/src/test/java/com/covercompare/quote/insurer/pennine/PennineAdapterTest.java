package com.covercompare.quote.insurer.pennine;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import com.covercompare.quote.insurer.InsurerQuoteRequest;
import com.covercompare.quote.insurer.InsurerResponse;
import com.covercompare.quote.insurer.TestInsurerProperties;
import com.covercompare.quote.insurer.UnexpectedInsurerResponseException;

class PennineAdapterTest {

	private static final String PATH = "/pennine/v1/quotes";

	private static final InsurerQuoteRequest REQUEST = new InsurerQuoteRequest(35, 20, new BigDecimal("322.121250"));

	@RegisterExtension
	static WireMockExtension pennine = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

	private PennineAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new PennineAdapter(RestClient.builder(),
				TestInsurerProperties.pointingAt(PennineAdapter.CODE, pennine.baseUrl()));
	}

	@Test
	void sendsCamelCaseJsonAndReadsAQuote() {
		pennine.stubFor(post(PATH)
			.withRequestBody(equalToJson("""
					{ "driverAge": 35, "vehicleGroup": 20, "riskPremium": 322.121250 }"""))
			.willReturn(okJson("""
					{ "outcome": "QUOTED", "netPremium": 306.02, "tax": 36.72, "totalPremium": 342.74 }""")));

		assertThat(adapter.requestQuote(REQUEST)).isEqualTo(new InsurerResponse.Quoted(new BigDecimal("306.02"),
				new BigDecimal("36.72"), new BigDecimal("342.74")));
	}

	@Test
	void readsADecline() {
		pennine.stubFor(post(PATH).willReturn(okJson("""
				{ "outcome": "DECLINED", "declineReason": "Driver must be at least 25" }""")));

		assertThat(adapter.requestQuote(REQUEST))
			.isEqualTo(new InsurerResponse.Declined("Driver must be at least 25"));
	}

	@Test
	void rejectsAnUnknownOutcome() {
		pennine.stubFor(post(PATH).willReturn(okJson("""
				{ "outcome": "MAYBE" }""")));

		assertThatThrownBy(() -> adapter.requestQuote(REQUEST))
			.isInstanceOf(UnexpectedInsurerResponseException.class)
			.hasMessageContaining("MAYBE");
	}

	@Test
	void surfacesServerErrorsForThePanelToRetry() {
		pennine.stubFor(post(PATH).willReturn(serviceUnavailable()));

		assertThatThrownBy(() -> adapter.requestQuote(REQUEST)).isInstanceOf(HttpServerErrorException.class);
	}

	@Test
	void identifiesItself() {
		assertThat(adapter.code()).isEqualTo("pennine");
		assertThat(adapter.displayName()).isEqualTo("Pennine Mutual");
	}

}
