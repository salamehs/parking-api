package com.example.parking.service;

import com.example.parking.city.CityRegistry;
import com.example.parking.exception.CityNotCoveredException;
import com.example.parking.exception.ProviderException;
import com.example.parking.model.City;
import com.example.parking.model.Parking;
import com.example.parking.model.SearchCriteria;
import com.example.parking.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Service that finds nearby parkings for a search position.
 *
 * <p>It finds the city from the position, loads that city's parkings, computes the distance for
 * each parking, filters by radius, and sorts the results by distance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingService {

    private final CityRegistry cityRegistry;
    private final CityParkingsCache cityParkings;

    private static int distanceMeters(SearchCriteria criteria, Parking parking) {
        return (int) Math.round(GeoUtils.distanceMeters(
            criteria.latitude(), criteria.longitude(), parking.getLatitude(), parking.getLongitude()));
    }

    /**
     * Returns the parkings near the given position.
     *
     * <p>The city is selected from the search position. The provider list may come from the cache, and
     * the results are filtered and sorted here.
     *
     * @throws CityNotCoveredException if no configured city covers the position
     * @throws ProviderException       if the city's provider fails or times out
     */
    public List<Parking> findNearby(SearchCriteria criteria) {
        City city = cityRegistry.findCovering(criteria.latitude(), criteria.longitude())
            .orElseThrow(CityNotCoveredException::new);
        long startNanos = System.nanoTime();
        List<Parking> fetched;
        try {
            fetched = cityParkings.get(city);
        } catch (ProviderException exception) {
            log.warn("city={} provider={} failed ({}): {}",
                city.id(), city.providerType(), exception.getKind(), exception.getTechnicalMessage());
            throw exception;
        }

        List<Parking> nearby = fetched.stream()
            .map(parking -> parking.toBuilder().distanceMeters(distanceMeters(criteria, parking)).build())
            .filter(parking -> parking.getDistanceMeters() <= criteria.radiusMeters())
            .sorted(Comparator.comparingInt(Parking::getDistanceMeters))
            .toList();

        log.info("city={} provider={} fetched={} returned={} latencyMs={}", city.id(), city.providerType(),
            fetched.size(), nearby.size(), (System.nanoTime() - startNanos) / 1_000_000);
        return nearby;
    }
}
