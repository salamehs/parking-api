package com.example.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Response returned by the nearby parking search.
 */
@Value
@Builder
@Schema(name = "ParkingsResponse", description = "List of parkings near the searched position")
public class ParkingsResponse {

    @Schema(
        description = "Parkings sorted from nearest to farthest",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<ParkingDto> parkings;
}
