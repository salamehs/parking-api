package com.example.parking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

/**
 * Thrown when no configured city covers the searched position.
 *
 * <p>Spring renders it as a 404 response with a generic problem detail body.
 */
public class CityNotCoveredException extends ErrorResponseException {

    public CityNotCoveredException() {
        super(HttpStatus.NOT_FOUND);
        setTitle("City not covered");
        setDetail("No supported city at this position");
    }
}
