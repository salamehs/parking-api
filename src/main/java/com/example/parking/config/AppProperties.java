package com.example.parking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Map;

/**
 * Application settings loaded from {@code application.yml}.
 *
 * <p>It contains the configured cities and their provider settings. Each city is identified by its
 * id in the YAML file.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Map<String, CityProperties> cities) {

    public AppProperties {
        cities = cities == null ? Map.of() : cities;
    }

    /**
     * Settings for one city.
     *
     * <p>The city center and coverage radius are used to find the right city for a position. The
     * provider type selects the adapter, and the base URL is the city's public parking source.
     */
    public record CityProperties(
        Double centerLat,
        Double centerLon,
        @DefaultValue("15000") int radiusMeters,
        String providerType,
        String baseUrl,
        @DefaultValue("2s") Duration timeout) {
    }
}
