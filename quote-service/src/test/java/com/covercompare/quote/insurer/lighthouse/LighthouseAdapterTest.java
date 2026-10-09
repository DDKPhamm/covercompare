package com.covercompare.quote.insurer.lighthouse;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.web.client.RestClient;

import com.covercompare.quote.insurer.InsurerQuoteRequest;
import com.covercompare.quote.insurer.InsurerResponse;
import com.covercompare.quote.insurer.TestInsurerProperties;
import com.covercompare.quote.insurer.UnexpectedInsurerResponseException;

class LighthouseAdapterTest {

	private static final String PATH = "/lighthouse/api/quote";

	private static final InsurerQuoteRequest REQUEST = new InsurerQuoteRequest(35, 20, new BigDecimal("322.121250"));

	@RegisterExtension
	static WireMockExtension lighthouse = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort())
		.build();

	private LighthouseAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new LighthouseAdapter(RestClient.builder(),
				TestInsurerProperties.pointingAt(LighthouseAdapter.CODE, lighthouse.baseUrl()));
	}

	@Test
	void sendsSnakeCaseJsonAndConvertsPenceToPounds() {
		lighthouse.stubFor(post(PATH)
			.withRequestBody(equalToJson("""
					{ "driver_age": 35, "vehicle_group": 20, "risk_premium": 322.121250 }"""))
			.willReturn(okJson("""
					{ "status": "ACCEPTED", "premium_pence": 32212, "ipt_pence": 3865, "total_pence": 36077 }""")));

		assertThat(adapter.requestQuote(REQUEST)).isEqualTo(new InsurerResponse.Quoted(new BigDecimal("322.12"),
				new BigDecimal("38.65"), new BigDecimal("360.77")));
	}

	@Test
	void readsARejection() {
		lighthouse.stubFor(post(PATH).willReturn(okJson("""
				{ "status": "REJECTED", "message": "Vehicle group above 40" }""")));

		assertThat(adapter.requestQuote(REQUEST)).isEqualTo(new InsurerResponse.Declined("Vehicle group above 40"));
	}

	@Test
	void rejectsAnAcceptedQuoteWithNoPrice() {
		lighthouse.stubFor(post(PATH).willReturn(okJson("""
				{ "status": "ACCEPTED" }""")));

		assertThatThrownBy(() -> adapter.requestQuote(REQUEST))
			.isInstanceOf(UnexpectedInsurerResponseException.class);
	}

	@Test
	void rejectsAnUnknownStatus() {
		lighthouse.stubFor(post(PATH).willReturn(okJson("""
				{ "status": "PENDING" }""")));

		assertThatThrownBy(() -> adapter.requestQuote(REQUEST))
			.isInstanceOf(UnexpectedInsurerResponseException.class)
			.hasMessageContaining("PENDING");
	}

}
