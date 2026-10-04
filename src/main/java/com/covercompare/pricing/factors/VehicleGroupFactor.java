package com.covercompare.pricing.factors;

import java.math.BigDecimal;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.covercompare.pricing.RatingAdjustment;
import com.covercompare.pricing.RatingFactor;
import com.covercompare.pricing.RiskProfile;

/**
 * UK insurance groups run from 1 (cheapest to insure) to 50. Each group above 1 adds 0.03 to a
 * starting multiplier of 0.80, so group 1 is 0.80 and group 50 is 2.27.
 */
@Component
@Order(5)
class VehicleGroupFactor implements RatingFactor {

	static final String NAME = "Vehicle insurance group";

	private static final BigDecimal GROUP_ONE_MULTIPLIER = new BigDecimal("0.80");

	private static final BigDecimal STEP_PER_GROUP = new BigDecimal("0.03");

	@Override
	public RatingAdjustment assess(RiskProfile profile) {
		int group = profile.vehicleInsuranceGroup();
		BigDecimal multiplier = GROUP_ONE_MULTIPLIER.add(STEP_PER_GROUP.multiply(BigDecimal.valueOf(group - 1)));
		return new RatingAdjustment(NAME, multiplier, "Insurance group " + group);
	}

}
