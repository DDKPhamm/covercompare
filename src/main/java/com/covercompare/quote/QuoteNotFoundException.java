package com.covercompare.quote;

import java.util.UUID;

public class QuoteNotFoundException extends RuntimeException {

	public QuoteNotFoundException(UUID id) {
		super("No quote found with id " + id);
	}

}
