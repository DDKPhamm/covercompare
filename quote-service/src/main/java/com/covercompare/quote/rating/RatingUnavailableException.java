package com.covercompare.quote.rating;

public class RatingUnavailableException extends RuntimeException {

	public RatingUnavailableException(Throwable cause) {
		super("Pricing service could not rate the risk", cause);
	}

}
