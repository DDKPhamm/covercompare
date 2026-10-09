package com.covercompare.insurer;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.covercompare.insurer.SimulatorProperties.InsurerSettings;

/**
 * Decides whether a simulated insurer will cover a risk, and at what price. Every insurer shares
 * this logic; they differ only in their settings and in the API format they expose.
 */
@Component
public class Underwriter {

	private final SimulatorProperties properties;

	public Underwriter(SimulatorProperties properties) {
		this.properties = properties;
	}

	public Decision underwrite(String insurerCode, int driverAge, int vehicleGroup, BigDecimal riskPremium) {
		InsurerSettings insurer = properties.insurer(insurerCode);
		if (driverAge < insurer.minDriverAge()) {
			return new Decision.Declined("Driver must be at least " + insurer.minDriverAge());
		}
		if (vehicleGroup > insurer.maxVehicleGroup()) {
			return new Decision.Declined("Vehicle insurance group must be " + insurer.maxVehicleGroup() + " or below");
		}
		// Round each component to pence before adding, so net + tax always equals the total shown.
		BigDecimal net = riskPremium.multiply(insurer.rateMultiplier()).setScale(2, RoundingMode.HALF_UP);
		BigDecimal tax = net.multiply(properties.insurancePremiumTaxRate()).setScale(2, RoundingMode.HALF_UP);
		return new Decision.Quoted(net, tax, net.add(tax));
	}

	public sealed interface Decision {

		record Quoted(BigDecimal netPremium, BigDecimal insurancePremiumTax, BigDecimal totalPremium)
				implements Decision {
		}

		record Declined(String reason) implements Decision {
		}

	}

}
