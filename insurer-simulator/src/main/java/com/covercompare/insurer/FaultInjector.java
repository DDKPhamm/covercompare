package com.covercompare.insurer;

import java.time.Duration;
import java.util.random.RandomGenerator;

import com.covercompare.insurer.SimulatorProperties.Faults;

/**
 * Applies an insurer's configured latency and failure rate to the current request.
 */
public class FaultInjector {

	private final SimulatorProperties properties;

	private final RandomGenerator random;

	private final Sleeper sleeper;

	public FaultInjector(SimulatorProperties properties, RandomGenerator random, Sleeper sleeper) {
		this.properties = properties;
		this.random = random;
		this.sleeper = sleeper;
	}

	public void apply(String insurerCode) {
		Faults faults = properties.insurer(insurerCode).faults();
		Duration delay = faults.latency();
		if (faults.jitter().isPositive()) {
			delay = delay.plusMillis(random.nextLong(faults.jitter().toMillis() + 1));
		}
		if (delay.isPositive()) {
			sleeper.sleep(delay);
		}
		if (random.nextDouble() < faults.failureRate()) {
			throw new SimulatedOutageException(insurerCode);
		}
	}

	@FunctionalInterface
	public interface Sleeper {

		void sleep(Duration duration);

	}

	public static class SimulatedOutageException extends RuntimeException {

		public SimulatedOutageException(String insurerCode) {
			super("Simulated outage at " + insurerCode);
		}

	}

}
