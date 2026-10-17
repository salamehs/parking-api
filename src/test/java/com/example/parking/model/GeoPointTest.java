package com.example.parking.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeoPointTest {

    @Test
    void parsesLatitudeThenLongitude() {
        assertThat(GeoPoint.parse("46.58383409422, 0.33771061805567"))
            .contains(new GeoPoint(46.58383409422, 0.33771061805567));
    }

    @Test
    void toleratesMissingOrExtraWhitespace() {
        assertThat(GeoPoint.parse("46.5,0.3")).contains(new GeoPoint(46.5, 0.3));
        assertThat(GeoPoint.parse("  46.5 ,   0.3  ")).contains(new GeoPoint(46.5, 0.3));
    }

    @Test
    void parsesNegativeValues() {
        assertThat(GeoPoint.parse("-33.86, -151.21")).contains(new GeoPoint(-33.86, -151.21));
    }

    @Test
    void rejectsUnreadableValues() {
        assertThat(GeoPoint.parse(null)).isEmpty();
        assertThat(GeoPoint.parse("")).isEmpty();
        assertThat(GeoPoint.parse("   ")).isEmpty();
        assertThat(GeoPoint.parse("46.5")).isEmpty();
        assertThat(GeoPoint.parse("46.5, 0.3, 12")).isEmpty();
        assertThat(GeoPoint.parse("abc, def")).isEmpty();
        assertThat(GeoPoint.parse("NaN, 0.3")).isEmpty();
        assertThat(GeoPoint.parse("46.5, Infinity")).isEmpty();
    }

    @Test
    void rejectsOutOfRangeValues() {
        assertThat(GeoPoint.parse("91, 0")).isEmpty();
        assertThat(GeoPoint.parse("0, 181")).isEmpty();
        assertThat(GeoPoint.parse("200, 46")).isEmpty();
    }

    @Test
    void firstValidSkipsUnreadableCandidates() {
        assertThat(GeoPoint.firstValid(null, "bad", "46.5, 0.3", "1, 2")).contains(new GeoPoint(46.5, 0.3));
        assertThat(GeoPoint.firstValid(null, "bad")).isEmpty();
        assertThat(GeoPoint.firstValid()).isEmpty();
    }
}
