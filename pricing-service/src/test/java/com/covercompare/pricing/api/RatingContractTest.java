package com.covercompare.pricing.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Provider side of the pricing contract: given the shared example request, the real rating engine
 * must return exactly the shared example response that quote-service is tested against.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RatingContractTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void honoursTheRatingContract() throws IOException {
		var result = mvc.post()
			.uri("/api/v1/ratings")
			.contentType(MediaType.APPLICATION_JSON)
			.content(contract("rating-request.json"));

		assertThat(result).hasStatus(HttpStatus.OK)
			.bodyJson()
			.isEqualTo(contract("rating-response.json"), JsonCompareMode.STRICT);
	}

	private static String contract(String name) throws IOException {
		return new ClassPathResource("contracts/pricing/" + name).getContentAsString(StandardCharsets.UTF_8);
	}

}
