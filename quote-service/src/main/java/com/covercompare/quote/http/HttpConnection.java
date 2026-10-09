package com.covercompare.quote.http;

import java.net.URI;
import java.time.Duration;

import jakarta.validation.constraints.NotNull;

/**
 * Where a downstream service lives and how long to wait for it.
 * @param connectTimeout how long to wait to open a connection; short, because a refused or
 * unreachable host will not start answering if you wait longer
 * @param readTimeout how long to wait for the response once connected
 */
public record HttpConnection(@NotNull URI baseUrl, @NotNull Duration connectTimeout, @NotNull Duration readTimeout) {
}
