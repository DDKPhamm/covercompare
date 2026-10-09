package com.covercompare.pricing.factors;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.covercompare.pricing.RatingAdjustment;
import com.covercompare.pricing.RiskProfiles;

class RatingFactorsTest {

	@Nested
	class DriverAge {

		private final DriverAgeFactor factor = new DriverAgeFactor();

		@ParameterizedTest(name = "age {0} -> {1}")
		@CsvSource({ "17, 2.20", "20, 2.20", "21, 1.70", "24, 1.70", "25, 1.25", "29, 1.25", "30, 1.00", "59, 1.00",
				"60, 1.10", "69, 1.10", "70, 1.35", "95, 1.35" })
		void loadsYoungAndElderlyDrivers(int age, String expected) {
			RatingAdjustment adjustment = factor.assess(RiskProfiles.standard().driverAge(age).build());

			assertThat(adjustment.multiplier()).isEqualByComparingTo(expected);
			assertThat(adjustment.factor()).isEqualTo(DriverAgeFactor.NAME);
		}

	}

	@Nested
	class DrivingExperience {

		private final DrivingExperienceFactor factor = new DrivingExperienceFactor();

		@ParameterizedTest(name = "{0} years -> {1}")
		@CsvSource({ "0, 1.40", "1, 1.20", "2, 1.20", "3, 1.10", "4, 1.10", "5, 1.00", "40, 1.00" })
		void loadsInexperiencedDrivers(int years, String expected) {
			RatingAdjustment adjustment = factor.assess(RiskProfiles.standard().licenceHeldYears(years).build());

			assertThat(adjustment.multiplier()).isEqualByComparingTo(expected);
		}

	}

	@Nested
	class NoClaimsDiscount {

		private final NoClaimsDiscountFactor factor = new NoClaimsDiscountFactor();

		@ParameterizedTest(name = "{0} years -> {1}")
		@CsvSource({ "0, 1.00", "1, 0.75", "5, 0.45", "9, 0.35", "25, 0.35" })
		void discountGrowsWithClaimFreeYearsAndIsCapped(int years, String expected) {
			RatingAdjustment adjustment = factor.assess(RiskProfiles.standard().noClaimsYears(years).build());

			assertThat(adjustment.multiplier()).isEqualByComparingTo(expected);
		}

		@Test
		void explainsTheDiscountInTheReason() {
			RatingAdjustment adjustment = factor.assess(RiskProfiles.standard().noClaimsYears(5).build());

			assertThat(adjustment.reason()).isEqualTo("5 year(s) no claims: 55% discount");
		}

	}

	@Nested
	class PostcodeArea {

		private final PostcodeAreaFactor factor = new PostcodeAreaFactor();

		@ParameterizedTest(name = "{0} -> {1}")
		@CsvSource({ "EC, 1.45", "M, 1.35", "SK, 1.10", "TR, 0.85", "ZE, 1.00" })
		void appliesRegionalLoadingWithNeutralDefault(String area, String expected) {
			RatingAdjustment adjustment = factor.assess(RiskProfiles.standard().postcodeArea(area).build());

			assertThat(adjustment.multiplier()).isEqualByComparingTo(expected);
		}

	}

	@Nested
	class VehicleGroup {

		private final VehicleGroupFactor factor = new VehicleGroupFactor();

		@ParameterizedTest(name = "group {0} -> {1}")
		@CsvSource({ "1, 0.80", "2, 0.83", "20, 1.37", "50, 2.27" })
		void scalesLinearlyFromGroupOneToFifty(int group, String expected) {
			RatingAdjustment adjustment = factor.assess(RiskProfiles.standard().vehicleInsuranceGroup(group).build());

			assertThat(adjustment.multiplier()).isEqualByComparingTo(expected);
		}

	}

	@Nested
	class VehicleAge {

		private final VehicleAgeFactor factor = new VehicleAgeFactor();

		@ParameterizedTest(name = "{0} years old -> {1}")
		@CsvSource({ "0, 1.10", "2, 1.10", "3, 1.00", "10, 1.00", "11, 1.05", "15, 1.05", "16, 1.15" })
		void loadsNewAndVeryOldVehicles(int years, String expected) {
			RatingAdjustment adjustment = factor.assess(RiskProfiles.standard().vehicleAgeYears(years).build());

			assertThat(adjustment.multiplier()).isEqualByComparingTo(expected);
		}

	}

	@Nested
	class VoluntaryExcess {

		private final VoluntaryExcessFactor factor = new VoluntaryExcessFactor();

		@ParameterizedTest(name = "£{0} -> {1}")
		@CsvSource({ "0, 1.00", "249, 1.00", "250, 0.95", "500, 0.90", "999, 0.90", "1000, 0.85", "1500, 0.85" })
		void higherExcessLowersPremium(int excess, String expected) {
			RatingAdjustment adjustment = factor.assess(RiskProfiles.standard().voluntaryExcess(excess).build());

			assertThat(adjustment.multiplier()).isEqualByComparingTo(expected);
		}

	}

}
