package com.example.parking.dto.provider.poitiers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One parking as sent by the provider. Only the fields we use are declared; the others are ignored.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PoitiersParking {

    @JsonProperty("Nom")
    private String name;

    /**
     * Preferred position, as text: "latitude, longitude".
     */
    @JsonProperty("_geopoint")
    private String position;

    /**
     * Same format, used when {@code _geopoint} is missing or unreadable.
     */
    @JsonProperty("infos_parkingsgeo_point")
    private String fallbackPosition;

    @JsonProperty("Capacite")
    private Integer capacity;

    @JsonProperty("Places")
    private Integer availableSpaces;
}
