package com.covercompare.quote.insurer;

import java.time.Duration;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.covercompare.quote.http.HttpConnection;

/**
 * @param deadline the longest a customer waits for the whole panel; insurers that have not answered
 * by then are reported as unavailable
 */
@Validated
@ConfigurationProperties("covercompare.insurers")
public record InsurerPanelProperties(
		@NotNull Duration deadline,
		@NotNull @Valid RetrySettings retry,
		@NotNull @Valid CircuitBreakerSettings circuitBreaker,
		@NotEmpty Map<String, @Valid HttpConnection> connections) {

	public HttpConnection connection(String insurerCode) {
		HttpConnection connection = connections.get(insurerCode);
		if (connection == null) {
			throw new IllegalStateException("No connection configured for insurer " + insurerCode);
		}
		return connection;
	}

	/**
	 * @param maxAttempts total attempts including the first, so 2 means one retry
	 * @param backoff pause before each retry
	 */
	public record RetrySettings(@Min(1) @Max(5) int maxAttempts, @NotNull Duration backoff) {
	}

	/**
	 * Over the last {@code slidingWindowSize} calls (once at least {@code minimumNumberOfCalls} have
	 * been made), if {@code failureRateThreshold}% or more failed, stop calling the insurer for
	 * {@code waitInOpenState}, then let a few trial calls through to see if it has recovered.
	 */
	public record CircuitBreakerSettings(
			@Min(1) @Max(100) int failureRateThreshold,
			@Min(1) int slidingWindowSize,
			@Min(1) int minimumNumberOfCalls,
			@NotNull Duration waitInOpenState) {
	}

}
