package com.covercompare.insurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import com.covercompare.insurer.FaultInjector.SimulatedOutageException;
import com.covercompare.insurer.SimulatorProperties.Faults;
import com.covercompare.insurer.SimulatorProperties.InsurerSettings;

class FaultInjectorTest {

	private final List<Duration> sleeps = new ArrayList<>();

	@Test
	void doesNothingWhenNoFaultsAreConfigured() {
		FaultInjector injector = injector(new Faults(Duration.ZERO, Duration.ZERO, 0), fixedRandom(0.0, 0));

		assertThatNoException().isThrownBy(() -> injector.apply("acme"));
		assertThat(sleeps).isEmpty();
	}

	@Test
	void addsLatencyPlusRandomJitter() {
		FaultInjector injector = injector(new Faults(Duration.ofMillis(200), Duration.ofMillis(100), 0),
				fixedRandom(0.99, 40));

		injector.apply("acme");

		assertThat(sleeps).containsExactly(Duration.ofMillis(240));
	}

	@Test
	void failsWhenTheRandomDrawFallsWithinTheFailureRate() {
		FaultInjector injector = injector(new Faults(Duration.ZERO, Duration.ZERO, 0.3), fixedRandom(0.29, 0));

		assertThatThrownBy(() -> injector.apply("acme")).isInstanceOf(SimulatedOutageException.class)
			.hasMessage("Simulated outage at acme");
	}

	@Test
	void succeedsWhenTheRandomDrawIsAboveTheFailureRate() {
		FaultInjector injector = injector(new Faults(Duration.ZERO, Duration.ZERO, 0.3), fixedRandom(0.30, 0));

		assertThatNoException().isThrownBy(() -> injector.apply("acme"));
	}

	private FaultInjector injector(Faults faults, RandomGenerator random) {
		SimulatorProperties properties = new SimulatorProperties(new BigDecimal("0.12"),
				Map.of("acme", new InsurerSettings("Acme", BigDecimal.ONE, 17, 50, faults)));
		return new FaultInjector(properties, random, sleeps::add);
	}

	private static RandomGenerator fixedRandom(double nextDouble, long nextLong) {
		return new RandomGenerator() {
			@Override
			public long nextLong() {
				return nextLong;
			}

			@Override
			public long nextLong(long bound) {
				return nextLong;
			}

			@Override
			public double nextDouble() {
				return nextDouble;
			}
		};
	}

}
