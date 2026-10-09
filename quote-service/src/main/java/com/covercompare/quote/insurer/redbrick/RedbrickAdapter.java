package com.covercompare.quote.insurer.redbrick;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;

import com.covercompare.quote.http.RestClients;
import com.covercompare.quote.insurer.InsurerAdapter;
import com.covercompare.quote.insurer.InsurerPanelProperties;
import com.covercompare.quote.insurer.InsurerQuoteRequest;
import com.covercompare.quote.insurer.InsurerResponse;
import com.covercompare.quote.insurer.UnexpectedInsurerResponseException;

/**
 * Redbrick Insurance: XML over HTTP. A quote is HTTP 200; a decline is HTTP 422 with a reason.
 */
@Component
class RedbrickAdapter implements InsurerAdapter {

	static final String CODE = "redbrick";

	private final RestClient restClient;

	RedbrickAdapter(RestClient.Builder builder, InsurerPanelProperties properties) {
		this.restClient = RestClients.create(builder, properties.connection(CODE));
	}

	@Override
	public String code() {
		return CODE;
	}

	@Override
	public String displayName() {
		return "Redbrick Insurance";
	}

	@Override
	public InsurerResponse requestQuote(InsurerQuoteRequest request) {
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("DriverAge", Integer.toString(request.driverAge()));
		fields.put("VehicleGroup", Integer.toString(request.vehicleInsuranceGroup()));
		fields.put("RiskPremium", request.riskPremium().toPlainString());

		return restClient.post()
			.uri("/redbrick/quote")
			.contentType(MediaType.APPLICATION_XML)
			.accept(MediaType.APPLICATION_XML)
			.body(SecureXml.write("QuoteRequest", fields))
			.exchange((httpRequest, response) -> toResponse(response.getStatusCode(), readBody(response)), true);
	}

	private static InsurerResponse toResponse(HttpStatusCode status, String body) {
		if (status.is5xxServerError()) {
			throw HttpServerErrorException.create(status, "Redbrick returned " + status.value(), new HttpHeaders(),
					body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
		}
		if (status.value() == HttpStatus.OK.value()) {
			Document document = SecureXml.parse(CODE, body);
			return new InsurerResponse.Quoted(decimal(document, "Net"), decimal(document, "Ipt"),
					decimal(document, "Gross"));
		}
		if (status.value() == HttpStatus.UNPROCESSABLE_CONTENT.value()) {
			return new InsurerResponse.Declined(SecureXml.requiredText(CODE, SecureXml.parse(CODE, body), "Reason"));
		}
		throw new UnexpectedInsurerResponseException(CODE, "HTTP " + status.value());
	}

	private static BigDecimal decimal(Document document, String element) {
		try {
			return new BigDecimal(SecureXml.requiredText(CODE, document, element));
		}
		catch (NumberFormatException ex) {
			throw new UnexpectedInsurerResponseException(CODE, element + " is not a number");
		}
	}

	private static String readBody(ClientHttpResponse response) throws IOException {
		return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
	}

}
