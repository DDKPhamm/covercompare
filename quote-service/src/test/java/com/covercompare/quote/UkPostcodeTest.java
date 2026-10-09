package com.covercompare.quote;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class UkPostcodeTest {

	@ParameterizedTest(name = "\"{0}\" -> {1}")
	@CsvSource({ "SK1 3AB, SK1 3AB", "sk13ab, SK1 3AB", "' m1  1ae ', M1 1AE", "EC1A1BB, EC1A 1BB",
			"W1A 0AX, W1A 0AX" })
	void normalisesCaseAndSpacing(String input, String expected) {
		assertThat(UkPostcode.normalise(input)).isEqualTo(expected);
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@CsvSource({ "SK1 3AB, SK", "M1 1AE, M", "EC1A 1BB, EC", "W1A 0AX, W" })
	void extractsTheAreaLetters(String postcode, String expected) {
		assertThat(UkPostcode.area(postcode)).isEqualTo(expected);
	}

	@ParameterizedTest
	@ValueSource(strings = { "SK1 3AB", "sk13ab", "M1 1AE", "EC1A 1BB", "B33 8TH" })
	void acceptsValidPostcodes(String postcode) {
		assertThat(postcode).matches(UkPostcode.PATTERN);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "12345", "SK1", "SK1 3A", "SKK1 3AB", "SK1 3AB1" })
	void rejectsInvalidPostcodes(String postcode) {
		assertThat(postcode).doesNotMatch(UkPostcode.PATTERN);
	}

}
