package com.covercompare.quote;

import java.util.List;

/**
 * The request was well-formed but describes an impossible or uninsurable situation, such as a
 * licence held for longer than the driver has been old enough to drive.
 */
public class InvalidQuoteRequestException extends RuntimeException {

	private final List<String> problems;

	public InvalidQuoteRequestException(List<String> problems) {
		super(String.join("; ", problems));
		this.problems = List.copyOf(problems);
	}

	public List<String> getProblems() {
		return problems;
	}

}
