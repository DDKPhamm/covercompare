package com.covercompare.quote.insurer;

/**
 * The insurer answered, but not in a way the adapter understands. Not retried: asking again will
 * get the same answer.
 */
public class UnexpectedInsurerResponseException extends RuntimeException {

	public UnexpectedInsurerResponseException(String insurerCode, String detail) {
		super("Unexpected response from " + insurerCode + ": " + detail);
	}

}
