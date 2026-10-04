package com.covercompare.quote;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helpers for UK postcodes such as "SK1 3AB". The validation pattern is a practical approximation of
 * the official format, not a check that the postcode actually exists.
 */
public final class UkPostcode {

	public static final String PATTERN = "(?i)^\\s*[A-Z]{1,2}[0-9][A-Z0-9]?\\s*[0-9][A-Z]{2}\\s*$";

	private static final Pattern AREA = Pattern.compile("^[A-Z]{1,2}");

	private UkPostcode() {
	}

	/** Uppercases and puts a single space before the 3-character inward code: "sk13ab" becomes "SK1 3AB". */
	public static String normalise(String postcode) {
		String compact = postcode.replaceAll("\\s+", "").toUpperCase(Locale.UK);
		return compact.substring(0, compact.length() - 3) + " " + compact.substring(compact.length() - 3);
	}

	/** The leading letters of a normalised postcode: "SK1 3AB" gives "SK", "M1 1AE" gives "M". */
	public static String area(String normalisedPostcode) {
		Matcher matcher = AREA.matcher(normalisedPostcode);
		if (!matcher.find()) {
			throw new IllegalArgumentException("Not a UK postcode: " + normalisedPostcode);
		}
		return matcher.group();
	}

}
