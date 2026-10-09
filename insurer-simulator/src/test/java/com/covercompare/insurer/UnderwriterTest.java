package com.covercompare.insurer;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.covercompare.insurer.SimulatorProperties.Faults;
import com.covercompare.insurer.SimulatorProperties.InsurerSettings;
import com.covercompare.insurer.Underwriter.Decision;

class UnderwriterTest {

	private static final BigDecimal STANDARD_RISK_PREMIUM = new BigDecimal("322.121250");

	private final Underwriter underwriter = new Underwriter(new SimulatorProperties(new BigDecimal("0.12"),
			Map.of("pennine", insurer("0.95", 25, 50), "redbrick", insurer("1.12", 17, 40), "flat",
					insurer("1.00", 17, 50))));

	@Test
	void appliesTheInsurersLoadingThenInsurancePremiumTax() {
		Decision decision = underwriter.underwrite("pennine", 35, 20, STANDARD_RISK_PREMIUM);

		assertThat(decision).isEqualTo(
				new Decision.Quoted(new BigDecimal("306.02"), new BigDecimal("36.72"), new BigDecimal("342.74")));
	}

	@Test
	void roundsHalfPenniesUpAndKeepsTotalEqualToNetPlusTax() {
		Decision decision = underwriter.underwrite("flat", 35, 20, new BigDecimal("400.005"));

		Decision.Quoted quoted = (Decision.Quoted) decision;
		assertThat(quoted.netPremium()).isEqualTo(new BigDecimal("400.01"));
		assertThat(quoted.totalPremium()).isEqualTo(quoted.netPremium().add(quoted.insurancePremiumTax()));
	}

	@Test
	void declinesDriversBelowTheMinimumAge() {
		assertThat(underwriter.underwrite("pennine", 24, 20, STANDARD_RISK_PREMIUM))
			.isEqualTo(new Decision.Declined("Driver must be at least 25"));
	}

	@Test
	void declinesVehiclesAboveTheMaximumGroup() {
		assertThat(underwriter.underwrite("redbrick", 35, 41, STANDARD_RISK_PREMIUM))
			.isEqualTo(new Decision.Declined("Vehicle insurance group must be 40 or below"));
	}

	static InsurerSettings insurer(String multiplier, int minDriverAge, int maxVehicleGroup) {
		return new InsurerSettings("Test", new BigDecimal(multiplier), minDriverAge, maxVehicleGroup, noFaults());
	}

	static Faults noFaults() {
		return new Faults(Duration.ZERO, Duration.ZERO, 0);
	}

}
