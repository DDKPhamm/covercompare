package com.covercompare.quote.rating;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.covercompare.quote.http.HttpConnection;

@Validated
@ConfigurationProperties("covercompare.rating")
public record RatingProperties(@NotNull @Valid HttpConnection connection, @Min(1) int maxAttempts) {
}
