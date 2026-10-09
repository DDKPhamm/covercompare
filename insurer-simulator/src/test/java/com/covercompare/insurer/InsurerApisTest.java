package com.covercompare.insurer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Checks each simulated insurer's wire format using the real configuration, with no faults.
 */
@SpringBootTest
@AutoConfigureMockMvc
class InsurerApisTest {

	@Autowired
	private MockMvcTester mvc;

	@Nested
	class Pennine {

		@Test
		void quotesInPoundsWithTheOutcomeInTheBody() {
			var result = post("/pennine/v1/quotes", """
					{ "driverAge": 35, "vehicleGroup": 20, "riskPremium": 322.121250 }""");

			assertThat(result).hasStatus(HttpStatus.OK).bodyJson().isEqualTo("""
					{ "outcome": "QUOTED", "netPremium": 306.02, "tax": 36.72, "totalPremium": 342.74 }""");
		}

		@Test
		void declinesWithAReasonAndStill200() {
			var result = post("/pennine/v1/quotes", """
					{ "driverAge": 22, "vehicleGroup": 20, "riskPremium": 322.121250 }""");

			assertThat(result).hasStatus(HttpStatus.OK).bodyJson().isEqualTo("""
					{ "outcome": "DECLINED", "declineReason": "Driver must be at least 25" }""");
		}

		@Test
		void rejectsInvalidRequests() {
			assertThat(post("/pennine/v1/quotes", """
					{ "driverAge": 35, "vehicleGroup": 99, "riskPremium": 322.12 }""")).hasStatus(HttpStatus.BAD_REQUEST);
		}

	}

	@Nested
	class Lighthouse {

		@Test
		void quotesInWholePenceWithSnakeCaseFields() {
			var result = post("/lighthouse/api/quote", """
					{ "driver_age": 35, "vehicle_group": 20, "risk_premium": 322.121250 }""");

			assertThat(result).hasStatus(HttpStatus.OK).bodyJson().isEqualTo("""
					{ "status": "ACCEPTED", "premium_pence": 32212, "ipt_pence": 3865, "total_pence": 36077 }""");
		}

		@Test
		void rejectsVehiclesAboveGroup40() {
			var result = post("/lighthouse/api/quote", """
					{ "driver_age": 35, "vehicle_group": 41, "risk_premium": 322.121250 }""");

			assertThat(result).hasStatus(HttpStatus.OK).bodyJson().isEqualTo("""
					{ "status": "REJECTED", "message": "Vehicle insurance group must be 40 or below" }""");
		}

	}

	@Nested
	class Redbrick {

		@Test
		void quotesInXml() {
			var result = postXml("""
					<QuoteRequest><DriverAge>35</DriverAge><VehicleGroup>20</VehicleGroup>\
					<RiskPremium>322.121250</RiskPremium></QuoteRequest>""");

			assertThat(result).hasStatus(HttpStatus.OK).hasContentTypeCompatibleWith(MediaType.APPLICATION_XML);
			assertThat(result).bodyText()
				.contains("<Result>QUOTE</Result><Net>360.78</Net><Ipt>43.29</Ipt><Gross>404.07</Gross>");
		}

		@Test
		void signalsADeclineWithHttp422() {
			var result = postXml("""
					<QuoteRequest><DriverAge>35</DriverAge><VehicleGroup>48</VehicleGroup>\
					<RiskPremium>322.12</RiskPremium></QuoteRequest>""");

			assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
			assertThat(result).bodyText()
				.contains("<Result>DECLINE</Result><Reason>Vehicle insurance group must be 45 or below</Reason>");
		}

		@Test
		void rejectsOutOfRangeValuesWithHttp400() {
			var result = postXml("""
					<QuoteRequest><DriverAge>35</DriverAge><VehicleGroup>51</VehicleGroup>\
					<RiskPremium>322.12</RiskPremium></QuoteRequest>""");

			assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
			assertThat(result).bodyText().contains("VehicleGroup must be between 1 and 50");
		}

		@Test
		void rejectsXmlExternalEntityAttacks() {
			var result = postXml("""
					<?xml version="1.0"?>
					<!DOCTYPE QuoteRequest [<!ENTITY secret SYSTEM "file:///etc/passwd">]>
					<QuoteRequest><DriverAge>&secret;</DriverAge><VehicleGroup>20</VehicleGroup>\
					<RiskPremium>322.12</RiskPremium></QuoteRequest>""");

			assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
			assertThat(result).bodyText().contains("Request is not well-formed XML").doesNotContain("root:");
		}

		@Test
		void rejectsMalformedXml() {
			assertThat(postXml("<QuoteRequest><DriverAge>35")).hasStatus(HttpStatus.BAD_REQUEST);
		}

	}

	@Test
	void reportsHealthyForContainerHealthChecks() {
		assertThat(mvc.get().uri("/actuator/health/readiness")).hasStatus(HttpStatus.OK);
	}

	private org.springframework.test.web.servlet.assertj.MvcTestResult post(String uri, String json) {
		return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
	}

	private org.springframework.test.web.servlet.assertj.MvcTestResult postXml(String xml) {
		return mvc.post()
			.uri("/redbrick/quote")
			.contentType(MediaType.APPLICATION_XML)
			.accept(MediaType.APPLICATION_XML)
			.content(xml)
			.exchange();
	}

}
