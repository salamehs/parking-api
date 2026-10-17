package com.example.parking.provider;

import com.example.parking.exception.ProviderException;
import com.example.parking.exception.ProviderException.Kind;
import com.example.parking.model.ProviderConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP client used to call parking providers.
 *
 * <p>It creates one client per provider config, retries transient failures, and translates every
 * failure into a {@code ProviderException}.
 */
@Component
public class ProviderHttpClient {

    private final Map<ProviderConfig, RestClient> clients = new ConcurrentHashMap<>();
    private final RetryTemplate retryTemplate;

    /**
     * @param maxRetries retries after the first attempt (0 disables retrying)
     * @param delay      wait before the first retry, doubled for each next one
     */
    public ProviderHttpClient(@Value("${app.provider-retry.max-retries:2}") int maxRetries,
                              @Value("${app.provider-retry.delay:200ms}") Duration delay) {
        this.retryTemplate = new RetryTemplate(RetryPolicy.builder()
            .includes(ResourceAccessException.class,
                HttpServerErrorException.BadGateway.class,
                HttpServerErrorException.ServiceUnavailable.class,
                HttpServerErrorException.GatewayTimeout.class)
            .maxRetries(maxRetries)
            .delay(delay)
            .multiplier(2)
            .build());
    }

    private static RestClient createClient(ProviderConfig config) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(config.timeout());
        requestFactory.setReadTimeout(config.timeout());
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    private static boolean isTimeout(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calls the provider and returns the response body.
     *
     * <p>Transient failures are retried. If the provider still fails, or if the body is empty or
     * unreadable, this method throws a {@code ProviderException}.
     */
    public <T> T get(ProviderConfig config, Class<T> responseType) {
        try {
            RestClient client = clients.computeIfAbsent(config, ProviderHttpClient::createClient);
            T body = retryTemplate.invoke(() -> client.get()
                .uri(URI.create(config.baseUrl()))
                .retrieve()
                .body(responseType));
            if (body == null) {
                throw new ProviderException(Kind.UNAVAILABLE, "Empty body from " + config.baseUrl(), null);
            }
            return body;
        } catch (ResourceAccessException exception) {
            Kind kind = isTimeout(exception) ? Kind.TIMEOUT : Kind.UNAVAILABLE;
            throw new ProviderException(kind, exception.getMessage(), exception);
        } catch (RestClientException exception) {
            // HTTP 4xx/5xx from the provider, or a body that could not be read
            throw new ProviderException(Kind.UNAVAILABLE, exception.getMessage(), exception);
        }
    }
}
