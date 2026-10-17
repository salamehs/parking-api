package com.example.parking.service;

import com.example.parking.exception.ProviderException;
import com.example.parking.model.City;
import com.example.parking.model.Parking;
import com.example.parking.provider.ParkingProvider;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cache of the full parking list for one city.
 *
 * <p>The provider returns the whole city, so one cached list is enough for every request. Failures
 * are not cached, and concurrent requests for the same city share a single provider call.
 */
@Component
public class CityParkingsCache {

    private final Map<String, ParkingProvider> providersByType;
    private final Cache<String, List<Parking>> cache;

    @Autowired
    public CityParkingsCache(List<ParkingProvider> providers, @Value("${app.provider-cache.ttl:30s}") Duration ttl) {
        this(providers, ttl, Ticker.systemTicker());
    }

    CityParkingsCache(List<ParkingProvider> providers, Duration ttl, Ticker ticker) {
        this.providersByType = providers.stream()
            .collect(Collectors.toUnmodifiableMap(ParkingProvider::type, Function.identity()));
        this.cache = Caffeine.newBuilder().expireAfterWrite(ttl).ticker(ticker).build();
    }

    /**
     * Returns the city's parkings, from the cache if possible.
     *
     * @throws ProviderException if the provider fails or times out
     */
    public List<Parking> get(City city) {
        return cache.get(city.id(), id -> List.copyOf(providersByType.get(city.providerType())
            .findAll(city.providerConfig())));
    }

    /**
     * Clears the cache so the next request loads fresh data from the providers.
     */
    public void clear() {
        cache.invalidateAll();
    }
}
