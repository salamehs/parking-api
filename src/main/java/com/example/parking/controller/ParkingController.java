package com.example.parking.controller;

import com.example.parking.dto.ParkingsResponse;
import com.example.parking.mapper.ParkingMapper;
import com.example.parking.model.SearchCriteria;
import com.example.parking.service.ParkingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for nearby parking searches.
 *
 * <p>The client provides a position and an optional radius. The controller validates the input,
 * asks the service for the nearby parkings, and returns the public API response.
 */
@RestController
@RequestMapping("/api/v1/parkings")
@RequiredArgsConstructor
@Tag(name = "Parking API", description = "Search for parking facilities near a location")
public class ParkingController {

    private final ParkingService parkingService;
    private final ParkingMapper parkingMapper;

    @Operation(
        operationId = "findNearbyParkings",
        summary = "Find nearby parking facilities",
        description = """
            Searches for parking facilities within the requested radius of the supplied coordinates,
            ordered from nearest to farthest. The radius defaults to 1,000 meters and can be set from
            1 to 5,000 meters. The city is determined from the coordinates. An error is returned
            if the request does not include coordinates or if no city is found for the supplied
            coordinates.""")
    @ApiResponse(responseCode = "200", description = "Parkings within the radius, nearest first (may be empty)")
    @GetMapping
    public ParkingsResponse findNearby(
        @Parameter(description = "Latitude of the search center (WGS 84)", example = "46.5838")
        @RequestParam("lat")
        @DecimalMin(value = "-90", message = "lat must be between -90 and 90")
        @DecimalMax(value = "90", message = "lat must be between -90 and 90")
        double latitude,

        @Parameter(description = "Longitude of the search center (WGS 84)", example = "0.3377")
        @RequestParam("lon")
        @DecimalMin(value = "-180", message = "lon must be between -180 and 180")
        @DecimalMax(value = "180", message = "lon must be between -180 and 180")
        double longitude,

        @Parameter(description = "Search radius in meters", example = "1000")
        @RequestParam(name = "radius", defaultValue = "1000")
        @Min(value = 1, message = "radius must be between 1 and 5000")
        @Max(value = 5000, message = "radius must be between 1 and 5000")
        int radiusMeters) {
        SearchCriteria criteria = new SearchCriteria(latitude, longitude, radiusMeters);
        return parkingMapper.toResponse(parkingService.findNearby(criteria));
    }
}
