package com.example.parking.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GeoUtilsTest {

    @Test
    void zeroDistanceForSamePoint() {
        assertThat(GeoUtils.distanceMeters(43.58, 7.12, 43.58, 7.12)).isZero();
    }

    @Test
    void oneThousandthOfADegreeOfLatitudeIsAbout111Meters() {
        assertThat(GeoUtils.distanceMeters(43.5800, 7.12, 43.5810, 7.12)).isBetween(110.0, 112.0);
    }

    @Test
    void longitudeDegreesShrinkWithLatitude() {
        assertThat(GeoUtils.distanceMeters(43.58, 7.1200, 43.58, 7.1260)).isBetween(480.0, 487.0);
    }
}
