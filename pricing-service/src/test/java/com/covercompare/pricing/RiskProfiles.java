package com.covercompare.pricing;

/**
 * Builds {@link RiskProfile}s for tests. Starts from a typical low-risk driver so each test only
 * has to state the detail it cares about.
 */
public final class RiskProfiles {

	private CoverType coverType = CoverType.COMPREHENSIVE;

	private int driverAge = 35;

	private int licenceHeldYears = 10;

	private int noClaimsYears = 5;

	private String postcodeArea = "SK";

	private int vehicleInsuranceGroup = 20;

	private int vehicleAgeYears = 5;

	private int voluntaryExcess = 250;

	private RiskProfiles() {
	}

	public static RiskProfiles standard() {
		return new RiskProfiles();
	}

	public RiskProfiles coverType(CoverType coverType) {
		this.coverType = coverType;
		return this;
	}

	public RiskProfiles driverAge(int driverAge) {
		this.driverAge = driverAge;
		return this;
	}

	public RiskProfiles licenceHeldYears(int licenceHeldYears) {
		this.licenceHeldYears = licenceHeldYears;
		return this;
	}

	public RiskProfiles noClaimsYears(int noClaimsYears) {
		this.noClaimsYears = noClaimsYears;
		return this;
	}

	public RiskProfiles postcodeArea(String postcodeArea) {
		this.postcodeArea = postcodeArea;
		return this;
	}

	public RiskProfiles vehicleInsuranceGroup(int vehicleInsuranceGroup) {
		this.vehicleInsuranceGroup = vehicleInsuranceGroup;
		return this;
	}

	public RiskProfiles vehicleAgeYears(int vehicleAgeYears) {
		this.vehicleAgeYears = vehicleAgeYears;
		return this;
	}

	public RiskProfiles voluntaryExcess(int voluntaryExcess) {
		this.voluntaryExcess = voluntaryExcess;
		return this;
	}

	public RiskProfile build() {
		return new RiskProfile(coverType, driverAge, licenceHeldYears, noClaimsYears, postcodeArea,
				vehicleInsuranceGroup, vehicleAgeYears, voluntaryExcess);
	}

}
