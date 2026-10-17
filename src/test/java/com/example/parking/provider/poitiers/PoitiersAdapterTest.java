package com.example.parking.provider.poitiers;

import com.example.parking.TestPayloads;
import com.example.parking.mapper.PoitiersMapperImpl;
import com.example.parking.model.Parking;
import com.example.parking.model.ProviderConfig;
import com.example.parking.provider.ProviderHttpClient;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class PoitiersAdapterTest {

    @RegisterExtension
    static WireMockExtension provider = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private final PoitiersAdapter adapter = new PoitiersAdapter(new ProviderHttpClient(0, Duration.ZERO), new PoitiersMapperImpl());

    private ProviderConfig config(String pathAndQuery, Duration timeout) {
        return new ProviderConfig(provider.getRuntimeInfo().getHttpBaseUrl() + pathAndQuery, timeout);
    }

    private void stubOk(String body) {
        provider.stubFor(get(urlPathEqualTo("/lines"))
            .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));
    }

    @Test
    void mapsTheRealPayloadAndDropsTheParkingsWithoutCoordinates() {
        stubOk(TestPayloads.POITIERS);

        List<Parking> parkings = adapter.findAll(config("/lines", Duration.ofSeconds(2)));

        assertThat(parkings).extracting(Parking::getName).containsExactlyInAnyOrder("THEATRE", "NOTRE DAME",
            "ARRET MINUTE", "PALAIS DE JUSTICE", "HOTEL DE VILLE", "GARE TOUMAI", "BLOSSAC TISON");
        Parking theatre = parkings.stream().filter(parking -> parking.getName().equals("THEATRE")).findFirst().orElseThrow();
        assertThat(theatre.getLatitude()).isEqualTo(46.58383455409422);
        assertThat(theatre.getLongitude()).isEqualTo(0.33779491061805567);
        assertThat(theatre.getAvailableSpaces()).isEqualTo(93);
        assertThat(theatre.getTotalSpaces()).isEqualTo(320);
        assertThat(theatre.getDistanceMeters()).isNull();
    }

    @Test
    void callsTheConfiguredUrlAsIsWithoutGeographicParametersOrCredentials() {
        stubOk(TestPayloads.POITIERS);

        adapter.findAll(config("/lines?size=100&where=a%20b", Duration.ofSeconds(2)));

        provider.verify(getRequestedFor(urlPathEqualTo("/lines"))
            .withQueryParam("size", equalTo("100"))
            .withQueryParam("where", equalTo("a b"))
            .withQueryParam("lat", absent())
            .withQueryParam("lon", absent())
            .withQueryParam("radius", absent())
            .withoutHeader("Authorization"));
    }

    @Test
    void emptyOrMissingResultsGiveAnEmptyList() {
        stubOk("{\"total\":0}");

        assertThat(adapter.findAll(config("/lines", Duration.ofSeconds(2)))).isEmpty();
    }
}
