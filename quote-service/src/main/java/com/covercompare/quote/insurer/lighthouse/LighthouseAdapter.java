package com.covercompare.quote.insurer.lighthouse;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.covercompare.quote.http.RestClients;
import com.covercompare.quote.insurer.InsurerAdapter;
import com.covercompare.quote.insurer.InsurerPanelProperties;
import com.covercompare.quote.insurer.InsurerQuoteRequest;
import com.covercompare.quote.insurer.InsurerResponse;
import com.covercompare.quote.insurer.UnexpectedInsurerResponseException;

/**
 * Lighthouse Direct: snake_case JSON, prices in whole pence, ACCEPTED or REJECTED.
 */
@Component
class LighthouseAdapter implements InsurerAdapter {

	static final String CODE = "lighthouse";

	private final RestClient restClient;

	LighthouseAdapter(RestClient.Builder builder, InsurerPanelProperties properties) {
		this.restClient = RestClients.create(builder, properties.connection(CODE));
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public String displayName() {
		return "Lighthouse Direct";
	}

	@Override
	public InsurerResponse requestQuote(InsurerQuoteRequest request) {
		LighthouseResponse response = restClient.post()
			.uri("/lighthouse/api/quote")
			.contentType(MediaType.APPLICATION_JSON)
			.body(new LighthouseRequest(request.driverAge(), request.vehicleInsuranceGroup(), request.riskPremium()))
			.retrieve()
			.body(LighthouseResponse.class);
		if (response == null || response.status() == null) {
			throw new UnexpectedInsurerResponseException(CODE, "empty response");
		}
		return switch (response.status()) {
			case "ACCEPTED" -> new InsurerResponse.Quoted(pounds(response.premiumPence()), pounds(response.iptPence()),
					pounds(response.totalPence()));
			case "REJECTED" -> new InsurerResponse.Declined(response.message());
			default -> throw new UnexpectedInsurerResponseException(CODE, "unknown status " + response.status());
		};
	}

	private static BigDecimal pounds(Long pence) {
		if (pence == null) {
			throw new UnexpectedInsurerResponseException(CODE, "accepted quote is missing a price");
		}
		return BigDecimal.valueOf(pence, 2);
	}

	record LighthouseRequest(
			@JsonProperty("driver_age") int driverAge,
			@JsonProperty("vehicle_group") int vehicleGroup,
			@JsonProperty("risk_premium") BigDecimal riskPremium) {
	}

	record LighthouseResponse(
			String status,
			@JsonProperty("premium_pence") Long premiumPence,
			@JsonProperty("ipt_pence") Long iptPence,
			@JsonProperty("total_pence") Long totalPence,
			String message) {
	}

}
