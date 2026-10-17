package com.example.parking.mapper;

import com.example.parking.dto.ParkingDto;
import com.example.parking.dto.ParkingsResponse;
import com.example.parking.model.Parking;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ParkingMapperTest {

    private final ParkingMapper mapper = new ParkingMapperImpl();

    private final Parking parking = Parking.builder()
        .name("Parking Centre").latitude(43.58).longitude(7.12)
        .distanceMeters(111).availableSpaces(42).totalSpaces(120)
        .build();

    @Test
    void mapsEveryFieldOfThePublicContract() {
        ParkingDto dto = mapper.toDto(parking);

        assertThat(dto).isEqualTo(ParkingDto.builder()
            .name("Parking Centre").latitude(43.58).longitude(7.12)
            .distanceMeters(111).availableSpaces(42).totalSpaces(120)
            .build());
    }

    @Test
    void wrapsTheListInTheResponseEnvelopeKeepingOrder() {
        Parking second = Parking.builder().name("Other").latitude(43.59).longitude(7.13)
            .distanceMeters(500).build();

        ParkingsResponse response = mapper.toResponse(List.of(parking, second));

        assertThat(response.getParkings()).extracting(ParkingDto::getName).containsExactly("Parking Centre", "Other");
    }

    @Test
    void emptyListGivesEmptyEnvelope() {
        assertThat(mapper.toResponse(List.of()).getParkings()).isEmpty();
    }
}
