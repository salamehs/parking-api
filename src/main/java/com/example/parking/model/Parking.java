package com.example.parking.model;

import lombok.Builder;
import lombok.Value;

/**
 * Internal parking model.
 *
 * <p>Provider mappers convert their own payloads to this type, then the service computes the
 * distance and filters the results before the public response is built.
 */
@Value
@Builder(toBuilder = true)
public class Parking {

    String name;
    double latitude;
    double longitude;
    Integer distanceMeters;
    Integer availableSpaces;
    Integer totalSpaces;
}
