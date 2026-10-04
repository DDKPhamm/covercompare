package com.covercompare;

import org.springframework.boot.SpringApplication;

/**
 * Runs the app against a throwaway PostgreSQL container (requires Docker). Start it from your IDE
 * or with {@code ./mvnw spring-boot:test-run}.
 */
public class TestCoverCompareApplication {

	public static void main(String[] args) {
		SpringApplication.from(CoverCompareApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
