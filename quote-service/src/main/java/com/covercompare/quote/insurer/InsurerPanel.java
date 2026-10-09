package com.covercompare.quote.insurer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.annotation.PreDestroy;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.covercompare.quote.http.RestClients;

/**
 * Asks every insurer for a price at the same time and collects the answers. One slow or broken
 * insurer can never make the customer wait longer than the deadline, and never stops the other
 * insurers' prices being shown.
 */
@Service
public class InsurerPanel {

	static final String TIMED_OUT = "Insurer did not respond in time";

	static final String TEMPORARILY_UNAVAILABLE = "Insurer is temporarily unavailable";

	private static final Logger log = LoggerFactory.getLogger(InsurerPanel.class);

	private final List<InsurerAdapter> adapters;

	private final CircuitBreakerRegistry circuitBreakers;

	private final RetryRegistry retries;

	private final ExecutorService executor;

	private final long deadlineMillis;

	/** One cheap virtual thread per insurer call, so waiting on slow insurers costs almost nothing. */
	@Autowired
	public InsurerPanel(List<InsurerAdapter> adapters, CircuitBreakerRegistry circuitBreakers, RetryRegistry retries,
			InsurerPanelProperties properties) {
		this(adapters, circuitBreakers, retries, Executors.newVirtualThreadPerTaskExecutor(), properties);
	}

	InsurerPanel(List<InsurerAdapter> adapters, CircuitBreakerRegistry circuitBreakers, RetryRegistry retries,
			ExecutorService executor, InsurerPanelProperties properties) {
		this.adapters = List.copyOf(adapters);
		this.circuitBreakers = circuitBreakers;
		this.retries = retries;
		this.executor = executor;
		this.deadlineMillis = properties.deadline().toMillis();
	}

	public List<InsurerOutcome> requestQuotes(InsurerQuoteRequest request) {
		List<CompletableFuture<InsurerOutcome>> calls = adapters.stream()
			.map(adapter -> CompletableFuture.supplyAsync(() -> call(adapter, request), executor)
				.completeOnTimeout(unavailable(adapter, TIMED_OUT), deadlineMillis, TimeUnit.MILLISECONDS))
			.toList();
		return calls.stream().map(CompletableFuture::join).sorted(InsurerOutcome.DISPLAY_ORDER).toList();
	}

	@PreDestroy
	void shutdown() {
		executor.shutdownNow();
	}

	private InsurerOutcome call(InsurerAdapter adapter, InsurerQuoteRequest request) {
		CircuitBreaker circuitBreaker = circuitBreakers.circuitBreaker(adapter.code());
		Retry retry = retries.retry(adapter.code());
		// Retry wraps the circuit breaker, so every attempt counts towards the breaker's failure rate.
		Supplier<InsurerResponse> guarded = Retry.decorateSupplier(retry,
				CircuitBreaker.decorateSupplier(circuitBreaker, () -> adapter.requestQuote(request)));
		try {
			return switch (guarded.get()) {
				case InsurerResponse.Quoted quoted -> new InsurerOutcome.Quoted(adapter.code(), adapter.displayName(),
						quoted.netPremium(), quoted.insurancePremiumTax(), quoted.totalAnnualPremium());
				case InsurerResponse.Declined declined ->
					new InsurerOutcome.Declined(adapter.code(), adapter.displayName(), declined.reason());
			};
		}
		catch (CallNotPermittedException ex) {
			log.info("Skipped {}: circuit breaker is open", adapter.code());
			return unavailable(adapter, TEMPORARILY_UNAVAILABLE);
		}
		catch (RuntimeException ex) {
			log.warn("Quote request to {} failed: {}", adapter.code(), ex.toString());
			return unavailable(adapter, RestClients.isTimeout(ex) ? TIMED_OUT : TEMPORARILY_UNAVAILABLE);
		}
	}

	private static InsurerOutcome unavailable(InsurerAdapter adapter, String reason) {
		return new InsurerOutcome.Unavailable(adapter.code(), adapter.displayName(), reason);
	}

}
