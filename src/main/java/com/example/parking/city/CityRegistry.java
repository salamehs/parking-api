package com.example.parking.city;

import com.example.parking.config.AppProperties;
import com.example.parking.config.AppProperties.CityProperties;
import com.example.parking.model.City;
import com.example.parking.model.GeoPoint;
import com.example.parking.model.ProviderConfig;
import com.example.parking.provider.ParkingProvider;
import com.example.parking.util.GeoUtils;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registry of the cities covered by the API.
 *
 * <p>It loads the city configuration, validates it at startup, and finds the city that covers a
 * given position. If several cities match, the nearest center wins.
 *
 * <p>If the configuration is invalid, the application fails fast during startup.
 */

@Component
public class CityRegistry {

    private final List<City> cities;

    public CityRegistry(AppProperties properties, List<ParkingProvider> providers) {
        Set<String> providerTypes = providers.stream().map(ParkingProvider::type).collect(Collectors.toSet());
        this.cities = properties.cities().entrySet().stream()
            .map(entry -> toCity(entry.getKey(), entry.getValue(), providerTypes))
            .toList();
    }

    private static double distanceToCenter(City city, double latitude, double longitude) {
        return GeoUtils.distanceMeters(latitude, longitude, city.center().latitude(), city.center().longitude());
    }

    private static City toCity(String cityId, CityProperties settings, Set<String> providerTypes) {
        Double centerLatitude = settings.centerLat();
        Double centerLongitude = settings.centerLon();
        if (centerLatitude == null || centerLongitude == null
            || Math.abs(centerLatitude) > 90 || Math.abs(centerLongitude) > 180) {
            throw invalid(cityId, "center-lat / center-lon are required and must be valid coordinates");
        }
        if (settings.radiusMeters() < 1) {
            throw invalid(cityId, "radius-meters must be >= 1");
        }
        if (settings.timeout() == null || settings.timeout().isZero() || settings.timeout().isNegative()) {
            throw invalid(cityId, "timeout must be greater than zero");
        }
        String providerType = settings.providerType();
        if (providerType == null || !providerTypes.contains(providerType)) {
            throw invalid(cityId, "unknown provider-type '" + providerType + "' (available: " + providerTypes + ")");
        }
        return new City(cityId, new GeoPoint(centerLatitude, centerLongitude), settings.radiusMeters(), providerType,
            new ProviderConfig(requireHttpUrl(cityId, settings.baseUrl()), settings.timeout()));
    }

    private static String requireHttpUrl(String cityId, String url) {
        if (url != null && isAbsoluteHttpUrl(url)) {
            return url;
        }
        throw invalid(cityId, "base-url must be an absolute http(s) URL, got '" + url + "'");
    }

    private static boolean isAbsoluteHttpUrl(String url) {
        try {
            URI uri = URI.create(url);
            return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) && uri.getHost() != null;
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }

    private static IllegalStateException invalid(String cityId, String reason) {
        return new IllegalStateException("City '" + cityId + "': " + reason);
    }

    /**
     * The city whose center is nearest to the position, if the position is within that city's coverage radius.
     */
    public Optional<City> findCovering(double latitude, double longitude) {
        return cities.stream()
            .filter(city -> distanceToCenter(city, latitude, longitude) <= city.coverageRadiusMeters())
            .min(Comparator.comparingDouble(city -> distanceToCenter(city, latitude, longitude)));
    }
}
