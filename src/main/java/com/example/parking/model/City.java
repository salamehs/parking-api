package com.example.parking.model;

/**
 * A city covered by the API.
 *
 * <p>It stores the city id, its center, its coverage radius, and the provider used to load its
 * parkings.
 */
public record City(String id, GeoPoint center, int coverageRadiusMeters,
                   String providerType, ProviderConfig providerConfig) {
}
