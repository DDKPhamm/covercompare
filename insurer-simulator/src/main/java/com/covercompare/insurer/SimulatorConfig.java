package com.covercompare.insurer;

import java.util.Random;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class SimulatorConfig {

	/**
	 * {@code java.util.Random} rather than {@code RandomGenerator.getDefault()}: the default algorithm
	 * lives in the {@code jdk.random} module, which slim JRE images leave out. Random is also safe to
	 * share between request threads.
	 */
	@Bean
	FaultInjector faultInjector(SimulatorProperties properties) {
		return new FaultInjector(properties, new Random(), duration -> {
			try {
				Thread.sleep(duration);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		});
	}

}
