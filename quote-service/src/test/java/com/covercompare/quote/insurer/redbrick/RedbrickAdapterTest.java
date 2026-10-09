package com.covercompare.quote.insurer.redbrick;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToXml;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
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

class RedbrickAdapterTest {

	private static final String PATH = "/redbrick/quote";

	private static final InsurerQuoteRequest REQUEST = new InsurerQuoteRequest(35, 20, new BigDecimal("322.121250"));

	@RegisterExtension
	static WireMockExtension redbrick = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

	private RedbrickAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new RedbrickAdapter(RestClient.builder(),
				TestInsurerProperties.pointingAt(RedbrickAdapter.CODE, redbrick.baseUrl()));
	}

	@Test
	void sendsXmlAndReadsAQuote() {
		redbrick.stubFor(post(PATH)
			.withRequestBody(equalToXml("""
					<QuoteRequest><DriverAge>35</DriverAge><VehicleGroup>20</VehicleGroup>\
					<RiskPremium>322.121250</RiskPremium></QuoteRequest>"""))
			.willReturn(xml(200, """
					<QuoteResponse><Result>QUOTE</Result><Net>360.78</Net><Ipt>43.29</Ipt><Gross>404.07</Gross>\
					</QuoteResponse>""")));

		assertThat(adapter.requestQuote(REQUEST)).isEqualTo(new InsurerResponse.Quoted(new BigDecimal("360.78"),
				new BigDecimal("43.29"), new BigDecimal("404.07")));
	}

	@Test
	void treats422AsADecline() {
		redbrick.stubFor(post(PATH).willReturn(xml(422, """
				<QuoteResponse><Result>DECLINE</Result><Reason>Vehicle group above 45</Reason></QuoteResponse>""")));

		assertThat(adapter.requestQuote(REQUEST)).isEqualTo(new InsurerResponse.Declined("Vehicle group above 45"));
	}

	@Test
	void surfacesServerErrorsForThePanelToRetry() {
		redbrick.stubFor(post(PATH).willReturn(xml(503, "<Error/>")));

		assertThatThrownBy(() -> adapter.requestQuote(REQUEST)).isInstanceOf(HttpServerErrorException.class);
	}

	@Test
	void treatsOtherClientErrorsAsUnexpected() {
		redbrick.stubFor(post(PATH).willReturn(xml(400, """
				<QuoteResponse><Result>ERROR</Result><Reason>Bad request</Reason></QuoteResponse>""")));

		assertThatThrownBy(() -> adapter.requestQuote(REQUEST))
			.isInstanceOf(UnexpectedInsurerResponseException.class)
			.hasMessageContaining("400");
	}

	@Test
	void rejectsAQuoteWithAMissingPrice() {
		redbrick.stubFor(post(PATH).willReturn(xml(200, """
				<QuoteResponse><Result>QUOTE</Result><Net>360.78</Net></QuoteResponse>""")));

		assertThatThrownBy(() -> adapter.requestQuote(REQUEST))
			.isInstanceOf(UnexpectedInsurerResponseException.class)
			.hasMessageContaining("Ipt");
	}

	@Test
	void refusesResponsesContainingADoctype() {
		redbrick.stubFor(post(PATH).willReturn(xml(200, """
				<?xml version="1.0"?>
				<!DOCTYPE QuoteResponse [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
				<QuoteResponse><Result>QUOTE</Result><Net>&xxe;</Net><Ipt>1</Ipt><Gross>1</Gross></QuoteResponse>""")));

		assertThatThrownBy(() -> adapter.requestQuote(REQUEST))
			.isInstanceOf(UnexpectedInsurerResponseException.class)
			.hasMessageContaining("not well-formed");
	}

	private static ResponseDefinitionBuilder xml(int status, String body) {
		return aResponse().withStatus(status).withHeader("Content-Type", "application/xml").withBody(body);
	}

}
