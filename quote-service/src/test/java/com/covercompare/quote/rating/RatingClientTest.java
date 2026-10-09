package com.covercompare.quote.rating;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.web.client.RestClient;

import com.covercompare.quote.Contracts;
import com.covercompare.quote.domain.CoverType;
import com.covercompare.quote.http.HttpConnection;

class RatingClientTest {

	private static final String PATH = "/api/v1/ratings";

	private static final RatingRequest CONTRACT_REQUEST = new RatingRequest(CoverType.COMPREHENSIVE, 35, 10, 5, "SK",
			20, 5, 250);

	@RegisterExtension
	static WireMockExtension pricingService = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort())
		.build();

	private RatingClient client;

	@BeforeEach
	void setUp() {
		HttpConnection connection = new HttpConnection(URI.create(pricingService.baseUrl()), Duration.ofSeconds(1),
				Duration.ofSeconds(2));
		client = new RatingClient(RestClient.builder(), new RatingProperties(connection, 3));
	}

	@Test
	void sendsTheContractRequestAndReadsTheContractResponse() {
		pricingService.stubFor(post(PATH)
			.withRequestBody(equalToJson(Contracts.read("pricing/rating-request.json")))
			.willReturn(okJson(Contracts.read("pricing/rating-response.json"))));

		Rating rating = client.rate(CONTRACT_REQUEST);

		assertThat(rating.riskPremium()).isEqualByComparingTo("322.12125");
		assertThat(rating.adjustments()).hasSize(7);
		assertThat(rating.adjustments().getFirst().factor()).isNotBlank();
		assertThat(rating.adjustments()).extracting(Rating.Adjustment::multiplier)
			.allSatisfy(multiplier -> assertThat(multiplier).isPositive());
	}

	@Test
	void retriesAServerErrorAndSucceeds() {
		pricingService.stubFor(post(PATH).inScenario("flaky")
			.whenScenarioStateIs(Scenario.STARTED)
			.willReturn(serviceUnavailable())
			.willSetStateTo("recovered"));
		pricingService.stubFor(post(PATH).inScenario("flaky")
			.whenScenarioStateIs("recovered")
			.willReturn(okJson(Contracts.read("pricing/rating-response.json"))));

		Rating rating = client.rate(CONTRACT_REQUEST);

		assertThat(rating.riskPremium()).isEqualByComparingTo(new BigDecimal("322.12125"));
		pricingService.verify(2, postRequestedFor(urlEqualTo(PATH)));
	}

	@Test
	void givesUpAfterMaxAttempts() {
		pricingService.stubFor(post(PATH).willReturn(serviceUnavailable()));

		assertThatThrownBy(() -> client.rate(CONTRACT_REQUEST)).isInstanceOf(RatingUnavailableException.class);
		pricingService.verify(3, postRequestedFor(urlEqualTo(PATH)));
	}

	@Test
	void doesNotRetryARejectedRequest() {
		pricingService.stubFor(post(PATH).willReturn(aResponse().withStatus(400)));

		assertThatThrownBy(() -> client.rate(CONTRACT_REQUEST)).isInstanceOf(RatingUnavailableException.class);
		pricingService.verify(1, postRequestedFor(urlEqualTo(PATH)));
	}

	@Test
	void doesNotRetryATimeout() {
		pricingService.stubFor(post(PATH)
			.willReturn(okJson(Contracts.read("pricing/rating-response.json")).withFixedDelay(3_000)));

		assertThatThrownBy(() -> client.rate(CONTRACT_REQUEST)).isInstanceOf(RatingUnavailableException.class);
		pricingService.verify(1, postRequestedFor(urlEqualTo(PATH)));
	}

}
