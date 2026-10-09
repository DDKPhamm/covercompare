package com.covercompare.quote.insurer.pennine;

import java.math.BigDecimal;

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
 * Pennine Mutual: camelCase JSON, prices in pounds, always HTTP 200 with the outcome in the body.
 */
@Component
class PennineAdapter implements InsurerAdapter {

	static final String CODE = "pennine";

	private final RestClient restClient;

	PennineAdapter(RestClient.Builder builder, InsurerPanelProperties properties) {
		this.restClient = RestClients.create(builder, properties.connection(CODE));
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public String displayName() {
		return "Pennine Mutual";
	}

	@Override
	public InsurerResponse requestQuote(InsurerQuoteRequest request) {
		PennineResponse response = restClient.post()
			.uri("/pennine/v1/quotes")
			.contentType(MediaType.APPLICATION_JSON)
			.body(new PennineRequest(request.driverAge(), request.vehicleInsuranceGroup(), request.riskPremium()))
			.retrieve()
			.body(PennineResponse.class);
		if (response == null || response.outcome() == null) {
			throw new UnexpectedInsurerResponseException(CODE, "empty response");
		}
		return switch (response.outcome()) {
			case "QUOTED" -> new InsurerResponse.Quoted(response.netPremium(), response.tax(), response.totalPremium());
			case "DECLINED" -> new InsurerResponse.Declined(response.declineReason());
			default -> throw new UnexpectedInsurerResponseException(CODE, "unknown outcome " + response.outcome());
		};
	}

	record PennineRequest(int driverAge, int vehicleGroup, BigDecimal riskPremium) {
	}

	record PennineResponse(String outcome, BigDecimal netPremium, BigDecimal tax, BigDecimal totalPremium,
			String declineReason) {
	}

}
