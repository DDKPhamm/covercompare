package com.covercompare.quote.insurer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.covercompare.quote.insurer.InsurerOutcome.Declined;
import com.covercompare.quote.insurer.InsurerOutcome.Quoted;
import com.covercompare.quote.insurer.InsurerOutcome.Unavailable;

class InsurerPanelTest {

	private static final InsurerQuoteRequest REQUEST = new InsurerQuoteRequest(35, 20, new BigDecimal("322.12"));

	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

	@AfterEach
	void tearDown() {
		executor.shutdownNow();
	}

	@Test
	void showsQuotesCheapestFirstThenDeclinesThenUnavailableInsurers() {
		InsurerPanel panel = panel(Duration.ofSeconds(2),
				new FakeInsurer("broken", attempt -> {
					throw new UnexpectedInsurerResponseException("broken", "garbage");
				}),
				new FakeInsurer("pricey", attempt -> quote("500.00")),
				new FakeInsurer("picky", attempt -> new InsurerResponse.Declined("Too young")),
				new FakeInsurer("cheap", attempt -> quote("300.00")));

		List<InsurerOutcome> outcomes = panel.requestQuotes(REQUEST);

		assertThat(outcomes).extracting(InsurerOutcome::insurerCode)
			.containsExactly("cheap", "pricey", "picky", "broken");
		assertThat(outcomes.get(0)).isInstanceOf(Quoted.class);
		assertThat(outcomes.get(2)).isEqualTo(new Declined("picky", "Picky", "Too young"));
		assertThat(outcomes.get(3)).isEqualTo(new Unavailable("broken", "Broken", InsurerPanel.TEMPORARILY_UNAVAILABLE));
	}

	@Test
	void doesNotWaitPastTheDeadlineForASlowInsurer() {
		InsurerPanel panel = panel(Duration.ofMillis(200),
				new FakeInsurer("slow", attempt -> {
					sleep(Duration.ofSeconds(5));
					return quote("100.00");
				}),
				new FakeInsurer("fast", attempt -> quote("300.00")));

		long start = System.nanoTime();
		List<InsurerOutcome> outcomes = panel.requestQuotes(REQUEST);
		Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

		assertThat(elapsed).isLessThan(Duration.ofSeconds(2));
		assertThat(outcomes).containsExactly(
				new Quoted("fast", "Fast", new BigDecimal("267.86"), new BigDecimal("32.14"), new BigDecimal("300.00")),
				new Unavailable("slow", "Slow", InsurerPanel.TIMED_OUT));
	}

