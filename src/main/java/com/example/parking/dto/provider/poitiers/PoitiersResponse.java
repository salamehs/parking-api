package com.example.parking.dto.provider.poitiers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Provider payload: all the parkings of Poitiers, with no geographic filter.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PoitiersResponse {

    /**
     * Parkings available at the provider; larger than {@code results.size()} if the provider paginates.
     */
    private Integer total;
    private List<PoitiersParking> results;
}
