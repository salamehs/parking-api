package com.example.parking.model;

/**
 * Search parameters for a nearby parking request.
 *
 * <p>The controller validates these values before the service uses them.
 */
public record SearchCriteria(double latitude, double longitude, int radiusMeters) {
}
