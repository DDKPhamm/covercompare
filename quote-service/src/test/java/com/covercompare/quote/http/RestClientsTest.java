package com.covercompare.quote.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.web.client.RestClient;

class RestClientsTest {

	@RegisterExtension
	static WireMockExtension server = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

	@Test
	void aSlowResponseIsATimeoutAndNotRetryable() {
		server.stubFor(get("/slow").willReturn(ok().withFixedDelay(3_000)));

		Throwable failure = catchThrowable(() -> client(server.baseUrl(), Duration.ofMillis(300)).get()
			.uri("/slow")
			.retrieve()
			.toBodilessEntity());

		assertThat(RestClients.isTimeout(failure)).as("timeout: %s", failure).isTrue();
		assertThat(RestClients.isRetryable(failure)).isFalse();
	}

	@Test
	void aServerErrorIsRetryable() {
		server.stubFor(get("/broken").willReturn(serviceUnavailable()));

		Throwable failure = catchThrowable(
				() -> client(server.baseUrl(), Duration.ofSeconds(2)).get().uri("/broken").retrieve().toBodilessEntity());

		assertThat(RestClients.isRetryable(failure)).isTrue();
		assertThat(RestClients.isTimeout(failure)).isFalse();
	}

	@Test
	void aClientErrorIsNotRetryable() {
		server.stubFor(get("/bad").willReturn(aResponse().withStatus(400)));

		Throwable failure = catchThrowable(
				() -> client(server.baseUrl(), Duration.ofSeconds(2)).get().uri("/bad").retrieve().toBodilessEntity());

		assertThat(RestClients.isRetryable(failure)).isFalse();
	}

	@Test
	void aRefusedConnectionIsRetryable() throws IOException {
		int closedPort;
		try (ServerSocket socket = new ServerSocket(0)) {
			closedPort = socket.getLocalPort();
		}

		Throwable failure = catchThrowable(() -> client("http://localhost:" + closedPort, Duration.ofSeconds(2)).get()
			.uri("/")
			.retrieve()
			.toBodilessEntity());

		assertThat(RestClients.isRetryable(failure)).as("retryable: %s", failure).isTrue();
		assertThat(RestClients.isTimeout(failure)).isFalse();
	}

	private static RestClient client(String baseUrl, Duration readTimeout) {
		return RestClients.create(RestClient.builder(),
				new HttpConnection(URI.create(baseUrl), Duration.ofSeconds(1), readTimeout));
	}

}
