package com.example.parking.provider;

import com.example.parking.dto.provider.poitiers.PoitiersResponse;
import com.example.parking.exception.ProviderException;
import com.example.parking.exception.ProviderException.Kind;
import com.example.parking.model.ProviderConfig;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderHttpClientTest {

    private static final int MAX_RETRIES = 2;
    @RegisterExtension
    static WireMockExtension provider = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();
    private final ProviderHttpClient httpClient = new ProviderHttpClient(MAX_RETRIES, Duration.ofMillis(10));

    private static ResponseDefinitionBuilder json(String body) {
        return aResponse().withHeader("Content-Type", "application/json").withBody(body);
    }

    private ProviderConfig config(Duration timeout) {
        return new ProviderConfig(provider.getRuntimeInfo().getHttpBaseUrl() + "/parkings", timeout);
    }

    private void stub(ResponseDefinitionBuilder response) {
        provider.stubFor(get(urlPathEqualTo("/parkings")).willReturn(response));
    }

    private void assertFailsWith(Kind expected, ProviderConfig config) {
        assertThatThrownBy(() -> httpClient.get(config, PoitiersResponse.class))
            .isInstanceOfSatisfying(ProviderException.class,
                exception -> assertThat(exception.getKind()).isEqualTo(expected));
    }

    @Test
    void readsTheJsonBody() {
        stub(json("{\"total\":2,\"results\":[]}"));

        assertThat(httpClient.get(config(Duration.ofSeconds(2)), PoitiersResponse.class).getTotal()).isEqualTo(2);
    }

    @Test
    void repeatedCallsWithTheSameConfigurationWork() {
        stub(json("{\"total\":0}"));
        ProviderConfig config = config(Duration.ofSeconds(2));

        httpClient.get(config, PoitiersResponse.class);
        httpClient.get(config, PoitiersResponse.class);

        provider.verify(2, getRequestedFor(urlPathEqualTo("/parkings")));
    }

    @Test
    void transientProviderErrorIsRetriedUntilItSucceeds() {
        provider.stubFor(get(urlPathEqualTo("/parkings")).inScenario("flaky")
            .whenScenarioStateIs(Scenario.STARTED).willSetStateTo("recovered")
            .willReturn(aResponse().withStatus(503)));
        provider.stubFor(get(urlPathEqualTo("/parkings")).inScenario("flaky")
            .whenScenarioStateIs("recovered")
            .willReturn(json("{\"total\":1,\"results\":[]}")));

        assertThat(httpClient.get(config(Duration.ofSeconds(2)), PoitiersResponse.class).getTotal()).isEqualTo(1);
        provider.verify(2, getRequestedFor(urlPathEqualTo("/parkings")));
    }

    @ParameterizedTest
    @ValueSource(ints = {502, 503, 504})
    void persistentGatewayErrorIsRetriedThenUnavailable(int status) {
        stub(aResponse().withStatus(status));

        assertFailsWith(Kind.UNAVAILABLE, config(Duration.ofSeconds(2)));
        provider.verify(1 + MAX_RETRIES, getRequestedFor(urlPathEqualTo("/parkings")));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 500})
    void otherHttpErrorIsUnavailableWithoutRetry(int status) {
        stub(aResponse().withStatus(status));

        assertFailsWith(Kind.UNAVAILABLE, config(Duration.ofSeconds(2)));
        provider.verify(1, getRequestedFor(urlPathEqualTo("/parkings")));
    }

    @Test
    void unreadableBodyIsUnavailable() {
        stub(json("{not json"));

        assertFailsWith(Kind.UNAVAILABLE, config(Duration.ofSeconds(2)));
        provider.verify(1, getRequestedFor(urlPathEqualTo("/parkings"))); // not retried
    }

    @Test
    void emptyBodyIsUnavailable() {
        stub(aResponse().withStatus(204));

        assertFailsWith(Kind.UNAVAILABLE, config(Duration.ofSeconds(2)));
    }

    @Test
    void slowAnswerIsTimeout() {
        stub(json("{\"total\":0}").withFixedDelay(1500));

        assertFailsWith(Kind.TIMEOUT, config(Duration.ofMillis(300)));
        provider.verify(1 + MAX_RETRIES, getRequestedFor(urlPathEqualTo("/parkings")));
    }

    @Test
    void unreachableProviderIsUnavailable() {
        assertFailsWith(Kind.UNAVAILABLE, new ProviderConfig("http://localhost:1/parkings", Duration.ofSeconds(2)));
    }
}
