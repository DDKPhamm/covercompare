package com.covercompare.quote.rating;

import java.time.Duration;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.covercompare.quote.http.RestClients;

/**
 * Calls pricing-service. Without a rating there is nothing to show the customer, so any failure
 * after retries becomes {@link RatingUnavailableException} and the whole quote fails with 503.
 */
@Component
public class RatingClient {

	private final RestClient restClient;

	private final Retry retry;

	public RatingClient(RestClient.Builder builder, RatingProperties properties) {
		this.restClient = RestClients.create(builder, properties.connection());
		this.retry = Retry.of("pricing-service",
				RetryConfig.custom()
					.maxAttempts(properties.maxAttempts())
					.waitDuration(Duration.ofMillis(100))
					.retryOnException(RestClients::isRetryable)
					.build());
	}

	public Rating rate(RatingRequest request) {
		try {
			return retry.executeSupplier(() -> restClient.post()
				.uri("/api/v1/ratings")
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(Rating.class));
		}
		catch (RuntimeException ex) {
			throw new RatingUnavailableException(ex);
		}
	}

}
