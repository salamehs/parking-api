package com.example.parking.provider.poitiers;

import com.example.parking.dto.provider.poitiers.PoitiersParking;
import com.example.parking.dto.provider.poitiers.PoitiersResponse;
import com.example.parking.mapper.PoitiersMapper;
import com.example.parking.model.GeoPoint;
import com.example.parking.model.Parking;
import com.example.parking.model.ProviderConfig;
import com.example.parking.provider.ParkingProvider;
import com.example.parking.provider.ProviderHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Parking provider for Grand Poitiers.
 *
 * <p>It calls the Poitiers open-data API, maps the provider payload to the internal model, and
 * returns every parking of the city. The service then computes distances, filters by radius, and
 * sorts the results.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PoitiersAdapter implements ParkingProvider {

    public static final String TYPE = "poitiers";

    private final ProviderHttpClient httpClient;
    private final PoitiersMapper poitiersMapper;

    private static List<String> withoutPosition(PoitiersResponse response) {
        return response.getResults().stream()
            .filter(Objects::nonNull)
            .filter(result -> GeoPoint.firstValid(result.getPosition(), result.getFallbackPosition()).isEmpty())
            .map(PoitiersParking::getName)
            .toList();
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public List<Parking> findAll(ProviderConfig config) {
        PoitiersResponse response = httpClient.get(config, PoitiersResponse.class);
        int received = response.getResults() == null ? 0 : response.getResults().size();
        List<Parking> parkings = poitiersMapper.toParkings(response);
        if (parkings.size() < received && log.isDebugEnabled()) {
            log.debug("Provider {}: ignored, no readable coordinates: {}", config.baseUrl(), withoutPosition(response));
        }
        return parkings;
    }
}
