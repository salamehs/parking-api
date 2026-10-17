package com.example.parking.config;

import com.example.parking.dto.ProblemDto;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI document: spec at /v3/api-docs, UI at /swagger-ui.html, exported to docs/ by OpenApiExportIT.
 * The error responses are added to every operation here, so they are identical everywhere and the controller stays
 * short.
 */
@Configuration
public class OpenApiConfig {

    private static final String PROBLEM_SCHEMA = "Problem";
    private static final String PROBLEM_MEDIA_TYPE = "application/problem+json";
    private static final Map<String, List<String>> ERRORS = errors();

    private static ApiResponse problemResponse(int status, String description, String title, String detail) {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("type", "about:blank");
        example.put("title", title);
        example.put("status", status);
        example.put("detail", detail);
        example.put("instance", "/api/v1/parkings");
        return new ApiResponse()
            .description(description)
            .content(new Content().addMediaType(PROBLEM_MEDIA_TYPE, new MediaType()
                .schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM_SCHEMA))
                .example(example)));
    }

    private static Map<String, List<String>> errors() {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        errors.put("400", List.of("Missing, non-numeric or out-of-range parameter",
            "Bad Request", "Invalid request parameters: lat must be between -90 and 90"));
        errors.put("404", List.of("No covered city at this position",
            "City not covered", "No supported city at this position"));
        errors.put("500", List.of("Unexpected error",
            "Internal Server Error", "An unexpected error occurred"));
        errors.put("502", List.of("The city's provider failed or returned an unreadable answer",
            "Provider failure", "The parking data source is currently unavailable"));
        errors.put("504", List.of("The city's provider did not answer in time",
            "Provider timeout", "The parking data source did not answer in time"));
        return errors;
    }

    @Bean
    public OpenAPI parkingOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Parking API")
                .version("v1")
                .description("Returns the nearby parkings, with their position and availability."))
            .components(new Components());
    }

    @Bean
    public OpenApiCustomizer errorResponsesCustomizer() {
        return openApi -> {
            ModelConverters.getInstance().read(ProblemDto.class).forEach(openApi.getComponents()::addSchemas);
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                ERRORS.forEach((status, error) -> operation.getResponses().addApiResponse(status,
                    problemResponse(Integer.parseInt(status), error.get(0), error.get(1), error.get(2))));
            }));
        };
    }
}
