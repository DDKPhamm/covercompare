package com.covercompare.quote.insurer;

/**
 * Talks to one insurer in whatever format that insurer's API uses. Adding an insurer to the panel
 * means writing one new adapter; {@link InsurerPanel} picks up every adapter bean automatically.
 */
public interface InsurerAdapter {

	/** Matches the insurer's key under {@code covercompare.insurers.connections}. */
	String code();

	String displayName();

	/**
	 * @throws RuntimeException if the insurer could not be reached or gave an unusable answer
	 */
	InsurerResponse requestQuote(InsurerQuoteRequest request);

}
