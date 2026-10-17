package com.example.parking.controller;

import com.example.parking.TestPayloads;
import com.example.parking.service.CityParkingsCache;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.lessThan;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ParkingApiIT {

    private static final String PROVIDER_BODY = TestPayloads.POITIERS;
    private static final String QUERY = "?lat=46.5838&lon=0.3377&radius=1000";
    @RegisterExtension
    static WireMockExtension poitiersProvider = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort()).build();
    @RegisterExtension
    static WireMockExtension toursProvider = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort()).build();
    @Autowired
    MockMvc mvc;
    @Autowired
    CityParkingsCache cityParkingsCache;

    @DynamicPropertySource
    static void cities(DynamicPropertyRegistry registry) {
         registry.add("app.cities.poitiers.base-url", () -> poitiersProvider.getRuntimeInfo().getHttpBaseUrl() + "/parkings?size=100");
        registry.add("app.cities.poitiers.timeout", () -> "500ms");
        registry.add("app.provider-retry.delay", () -> "10ms");

        registry.add("app.cities.tours.center-lat", () -> 47.3941);
        registry.add("app.cities.tours.center-lon", () -> 0.6848);
        registry.add("app.cities.tours.radius-meters", () -> 15000);
        registry.add("app.cities.tours.provider-type", () -> "poitiers");
        registry.add("app.cities.tours.base-url", () -> toursProvider.getRuntimeInfo().getHttpBaseUrl() + "/tours-parkings");
    }

    @BeforeEach
    void forgetCachedParkings() {
        cityParkingsCache.clear();
    }

    @Test
    void secondSearchIsServedFromTheCacheWithoutCallingTheProviderAgain() throws Exception {
        stubProvider(PROVIDER_BODY);

        search(QUERY).andExpect(status().isOk()).andExpect(jsonPath("$.parkings.length()").value(6));
        search("?lat=46.5838&lon=0.3377&radius=100")
            .andExpect(status().isOk()).andExpect(jsonPath("$.parkings.length()").value(1));

        poitiersProvider.verify(1, getRequestedFor(urlPathEqualTo("/parkings")));
    }

    @Test
    void providerFailureIsNotCached() throws Exception {
        poitiersProvider.stubFor(get(urlPathEqualTo("/parkings")).willReturn(aResponse().withStatus(500)));
        search(QUERY).andExpect(status().isBadGateway());

        stubProvider(PROVIDER_BODY);
        search(QUERY).andExpect(status().isOk());
    }

    private void stubProvider(String body) {
        poitiersProvider.stubFor(get(urlPathEqualTo("/parkings")).willReturn(
            aResponse().withHeader("Content-Type", "application/json").withBody(body)));
    }

    private ResultActions search(String query) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get("/api/v1/parkings" + query));
    }

    @Test
    void returnsNearbyParkingsSortedByDistanceWithMapCoordinates() throws Exception {
        stubProvider(PROVIDER_BODY);

        search(QUERY)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parkings.length()").value(6))
            .andExpect(jsonPath("$.parkings[*].name").value(contains("THEATRE", "ARRET MINUTE", "GARE TOUMAI",
                "HOTEL DE VILLE", "NOTRE DAME", "BLOSSAC TISON")))
            .andExpect(jsonPath("$.parkings[0].id").doesNotExist()) // no identifier in the contract
            .andExpect(jsonPath("$.parkings[0].latitude").value(46.58383455409422))
            .andExpect(jsonPath("$.parkings[0].longitude").value(0.33779491061805567))
            .andExpect(jsonPath("$.parkings[0].distanceMeters", allOf(greaterThan(0), lessThan(20))))
            .andExpect(jsonPath("$.parkings[0].availableSpaces").value(93))
            .andExpect(jsonPath("$.parkings[0].totalSpaces").value(320))
            .andExpect(jsonPath("$.parkings[1].distanceMeters", allOf(greaterThan(195), lessThan(220))))
            .andExpect(jsonPath("$.parkings[5].distanceMeters", allOf(greaterThan(960), lessThan(990))));

        poitiersProvider.verify(getRequestedFor(urlPathEqualTo("/parkings"))
            .withQueryParam("size", equalTo("100"))
            .withQueryParam("lat", absent())
            .withQueryParam("lon", absent())
            .withQueryParam("radius", absent())
            .withoutHeader("Authorization"));
    }

    @Test
    void smallerRadiusReturnsOnlyTheParkingsInside() throws Exception {
        stubProvider(PROVIDER_BODY);

        search("?lat=46.5838&lon=0.3377&radius=100")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parkings.length()").value(1))
            .andExpect(jsonPath("$.parkings[0].name").value("THEATRE"));
    }

    @Test
    void eachCityIsServedByItsOwnProvider() throws Exception {
        stubProvider(PROVIDER_BODY);
        toursProvider.stubFor(get(urlPathEqualTo("/tours-parkings")).willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("{\"total\":1,\"results\":[{\"_geopoint\":\"47.3941, 0.6850\","
                + "\"Capacite\":500,\"Nom\":\"TOURS CENTRE\",\"Places\":12}]}")));

        search("?lat=47.3940&lon=0.6849&radius=1000")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parkings.length()").value(1))
            .andExpect(jsonPath("$.parkings[0].name").value("TOURS CENTRE"))
            .andExpect(jsonPath("$.parkings[0].availableSpaces").value(12))
            .andExpect(jsonPath("$.parkings[0].totalSpaces").value(500));

        poitiersProvider.verify(0, getRequestedFor(urlPathEqualTo("/parkings")));
    }

    @Test
    void positionInNoCoveredCityGivesA404() throws Exception {
        search("?lat=48.8566&lon=2.3522&radius=1000") // Paris
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void invalidParametersGiveA400() throws Exception {
        search("?lat=95&lon=7.12").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
        search("?lat=43.58&lon=7.12&radius=999999").andExpect(status().isBadRequest());
        search("?lat=43.58").andExpect(status().isBadRequest());
        search("?lat=abc&lon=7.12").andExpect(status().isBadRequest());
    }

    @Test
    void providerFailureBecomesAGeneric502() throws Exception {
        poitiersProvider.stubFor(get(urlPathEqualTo("/parkings")).willReturn(aResponse().withStatus(500)
            .withBody("internal provider stack trace")));

        search(QUERY)
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.status").value(502))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("stack trace"))));
    }

    @Test
    void providerRecoveringAfterA503IsTransparentForTheClient() throws Exception {
        poitiersProvider.stubFor(get(urlPathEqualTo("/parkings")).inScenario("flaky")
            .whenScenarioStateIs(Scenario.STARTED).willSetStateTo("recovered")
            .willReturn(aResponse().withStatus(503)));
        poitiersProvider.stubFor(get(urlPathEqualTo("/parkings")).inScenario("flaky")
            .whenScenarioStateIs("recovered")
            .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(PROVIDER_BODY)));

        search(QUERY).andExpect(status().isOk()).andExpect(jsonPath("$.parkings.length()").value(6));
        poitiersProvider.verify(2, getRequestedFor(urlPathEqualTo("/parkings")));
    }

    @Test
    void unreadableProviderAnswerBecomesA502() throws Exception {
        stubProvider("<html>maintenance</html>");

        search(QUERY).andExpect(status().isBadGateway());
    }

    @Test
    void healthAndDocumentationAreAvailable() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(MockMvcRequestBuilders.get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void providerTimeoutBecomesA504() throws Exception {
        poitiersProvider.stubFor(get(urlPathEqualTo("/parkings")).willReturn(aResponse()
            .withHeader("Content-Type", "application/json").withBody("{\"total\":0,\"results\":[]}")
            .withFixedDelay(1500)));

        search(QUERY)
            .andExpect(status().isGatewayTimeout())
            .andExpect(jsonPath("$.status").value(504));
    }
}
