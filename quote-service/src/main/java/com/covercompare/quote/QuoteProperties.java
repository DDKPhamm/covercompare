package com.covercompare.quote;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("covercompare.quote")
public record QuoteProperties(@NotNull Duration validity) {
}