	@Test
	void callsInsurersInParallel() {
		IntFunction<InsurerResponse> takesHalfASecond = attempt -> {
			sleep(Duration.ofMillis(500));
			return quote("300.00");
		};
		InsurerPanel panel = panel(Duration.ofSeconds(3), new FakeInsurer("a", takesHalfASecond),
				new FakeInsurer("b", takesHalfASecond), new FakeInsurer("c", takesHalfASecond));

		long start = System.nanoTime();
		List<InsurerOutcome> outcomes = panel.requestQuotes(REQUEST);

		assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(1_200));
		assertThat(outcomes).allMatch(Quoted.class::isInstance);
	}

	@Test
	void retriesAServerErrorOnce() {
		FakeInsurer flaky = new FakeInsurer("flaky", attempt -> {
			if (attempt == 1) {
				throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
			}
			return quote("300.00");
		});

		List<InsurerOutcome> outcomes = panel(Duration.ofSeconds(2), flaky).requestQuotes(REQUEST);

		assertThat(outcomes).singleElement().isInstanceOf(Quoted.class);
		assertThat(flaky.calls()).isEqualTo(2);
	}

	@Test
	void doesNotRetryAnAnswerItCannotUnderstand() {
		FakeInsurer confused = new FakeInsurer("confused", attempt -> {
			throw new UnexpectedInsurerResponseException("confused", "unknown status");
		});

		panel(Duration.ofSeconds(2), confused).requestQuotes(REQUEST);

		assertThat(confused.calls()).isEqualTo(1);
	}

	@Test
	void reportsAReadTimeoutAsTimedOutWithoutRetrying() {
		FakeInsurer timingOut = new FakeInsurer("timeout", attempt -> {
			throw new ResourceAccessException("I/O error", new HttpTimeoutException("request timed out"));
		});

		List<InsurerOutcome> outcomes = panel(Duration.ofSeconds(2), timingOut).requestQuotes(REQUEST);

		assertThat(outcomes).containsExactly(new Unavailable("timeout", "Timeout", InsurerPanel.TIMED_OUT));
		assertThat(timingOut.calls()).isEqualTo(1);
	}

	@Test
	void retriesARefusedConnection() {
		FakeInsurer refusing = new FakeInsurer("refusing", attempt -> {
			throw new ResourceAccessException("I/O error", new ConnectException("Connection refused"));
		});

		panel(Duration.ofSeconds(2), refusing).requestQuotes(REQUEST);

		assertThat(refusing.calls()).isEqualTo(2);
	}

	@Test
	void stopsCallingAnInsurerOnceItsCircuitBreakerOpens() {
		FakeInsurer down = new FakeInsurer("down", attempt -> {
			throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
		});
		InsurerPanel panel = panel(Duration.ofSeconds(2), down);

		// Two requests, each tried twice, fill the breaker's window of 4 calls with failures.
		panel.requestQuotes(REQUEST);
		panel.requestQuotes(REQUEST);
		List<InsurerOutcome> third = panel.requestQuotes(REQUEST);

		assertThat(down.calls()).isEqualTo(4);
		assertThat(third)
			.containsExactly(new Unavailable("down", "Down", InsurerPanel.TEMPORARILY_UNAVAILABLE));
	}

	@Test
	void keepsEachInsurersCircuitBreakerSeparate() {
		FakeInsurer down = new FakeInsurer("down", attempt -> {
			throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
		});
		FakeInsurer healthy = new FakeInsurer("healthy", attempt -> quote("300.00"));
		InsurerPanel panel = panel(Duration.ofSeconds(2), down, healthy);

		for (int i = 0; i < 3; i++) {
			panel.requestQuotes(REQUEST);
		}

		assertThat(healthy.calls()).isEqualTo(3);
	}

	private InsurerPanel panel(Duration deadline, InsurerAdapter... adapters) {
		InsurerPanelProperties properties = TestInsurerProperties.withConnections(deadline, Map.of());
		ResilienceConfig config = new ResilienceConfig();
		return new InsurerPanel(List.of(adapters), config.insurerCircuitBreakers(properties),
				config.insurerRetries(properties), executor, properties);
	}

	/** Splits a total into net premium and 12% Insurance Premium Tax, like a real insurer would. */
	private static InsurerResponse quote(String total) {
		BigDecimal gross = new BigDecimal(total);
		BigDecimal net = gross.divide(new BigDecimal("1.12"), 2, RoundingMode.HALF_UP);
		return new InsurerResponse.Quoted(net, gross.subtract(net), gross);
	}

	private static void sleep(Duration duration) {
		try {
			Thread.sleep(duration);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new ResourceAccessException("interrupted", new IOException(ex));
		}
	}

	private static final class FakeInsurer implements InsurerAdapter {

		private final String code;

		private final IntFunction<InsurerResponse> behaviour;

		private final AtomicInteger calls = new AtomicInteger();

		FakeInsurer(String code, IntFunction<InsurerResponse> behaviour) {
			this.code = code;
			this.behaviour = behaviour;
		}

		@Override
		public String code() {
			return code;
		}

		@Override
		public String displayName() {
			return Character.toUpperCase(code.charAt(0)) + code.substring(1);
		}

		@Override
		public InsurerResponse requestQuote(InsurerQuoteRequest request) {
			return behaviour.apply(calls.incrementAndGet());
		}

		int calls() {
			return calls.get();
		}

	}

}
