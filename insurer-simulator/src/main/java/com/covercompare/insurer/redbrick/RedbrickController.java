package com.covercompare.insurer.redbrick;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;

import com.covercompare.insurer.FaultInjector;
import com.covercompare.insurer.Underwriter;
import com.covercompare.insurer.Underwriter.Decision;
import com.covercompare.insurer.redbrick.SecureXml.InvalidXmlException;

/**
 * Redbrick Insurance: a legacy XML API that signals a decline with HTTP 422 rather than in the body.
 */
@RestController
class RedbrickController {

	static final String CODE = "redbrick";

	private final Underwriter underwriter;

	private final FaultInjector faults;

	RedbrickController(Underwriter underwriter, FaultInjector faults) {
		this.underwriter = underwriter;
		this.faults = faults;
	}

	@PostMapping(path = "/redbrick/quote", consumes = MediaType.APPLICATION_XML_VALUE,
			produces = MediaType.APPLICATION_XML_VALUE)
	ResponseEntity<String> quote(@RequestBody String body) {
		faults.apply(CODE);
		Document request = SecureXml.parse(body);
		int driverAge = parseInt(SecureXml.requiredText(request, "DriverAge"), "DriverAge", 17, 120);
		int vehicleGroup = parseInt(SecureXml.requiredText(request, "VehicleGroup"), "VehicleGroup", 1, 50);
		BigDecimal riskPremium = parsePositiveDecimal(SecureXml.requiredText(request, "RiskPremium"));

		Map<String, String> response = new LinkedHashMap<>();
		HttpStatus status = switch (underwriter.underwrite(CODE, driverAge, vehicleGroup, riskPremium)) {
			case Decision.Quoted quoted -> {
				response.put("Result", "QUOTE");
				response.put("Net", quoted.netPremium().toPlainString());
				response.put("Ipt", quoted.insurancePremiumTax().toPlainString());
				response.put("Gross", quoted.totalPremium().toPlainString());
				yield HttpStatus.OK;
			}
			case Decision.Declined declined -> {
				response.put("Result", "DECLINE");
				response.put("Reason", declined.reason());
				yield HttpStatus.UNPROCESSABLE_CONTENT;
			}
		};
		return xml(status, response);
	}

	@ExceptionHandler(InvalidXmlException.class)
	ResponseEntity<String> handleInvalidXml(InvalidXmlException ex) {
		return xml(HttpStatus.BAD_REQUEST, Map.of("Result", "ERROR", "Reason", ex.getMessage()));
	}

	private static ResponseEntity<String> xml(HttpStatus status, Map<String, String> fields) {
		return ResponseEntity.status(status)
			.contentType(MediaType.APPLICATION_XML)
			.body(SecureXml.write("QuoteResponse", new LinkedHashMap<>(fields)));
	}

	private static int parseInt(String value, String element, int min, int max) {
		try {
			int parsed = Integer.parseInt(value);
			if (parsed < min || parsed > max) {
				throw new InvalidXmlException(element + " must be between " + min + " and " + max, null);
			}
			return parsed;
		}
		catch (NumberFormatException ex) {
			throw new InvalidXmlException(element + " must be a whole number", ex);
		}
	}

	private static BigDecimal parsePositiveDecimal(String value) {
		try {
			BigDecimal parsed = new BigDecimal(value);
			if (parsed.signum() <= 0) {
				throw new InvalidXmlException("RiskPremium must be positive", null);
			}
			return parsed;
		}
		catch (NumberFormatException ex) {
			throw new InvalidXmlException("RiskPremium must be a decimal number", ex);
		}
	}

}
