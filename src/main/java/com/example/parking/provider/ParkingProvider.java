package com.example.parking.provider;

import com.example.parking.exception.ProviderException;
import com.example.parking.model.Parking;
import com.example.parking.model.ProviderConfig;

import java.util.List;

/**
 * Provider adapter for one parking data format.
 *
 * <p>Each implementation knows how to call one city's provider and map its payload to the
 * internal parking model.
 */
public interface ParkingProvider {

    /**
     * Value of {@code provider-type} in the city configuration that selects this provider.
     */
    String type();

    /**
     * Loads all parkings for the city from the provider.
     *
     * <p>The provider gets only the connection settings, not the search position. The service handles
     * distance, radius filtering, and sorting.
     */
    List<Parking> findAll(ProviderConfig config);
}
