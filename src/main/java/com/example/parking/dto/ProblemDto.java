package com.example.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * RFC 9457 problem details returned by the API for errors.
 */
@Schema(name = "Problem", description = "RFC 9457 problem details returned on errors")
public record ProblemDto(@Schema(description = "Problem type", example = "about:blank") String type,
                         @Schema(description = "Short error title", example = "Bad Request") String title,
                         @Schema(description = "HTTP status code", example = "400") int status,
                         @Schema(description = "Human-readable error message",
                             example = "Invalid request parameters: lat must be between -90 and 90") String detail,
                         @Schema(description = "Request path", example = "/api/v1/parkings") String instance) {

}
