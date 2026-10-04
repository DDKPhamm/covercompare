package com.covercompare.quote;

import java.time.LocalDate;

import com.covercompare.pricing.CoverType;
import com.covercompare.quote.api.DriverDetails;
import com.covercompare.quote.api.QuoteRequest;
import com.covercompare.quote.api.VehicleDetails;

public final class QuoteRequests {

	private QuoteRequests() {
	}

	/** A 35-year-old Stockport driver on 4 October 2026, the date the test clock is fixed to. */
	public static QuoteRequest standard() {
		return withDriver(new DriverDetails(LocalDate.of(1991, 3, 15), 10, 5, "sk1 3ab"));
	}

	public static QuoteRequest withDriver(DriverDetails driver) {
		return new QuoteRequest(driver, new VehicleDetails("Ford", "Fiesta", 2021, 20), CoverType.COMPREHENSIVE, 250);
	}

	public static QuoteRequest withVehicle(VehicleDetails vehicle) {
		return new QuoteRequest(standard().driver(), vehicle, CoverType.COMPREHENSIVE, 250);
	}

}
