package com.example.parking.service;

import com.example.parking.city.CityRegistry;
import com.example.parking.config.AppProperties;
import com.example.parking.config.AppProperties.CityProperties;
import com.example.parking.exception.CityNotCoveredException;
import com.example.parking.exception.ProviderException;
import com.example.parking.model.Parking;
import com.example.parking.model.ProviderConfig;
import com.example.parking.model.SearchCriteria;
import com.example.parking.provider.ParkingProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParkingServiceTest {

    private static final Parking FAR = Parking.builder().name("Far").latitude(46.60).longitude(0.37).build();
    private static final Parking NEAR = Parking.builder().name("Near").latitude(46.5839).longitude(0.3378).build();
    private static final Parking MIDDLE = Parking.builder().name("Middle").latitude(46.5810).longitude(0.3400).build();

    private static ParkingService serviceWith(FakeProvider provider) {
        Map<String, CityProperties> cities = Map.of(
            "poitiers", new CityProperties(46.5802, 0.3404, 15_000, "poitiers",
                "https://poitiers.example/parkings", Duration.ofSeconds(2)),
            "tours", new CityProperties(47.3941, 0.6848, 15_000, "poitiers",
                "https://tours.example/parkings", Duration.ofSeconds(2)));
        CityRegistry registry = new CityRegistry(new AppProperties(cities), List.of(provider));
        return new ParkingService(registry, new CityParkingsCache(List.of(provider), Duration.ZERO)); // no cache here
    }

    @Test
    void computesDistanceFiltersByRadiusAndSortsNearestFirst() {
        ParkingService service = serviceWith(new FakeProvider(List.of(FAR, MIDDLE, NEAR)));

        List<Parking> result = service.findNearby(new SearchCriteria(46.5838, 0.3377, 1000));

        assertThat(result).extracting(Parking::getName).containsExactly("Near", "Middle");
        assertThat(result.get(0).getDistanceMeters()).isLessThan(20);
        assertThat(result.get(1).getDistanceMeters()).isBetween(340, 375);
    }

    @Test
    void smallerRadiusKeepsOnlyTheParkingsInside() {
        ParkingService service = serviceWith(new FakeProvider(List.of(FAR, MIDDLE, NEAR)));

        List<Parking> result = service.findNearby(new SearchCriteria(46.5838, 0.3377, 100));

        assertThat(result).extracting(Parking::getName).containsExactly("Near");
    }

    @Test
    void callsTheProviderOfTheCityThePositionBelongsTo() {
        FakeProvider provider = new FakeProvider(List.of());
        ParkingService service = serviceWith(provider);

        service.findNearby(new SearchCriteria(46.5838, 0.3377, 1000)); // Poitiers
        service.findNearby(new SearchCriteria(47.3900, 0.6900, 1000)); // Tours

        assertThat(provider.urls).containsExactly("https://poitiers.example/parkings", "https://tours.example/parkings");
    }

    @Test
    void positionInNoCoveredCityIsRefusedWithoutCallingAnyProvider() {
        FakeProvider provider = new FakeProvider(List.of(NEAR));
        ParkingService service = serviceWith(provider);

        assertThatThrownBy(() -> service.findNearby(new SearchCriteria(48.8566, 2.3522, 1000))) // Paris
            .isInstanceOf(CityNotCoveredException.class);
        assertThat(provider.urls).isEmpty();
    }

    @Test
    void providerFailureIsPassedOnUnchanged() {
        FakeProvider provider = new FakeProvider(List.of());
        provider.failure = new ProviderException(ProviderException.Kind.TIMEOUT, "Read timed out", null);
        ParkingService service = serviceWith(provider);

        assertThatThrownBy(() -> service.findNearby(new SearchCriteria(46.5838, 0.3377, 1000)))
            .isSameAs(provider.failure);
    }

    @Test
    void parkingExactlyOnTheRadiusIsKept() {
        ParkingService service = serviceWith(new FakeProvider(List.of(NEAR, MIDDLE)));
        int middleDistance = service.findNearby(new SearchCriteria(46.5838, 0.3377, 1000)).get(1).getDistanceMeters();

        assertThat(service.findNearby(new SearchCriteria(46.5838, 0.3377, middleDistance)))
            .extracting(Parking::getName).containsExactly("Near", "Middle");
    }

    private static final class FakeProvider implements ParkingProvider {
        final List<String> urls = new ArrayList<>();
        final List<Parking> parkings;
        ProviderException failure;

        FakeProvider(List<Parking> parkings) {
            this.parkings = parkings;
        }

        @Override
        public String type() {
            return "poitiers";
        }

        @Override
        public List<Parking> findAll(ProviderConfig config) {
            urls.add(config.baseUrl());
            if (failure != null) {
                throw failure;
            }
            return parkings;
        }
    }
}
