package com.covercompare.quote;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Loads the shared contract files from the repository's {@code contracts/} folder, which the build
 * copies onto the test classpath. pricing-service tests the same files from the provider side.
 */
public final class Contracts {

	private Contracts() {
	}

	public static String read(String path) {
		try (InputStream in = Contracts.class.getResourceAsStream("/contracts/" + path)) {
			if (in == null) {
				throw new IllegalArgumentException("No contract file " + path);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
