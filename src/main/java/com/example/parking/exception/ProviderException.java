package com.example.parking.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;


/**
 * Thrown when a city's provider fails.
 *
 * <p>Spring renders it as a 504 (timeout) or 502 (anything else) with a generic problem detail body;
 * {@code technicalMessage} (provider URL, raw error) is for the logs only.
 */
@Getter
public class ProviderException extends ErrorResponseException {

    public enum Kind {
        /** The provider answered with an error, was unreachable, or returned an unreadable or empty body. */
        UNAVAILABLE,
        /** The provider did not answer within the city's timeout. */
        TIMEOUT
    }

    private final Kind kind;
    private final String technicalMessage;

    public ProviderException(Kind kind, String technicalMessage, Throwable cause) {
        super(kind == Kind.TIMEOUT ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY, cause);
        this.kind = kind;
        this.technicalMessage = technicalMessage;
        if (kind == Kind.TIMEOUT) {
            setTitle("Provider timeout");
            setDetail("The parking data source did not answer in time");
        } else {
            setTitle("Provider failure");
            setDetail("The parking data source is currently unavailable");
        }
    }
}
