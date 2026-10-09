package com.covercompare.quote.insurer;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.covercompare.quote.http.RestClients;

@Configuration(proxyBeanMethods = false)
class ResilienceConfig {

	private static final Logger log = LoggerFactory.getLogger(ResilienceConfig.class);

	@Bean
	CircuitBreakerRegistry insurerCircuitBreakers(InsurerPanelProperties properties) {
		InsurerPanelProperties.CircuitBreakerSettings settings = properties.circuitBreaker();
		CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
			.slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
			.slidingWindowSize(settings.slidingWindowSize())
			.minimumNumberOfCalls(settings.minimumNumberOfCalls())
			.failureRateThreshold(settings.failureRateThreshold())
			.waitDurationInOpenState(settings.waitInOpenState())
			.automaticTransitionFromOpenToHalfOpenEnabled(false)
			.build());
		registry.getEventPublisher()
			.onEntryAdded(added -> added.getAddedEntry()
				.getEventPublisher()
				.onStateTransition(event -> log.warn("Circuit breaker for {} changed state: {}",
						event.getCircuitBreakerName(), event.getStateTransition())));
		return registry;
	}

	@Bean
	RetryRegistry insurerRetries(InsurerPanelProperties properties) {
		return RetryRegistry.of(RetryConfig.custom()
			.maxAttempts(properties.retry().maxAttempts())
			.waitDuration(properties.retry().backoff())
			.retryOnException(RestClients::isRetryable)
			.build());
	}

}
