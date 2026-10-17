package com.example.parking.mapper;

import com.example.parking.dto.provider.poitiers.PoitiersParking;
import com.example.parking.dto.provider.poitiers.PoitiersResponse;
import com.example.parking.model.GeoPoint;
import com.example.parking.model.Parking;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PoitiersMapperTest {

    private final PoitiersMapper mapper = new PoitiersMapperImpl();

    private static PoitiersParking.PoitiersParkingBuilder theatre() {
        return PoitiersParking.builder()
            .name("THEATRE")
            .position("46.58383455409422, 0.33779461805567")
            .fallbackPosition("46.58383409422, 0.33771061805567")
            .capacity(320)
            .availableSpaces(93);
    }

    @Test
    void mapsTheProviderFieldsToTheCanonicalModel() {
        Parking parking = mapper.toParking(theatre().build(), new GeoPoint(46.58383455409422, 0.33779461805567));

        assertThat(parking.getName()).isEqualTo("THEATRE");
        assertThat(parking.getLatitude()).isEqualTo(46.58383455409422);
        assertThat(parking.getLongitude()).isEqualTo(0.33779461805567);
        assertThat(parking.getAvailableSpaces()).isEqualTo(93);
        assertThat(parking.getTotalSpaces()).isEqualTo(320);
        assertThat(parking.getDistanceMeters()).isNull();
    }

    @Test
    void readsLatitudeThenLongitudeFromThePreferredGeopoint() {
        List<Parking> parkings = mapper.toParkings(PoitiersResponse.builder().total(1).results(List.of(theatre().build())).build());

        assertThat(parkings).hasSize(1);
        assertThat(parkings.get(0).getLatitude()).isEqualTo(46.58383455409422);
        assertThat(parkings.get(0).getLongitude()).isEqualTo(0.33779461805567);
    }

    @Test
    void fallsBackToTheSecondPositionFieldWhenThePreferredOneIsUnreadable() {
        PoitiersParking result = theatre().position("not a position").build();

        Parking parking = mapper.toParkings(PoitiersResponse.builder().results(List.of(result)).build()).get(0);

        assertThat(parking.getLatitude()).isEqualTo(46.58383409422);
        assertThat(parking.getLongitude()).isEqualTo(0.33771061805567);
    }

    @Test
    void dropsEntriesWhosePositionCannotBeRead() {
        PoitiersResponse response = PoitiersResponse.builder().results(Arrays.asList(
                theatre().build(),
                theatre().name("No position").position(null).fallbackPosition(null).build(),
                theatre().name("Garbage position").position("abc, def").fallbackPosition("1,2,3").build(),
                null))
            .build();

        assertThat(mapper.toParkings(response)).extracting(Parking::getName).containsExactly("THEATRE");
    }

    @Test
    void keepsEntriesWithoutPlacesInformation() {
        Parking parking = mapper.toParkings(PoitiersResponse.builder()
            .results(List.of(theatre().availableSpaces(null).capacity(null).build())).build()).get(0);

        assertThat(parking.getAvailableSpaces()).isNull();
        assertThat(parking.getTotalSpaces()).isNull();
    }

    @Test
    void nullResponseOrNullListGivesEmptyResult() {
        assertThat(mapper.toParkings(null)).isEmpty();
        assertThat(mapper.toParkings(new PoitiersResponse())).isEmpty();
    }
}
