package com.covercompare.quote.insurer;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import com.covercompare.quote.http.HttpConnection;

public final class TestInsurerProperties {

	private TestInsurerProperties() {
	}

	/** Generous timeouts: the first request in a cold test JVM can take well over half a second. */
	public static InsurerPanelProperties pointingAt(String insurerCode, String baseUrl) {
		return withConnections(Duration.ofSeconds(5),
				Map.of(insurerCode, new HttpConnection(URI.create(baseUrl), Duration.ofSeconds(2),
						Duration.ofSeconds(3))));
	}

	public static InsurerPanelProperties withConnections(Duration deadline, Map<String, HttpConnection> connections) {
		return new InsurerPanelProperties(deadline,
				new InsurerPanelProperties.RetrySettings(2, Duration.ofMillis(10)),
				new InsurerPanelProperties.CircuitBreakerSettings(50, 4, 4, Duration.ofMinutes(1)), connections);
	}

}
