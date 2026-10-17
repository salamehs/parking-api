package com.example.parking.model;

import java.util.Optional;

/**
 * A geographic point in WGS 84.
 *
 * <p>Latitude and longitude are stored in decimal degrees.
 */
public record GeoPoint(double latitude, double longitude) {

    /**
     * Parses a {@code "latitude, longitude"} string.
     *
     * <p>Returns empty if the text is missing, malformed, not finite, or outside valid coordinate
     * ranges.
     */
    public static Optional<GeoPoint> parse(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String[] parts = text.split(",");
        if (parts.length != 2) {
            return Optional.empty();
        }
        try {
            double latitude = Double.parseDouble(parts[0].trim());
            double longitude = Double.parseDouble(parts[1].trim());
            boolean valid = Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
            return valid ? Optional.of(new GeoPoint(latitude, longitude)) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Returns the first candidate that can be parsed as a valid geographic point.
     */
    public static Optional<GeoPoint> firstValid(String... candidates) {
        for (String candidate : candidates) {
            Optional<GeoPoint> point = parse(candidate);
            if (point.isPresent()) {
                return point;
            }
        }
        return Optional.empty();
    }
}
