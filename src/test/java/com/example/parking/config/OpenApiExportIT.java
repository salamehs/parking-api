package com.example.parking.config;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiExportIT {

    private static final String OPERATION = "$.paths['/api/v1/parkings'].get";

    @Autowired
    MockMvc mvc;

    @Test
    void exportsTheOpenApiSpecification() throws Exception {
        String json = mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String yaml = mvc.perform(get("/v3/api-docs.yaml"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(JsonPath.<String>read(json, "$.openapi")).startsWith("3.");
        assertThat(JsonPath.<String>read(json, "$.info.title")).isEqualTo("Parking API");
        assertThat(JsonPath.<String>read(json, OPERATION + ".operationId")).isEqualTo("findNearbyParkings");
        Map<String, Object> responses = JsonPath.read(json, OPERATION + ".responses");
        assertThat(responses).containsKeys("200", "400", "404", "500", "502", "504");
        assertThat(JsonPath.<String>read(json, OPERATION + ".responses['404'].content['application/problem+json'].schema['$ref']"))
            .isEqualTo("#/components/schemas/Problem");
        assertThat(JsonPath.<Object>read(json, "$.components.schemas.Problem")).isNotNull();
        assertThat(JsonPath.<List<Object>>read(json, OPERATION + ".parameters[?(@.name == 'radius')].schema.maximum"))
            .containsExactly(5000);

        Map<String, Object> paths = JsonPath.read(json, "$.paths");
        assertThat(paths).as("only the public API is documented").hasSize(1);
        assertThat(json).doesNotContain("price").doesNotContain("currency");

        Path outputDir = Path.of(System.getProperty("openapi.output.dir", "docs"));
        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve("openapi.json"), json + System.lineSeparator(), StandardCharsets.UTF_8);
        Files.writeString(outputDir.resolve("openapi.yaml"), yaml, StandardCharsets.UTF_8);
    }
}
