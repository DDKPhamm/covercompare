package com.covercompare;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;

/**
 * Exercises the whole application against a real PostgreSQL database in Docker, proving the Flyway
 * migration, JPA mappings and HTTP layer work together. Skipped automatically when Docker is not
 * available; always runs in CI.
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

	@Autowired
	private MockMvcTester mvc;

	@TestBean
	private Clock clock;

	static Clock clock() {
		return Clock.fixed(Instant.parse("2026-10-04T12:00:00Z"), ZoneOffset.UTC);
	}

	@Test
	void createdQuoteCanBeReadBackFromTheDatabase() throws Exception {
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
			.extractingPath("$.insurerQuotes[*].insurerCode")
			.asArray()
			.containsExactly("PENNINE", "LIGHTHOUSE", "REDBRICK");
		assertThat(fetched).bodyJson().extractingPath("$.insurerQuotes[0].totalAnnualPremium").isEqualTo(342.74);
	}

	@Test
	void reportsHealthy() {
		assertThat(mvc.get().uri("/actuator/health")).hasStatus(HttpStatus.OK)
			.bodyJson()
			.extractingPath("$.status")
			.isEqualTo("UP");
	}

}
