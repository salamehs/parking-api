package com.example.parking.mapper;

import com.example.parking.dto.provider.poitiers.PoitiersParking;
import com.example.parking.dto.provider.poitiers.PoitiersResponse;
import com.example.parking.model.GeoPoint;
import com.example.parking.model.Parking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;
import java.util.Objects;

/**
 * Maps the Poitiers provider payload to the internal parking model.
 *
 * <p>The provider gives coordinates as text. If a parking has no readable position, it is ignored
 * because it cannot be shown on a map or given a distance.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PoitiersMapper {

    @Mapping(target = "name", source = "result.name")
    @Mapping(target = "latitude", source = "point.latitude")
    @Mapping(target = "longitude", source = "point.longitude")
    @Mapping(target = "availableSpaces", source = "result.availableSpaces")
    @Mapping(target = "totalSpaces", source = "result.capacity")
    @Mapping(target = "distanceMeters", ignore = true)
    Parking toParking(PoitiersParking result, GeoPoint point);

    default List<Parking> toParkings(PoitiersResponse response) {
        if (response == null || response.getResults() == null) {
            return List.of();
        }
        return response.getResults().stream()
            .filter(Objects::nonNull)
            .flatMap(result -> GeoPoint.firstValid(result.getPosition(), result.getFallbackPosition())
                .map(point -> toParking(result, point))
                .stream())
            .toList();
    }
}
