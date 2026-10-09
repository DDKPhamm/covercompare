package com.covercompare;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.covercompare.quote.Contracts;
import com.covercompare.quote.domain.QuoteRepository;

/**
 * Runs the whole quote-service against a real PostgreSQL database in Docker, with WireMock standing
 * in for pricing-service and the insurers. Proves the Flyway migrations, JPA mappings, HTTP clients
 * and API work together. Skipped automatically when Docker is not available; always runs in CI.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class QuoteApiIT {

	private static final String REQUEST = """
			{
			  "driver": {
			    "dateOfBirth": "1991-03-15",
			    "licenceHeldYears": 10,
			    "noClaimsYears": 5,
			    "postcode": "sk1 3ab"
			  },
			  "vehicle": { "make": "Ford", "model": "Fiesta", "year": 2021, "insuranceGroup": 20 },
			  "coverType": "COMPREHENSIVE",
			  "voluntaryExcess": 250
			}
			""";

	@RegisterExtension
	static WireMockExtension downstream = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort())
		.build();

	@DynamicPropertySource
	static void pointClientsAtWireMock(DynamicPropertyRegistry registry) {
		registry.add("covercompare.rating.connection.base-url", downstream::baseUrl);
		for (String insurer : new String[] { "pennine", "lighthouse", "redbrick" }) {
			registry.add("covercompare.insurers.connections." + insurer + ".base-url", downstream::baseUrl);
		}
	}

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private QuoteRepository quoteRepository;

	@TestBean
	private Clock clock;

	static Clock clock() {
		return Clock.fixed(Instant.parse("2026-10-04T12:00:00Z"), ZoneOffset.UTC);
	}

	@Test
	void storesAQuotedADeclinedAndAnUnavailableInsurer() throws Exception {
		downstream.stubFor(post("/api/v1/ratings")
			.withRequestBody(equalToJson(Contracts.read("pricing/rating-request.json")))
			.willReturn(okJson(Contracts.read("pricing/rating-response.json"))));
		downstream.stubFor(post("/pennine/v1/quotes").willReturn(okJson("""
				{ "outcome": "QUOTED", "netPremium": 306.02, "tax": 36.72, "totalPremium": 342.74 }""")));
		downstream.stubFor(post("/lighthouse/api/quote").willReturn(okJson("""
				{ "status": "REJECTED", "message": "Vehicle group above 40" }""")));
		downstream.stubFor(post("/redbrick/quote").willReturn(serviceUnavailable()));

		MvcTestResult created = mvc.post()
			.uri("/api/v1/quotes")
			.contentType(MediaType.APPLICATION_JSON)
			.content(REQUEST)
			.exchange();
		assertThat(created).hasStatus(HttpStatus.CREATED);
		String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		MvcTestResult fetched = mvc.get().uri("/api/v1/quotes/{id}", id).exchange();

		assertThat(fetched).hasStatus(HttpStatus.OK);
		assertThat(fetched).bodyJson().extractingPath("$.ratingFactors").asArray().hasSize(7);
		assertThat(fetched).bodyJson()
			.extractingPath("$.insurerQuotes[*].status")
			.asArray()
			.containsExactly("QUOTED", "DECLINED", "UNAVAILABLE");
		assertThat(fetched).bodyJson().extractingPath("$.insurerQuotes[0].totalAnnualPremium").isEqualTo(342.74);
		assertThat(fetched).bodyJson()
			.extractingPath("$.insurerQuotes[1].declineReason")
			.isEqualTo("Vehicle group above 40");
		assertThat(fetched).bodyJson()
			.extractingPath("$.insurerQuotes[2].unavailableReason")
			.isEqualTo("Insurer is temporarily unavailable");
	}

	@Test
	void returns503AndSavesNothingWhenPricingIsDown() {
		downstream.stubFor(post("/api/v1/ratings").willReturn(aResponse().withStatus(500)));
		long before = quoteRepository.count();

		MvcTestResult result = mvc.post()
			.uri("/api/v1/quotes")
			.contentType(MediaType.APPLICATION_JSON)
			.content(REQUEST)
			.exchange();

		assertThat(result).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(quoteRepository.count()).isEqualTo(before);
	}

	@Test
	void reportsHealthy() {
		assertThat(mvc.get().uri("/actuator/health")).hasStatus(HttpStatus.OK)
			.bodyJson()
			.extractingPath("$.status")
			.isEqualTo("UP");
	}

}
