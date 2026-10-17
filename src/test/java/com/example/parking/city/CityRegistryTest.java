package com.example.parking.city;

import com.example.parking.config.AppProperties;
import com.example.parking.config.AppProperties.CityProperties;
import com.example.parking.model.City;
import com.example.parking.model.Parking;
import com.example.parking.model.ProviderConfig;
import com.example.parking.provider.ParkingProvider;
import com.example.parking.util.GeoUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CityRegistryTest {

    private static final List<ParkingProvider> PROVIDERS = List.of(provider("provider-x"));

    private static ParkingProvider provider(String type) {
        return new ParkingProvider() {
            @Override
            public String type() {
                return type;
            }

            @Override
            public List<Parking> findAll(ProviderConfig config) {
                return List.of();
            }
        };
    }

    private static CityProperties city(Double lat, Double lon, int radiusMeters, String type, String url) {
        return new CityProperties(lat, lon, radiusMeters, type, url, Duration.ofSeconds(2));
    }

    private static CityRegistry registry(Map<String, CityProperties> cities) {
        return new CityRegistry(new AppProperties(cities), PROVIDERS);
    }

    private static CityRegistry twoCities() {
        Map<String, CityProperties> cities = new LinkedHashMap<>();
        cities.put("poitiers", city(46.5802, 0.3404, 15_000, "provider-x", "https://poitiers.example/parkings"));
        cities.put("tours", city(47.3941, 0.6848, 15_000, "provider-x", "https://tours.example/parkings"));
        return registry(cities);
    }

    @Test
    void findsTheCityAPositionBelongsTo() {
        CityRegistry registry = twoCities();

        City poitiers = registry.findCovering(46.5838, 0.3377).orElseThrow();
        City tours = registry.findCovering(47.39, 0.69).orElseThrow();

        assertThat(poitiers.id()).isEqualTo("poitiers");
        assertThat(poitiers.providerConfig().baseUrl()).isEqualTo("https://poitiers.example/parkings");
        assertThat(tours.id()).isEqualTo("tours");
        assertThat(tours.providerConfig().baseUrl()).isEqualTo("https://tours.example/parkings");
    }

    @Test
    void positionOutsideEveryCityBelongsToNone() {
        assertThat(twoCities().findCovering(48.8566, 2.3522)).isEmpty(); // Paris
    }

    @Test
    void whenCoverageAreasOverlapTheNearestCenterWins() {
        Map<String, CityProperties> cities = new LinkedHashMap<>();
        cities.put("a", city(46.00, 0.00, 20_000, "provider-x", "https://a.example/p"));
        cities.put("b", city(46.10, 0.00, 20_000, "provider-x", "https://b.example/p"));

        assertThat(registry(cities).findCovering(46.08, 0.00).orElseThrow().id()).isEqualTo("b");
    }

    @Test
    void rejectsAnUnknownProviderType() {
        assertThatThrownBy(() -> registry(Map.of("poitiers",
            city(46.58, 0.34, 15_000, "provider-nope", "https://p.example/x"))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("poitiers")
            .hasMessageContaining("provider-nope");
        assertThatThrownBy(() -> registry(Map.of("poitiers", city(46.58, 0.34, 15_000, null, "https://p.example/x"))))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("provider-type");
    }

    @Test
    void coverageEndsAtTheConfiguredRadius() {
        double distance = GeoUtils.distanceMeters(46.5802, 0.3404, 46.60, 0.3404); // about 2.2 km north of the center
        int justInside = (int) Math.ceil(distance);
        int justOutside = (int) Math.floor(distance);

        assertThat(registry(Map.of("c", city(46.5802, 0.3404, justInside, "provider-x", "https://p.example/x")))
            .findCovering(46.60, 0.3404)).isPresent();
        assertThat(registry(Map.of("c", city(46.5802, 0.3404, justOutside, "provider-x", "https://p.example/x")))
            .findCovering(46.60, 0.3404)).isEmpty();
    }

    @Test
    void rejectsMissingOrInvalidFields() {
        for (Duration badTimeout : new Duration[]{null, Duration.ZERO, Duration.ofSeconds(-1)}) {
            CityProperties city = new CityProperties(46.58, 0.34, 15_000, "provider-x", "https://p.example/x", badTimeout);
            assertThatThrownBy(() -> registry(Map.of("c", city)))
                .as("timeout %s", badTimeout)
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("timeout");
        }
        assertThatThrownBy(() -> registry(Map.of("c", city(null, 0.34, 15_000, "provider-x", "https://p.example/x"))))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("center-lat");
        assertThatThrownBy(() -> registry(Map.of("c", city(95.0, 0.34, 15_000, "provider-x", "https://p.example/x"))))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("center-lat");
        assertThatThrownBy(() -> registry(Map.of("c", city(46.58, 0.34, 0, "provider-x", "https://p.example/x"))))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("radius-meters");
        for (String badUrl : new String[]{null, " ", "not a url", "ftp://p.example/x", "/relative", "http://"}) {
            assertThatThrownBy(() -> registry(Map.of("c", city(46.58, 0.34, 15_000, "provider-x", badUrl))))
                .as("base-url '%s'", badUrl)
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("base-url");
        }
    }
}
