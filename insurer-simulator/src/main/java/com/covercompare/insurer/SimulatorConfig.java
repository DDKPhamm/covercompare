package com.covercompare.insurer;

import java.util.random.RandomGenerator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class SimulatorConfig {

	@Bean
	FaultInjector faultInjector(SimulatorProperties properties) {
		return new FaultInjector(properties, RandomGenerator.getDefault(), duration -> {
			try {
				Thread.sleep(duration);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		});
	}

}
