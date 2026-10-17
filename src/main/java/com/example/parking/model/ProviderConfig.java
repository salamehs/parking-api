package com.example.parking.model;

import java.time.Duration;

/**
 * Provider connection settings.
 *
 * <p>It stores the provider URL and the timeout used when calling that city's source.
 */
public record ProviderConfig(String baseUrl, Duration timeout) {
}
