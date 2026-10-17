package com.example.parking.mapper;

import com.example.parking.dto.ParkingDto;
import com.example.parking.dto.ParkingsResponse;
import com.example.parking.model.Parking;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * Maps the internal parking model to the public API response model.
 *
 * <p>This is the only place that knows the public JSON shape of a parking.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ParkingMapper {

    ParkingDto toDto(Parking parking);

    List<ParkingDto> toDtos(List<Parking> parkings);

    default ParkingsResponse toResponse(List<Parking> parkings) {
        return ParkingsResponse.builder().parkings(toDtos(parkings)).build();
    }
}
