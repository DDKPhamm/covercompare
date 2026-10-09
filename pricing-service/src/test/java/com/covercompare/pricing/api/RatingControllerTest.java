package com.covercompare.pricing.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
class RatingControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void rejectsInvalidRiskProfilesWithFieldErrors() {
		String invalid = """
				{ "coverType": "COMPREHENSIVE", "driverAge": 16, "licenceHeldYears": 0, "noClaimsYears": 0,
				  "postcodeArea": "sk1", "vehicleInsuranceGroup": 20, "vehicleAgeYears": 5, "voluntaryExcess": 0 }
				""";

		var result = mvc.post().uri("/api/v1/ratings").contentType(MediaType.APPLICATION_JSON).content(invalid);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson()
			.extractingPath("$.errors")
			.asMap()
			.containsOnlyKeys("driverAge", "postcodeArea");
	}

	@Test
	void reportsHealthyForContainerHealthChecks() {
		assertThat(mvc.get().uri("/actuator/health/readiness")).hasStatus(HttpStatus.OK);
	}

}
