package com.example.parking.service;

import com.example.parking.exception.ProviderException;
import com.example.parking.exception.ProviderException.Kind;
import com.example.parking.model.City;
import com.example.parking.model.GeoPoint;
import com.example.parking.model.Parking;
import com.example.parking.model.ProviderConfig;
import com.example.parking.provider.ParkingProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CityParkingsCacheTest {

    private static final Duration TTL = Duration.ofSeconds(30);
    private static final ProviderConfig CONFIG = new ProviderConfig("https://p.example/x", Duration.ofSeconds(2));
    private static final City POITIERS = new City("poitiers", new GeoPoint(46.58, 0.34), 15_000, "fake", CONFIG);
    private static final City TOURS = new City("tours", new GeoPoint(47.39, 0.68), 15_000, "fake", CONFIG);
    private final FakeProvider provider = new FakeProvider();
    private final AtomicLong nanos = new AtomicLong();
    private final CityParkingsCache cache = new CityParkingsCache(List.of(provider), TTL, nanos::get);

    private void passTime(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }

    @Test
    void secondRequestIsServedFromTheCache() {
        cache.get(POITIERS);
        List<Parking> second = cache.get(POITIERS);

        assertThat(provider.calls).isEqualTo(1);
        assertThat(second).extracting(Parking::getName).containsExactly("THEATRE");
    }

    @Test
    void eachCityHasItsOwnEntry() {
        cache.get(POITIERS);
        cache.get(TOURS);
        cache.get(POITIERS);

        assertThat(provider.calls).isEqualTo(2);
    }

    @Test
    void entryIsRefreshedOnceTheTtlHasPassed() {
        cache.get(POITIERS);

        passTime(TTL.minusSeconds(1));
        cache.get(POITIERS);
        assertThat(provider.calls).isEqualTo(1);

        passTime(Duration.ofSeconds(2));
        cache.get(POITIERS);
        assertThat(provider.calls).isEqualTo(2);
    }

    @Test
    void failureIsNotCached() {
        provider.failure = new ProviderException(Kind.UNAVAILABLE, "down", null);
        assertThatThrownBy(() -> cache.get(POITIERS)).isSameAs(provider.failure);

        provider.failure = null;
        assertThat(cache.get(POITIERS)).hasSize(1);
        assertThat(provider.calls).isEqualTo(2);
    }

    @Test
    void zeroTtlDisablesTheCache() {
        CityParkingsCache disabled = new CityParkingsCache(List.of(provider), Duration.ZERO);

        disabled.get(POITIERS);
        disabled.get(POITIERS);

        assertThat(provider.calls).isEqualTo(2);
    }

    @Test
    void clearForcesANewProviderCall() {
        cache.get(POITIERS);

        cache.clear();
        cache.get(POITIERS);

        assertThat(provider.calls).isEqualTo(2);
    }

    @Test
    void cachedListIsNotAffectedByLaterChangesOfTheProviderList() {
        List<Parking> first = cache.get(POITIERS);

        provider.parkings.clear();

        assertThat(first).hasSize(1);
        assertThat(cache.get(POITIERS)).hasSize(1);
    }

    /**
     * Counts its calls, and fails while {@code failure} is set.
     */
    private static final class FakeProvider implements ParkingProvider {
        final List<Parking> parkings = new ArrayList<>(List.of(Parking.builder().name("THEATRE").build()));
        int calls;
        ProviderException failure;

        @Override
        public String type() {
            return "fake";
        }

        @Override
        public List<Parking> findAll(ProviderConfig config) {
            calls++;
            if (failure != null) {
                throw failure;
            }
            return parkings;
        }
    }
}
