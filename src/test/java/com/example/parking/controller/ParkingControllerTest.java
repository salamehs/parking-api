package com.example.parking.controller;

import com.example.parking.exception.CityNotCoveredException;
import com.example.parking.exception.ProviderException;
import com.example.parking.mapper.ParkingMapperImpl;
import com.example.parking.model.Parking;
import com.example.parking.model.SearchCriteria;
import com.example.parking.service.ParkingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ParkingController.class)
@Import(ParkingMapperImpl.class)
class ParkingControllerTest {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String WEB_ORIGIN = "http://localhost:3000"; // app.cors.allowed-origins in application.yml

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParkingService parkingService;

    private ResultActions search(String query) throws Exception {
        return mockMvc.perform(get("/api/v1/parkings" + query));
    }

    @Test
    void returnsTheParkingsOfTheService() throws Exception {
        Parking theatre = Parking.builder().name("THEATRE").latitude(46.5838).longitude(0.3378)
            .distanceMeters(8).availableSpaces(93).totalSpaces(320).build();
        when(parkingService.findNearby(new SearchCriteria(46.5838, 0.3377, 500))).thenReturn(List.of(theatre));

        search("?lat=46.5838&lon=0.3377&radius=500")
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.parkings[0].name").value("THEATRE"))
            .andExpect(jsonPath("$.parkings[0].latitude").value(46.5838))
            .andExpect(jsonPath("$.parkings[0].longitude").value(0.3378))
            .andExpect(jsonPath("$.parkings[0].distanceMeters").value(8))
            .andExpect(jsonPath("$.parkings[0].availableSpaces").value(93))
            .andExpect(jsonPath("$.parkings[0].totalSpaces").value(320));
    }

    @Test
    void radiusDefaultsTo1000Meters() throws Exception {
        when(parkingService.findNearby(any())).thenReturn(List.of());

        search("?lat=46.5838&lon=0.3377").andExpect(status().isOk()).andExpect(jsonPath("$.parkings").isEmpty());

        verify(parkingService).findNearby(new SearchCriteria(46.5838, 0.3377, 1000));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
        "?lat=95&lon=0.3              | lat must be between -90 and 90",
        "?lat=-90.1&lon=0.3           | lat must be between -90 and 90",
        "?lat=NaN&lon=0.3             | lat must be between -90 and 90",
        "?lat=46.5&lon=181            | lon must be between -180 and 180",
        "?lat=46.5&lon=0.3&radius=0    | radius must be between 1 and 5000",
        "?lat=46.5&lon=0.3&radius=5001 | radius must be between 1 and 5000",
        "?lat=46.5                    | lon",
        "?lon=0.3                     | lat",
        "?lat=abc&lon=0.3             | lat",
        "?lat=46.5&lon=0.3&radius=1.5  | radius"})
    void invalidParameterGivesA400NamingTheParameter(String query, String expectedDetail) throws Exception {
        search(query)
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail", containsString(expectedDetail)));

        verifyNoInteractions(parkingService);
    }

    @Test
    void positionOutsideEveryCityGivesA404() throws Exception {
        when(parkingService.findNearby(any())).thenThrow(new CityNotCoveredException());

        search("?lat=48.8566&lon=2.3522")
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("City not covered"))
            .andExpect(jsonPath("$.detail").value("No supported city at this position"));
    }

    @Test
    void providerFailureGivesA502WithoutInternalDetails() throws Exception {
        when(parkingService.findNearby(any())).thenThrow(new ProviderException(
            ProviderException.Kind.UNAVAILABLE, "500 from https://internal.provider/secret", null));

        search("?lat=46.5838&lon=0.3377")
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.title").value("Provider failure"))
            .andExpect(content().string(not(containsString("internal.provider"))));
    }

    @Test
    void providerTimeoutGivesA504() throws Exception {
        when(parkingService.findNearby(any()))
            .thenThrow(new ProviderException(ProviderException.Kind.TIMEOUT, "Read timed out", null));

        search("?lat=46.5838&lon=0.3377")
            .andExpect(status().isGatewayTimeout())
            .andExpect(jsonPath("$.title").value("Provider timeout"));
    }

    @Test
    void unexpectedErrorGivesAGeneric500ProblemWithoutInternalDetails() throws Exception {
        when(parkingService.findNearby(any())).thenThrow(new IllegalStateException("secret bug detail"));

        search("?lat=46.5838&lon=0.3377")
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.title").value("Internal Server Error"))
            .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
            .andExpect(content().string(not(containsString("secret bug detail"))));
    }

    @Test
    void browserPreflightFromTheWebAppOriginSucceeds() throws Exception {
        mockMvc.perform(options("/api/v1/parkings")
                .header("Origin", WEB_ORIGIN)
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", WEB_ORIGIN));
    }

    @Test
    void browserPreflightFromAnotherOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/v1/parkings")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden());
    }

    @Test
    void webAppResponseCarriesTheCorsHeader() throws Exception {
        when(parkingService.findNearby(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/parkings?lat=46.5838&lon=0.3377").header("Origin", WEB_ORIGIN))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", WEB_ORIGIN));
    }

    @Test
    void mobileAppWithoutOriginHeaderIsServed() throws Exception {
        when(parkingService.findNearby(any())).thenReturn(List.of());

        search("?lat=46.5838&lon=0.3377")
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void unsupportedMethodGivesA405() throws Exception {
        mockMvc.perform(post("/api/v1/parkings"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }
}
