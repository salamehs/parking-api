package com.example.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/**
 * One parking returned by the public API.
 *
 * <p>This is the stable JSON contract used by web and mobile clients.
 */
@Value
@Builder
@Schema(name = "Parking", description = "A parking near the searched position")
public class ParkingDto {

    @Schema(
        description = "Parking name shown to the user",
        example = "THEATRE",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String name;

    @Schema(
        description = "Parking latitude in WGS 84",
        example = "46.58383455409422",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    double latitude;

    @Schema(
        description = "Parking longitude in WGS 84",
        example = "0.33779491061805567",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    double longitude;

    @Schema(
        description = "Distance from the search point in meters, computed by this API",
        example = "8",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    int distanceMeters;

    @Schema(
        description = "Number of free spaces right now, if the provider gives it",
        example = "93",
        nullable = true
    )
    Integer availableSpaces;

    @Schema(
        description = "Total number of spaces, if the provider gives it",
        example = "320",
        nullable = true
    )
    Integer totalSpaces;
}