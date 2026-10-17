package com.example.parking;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Real provider answers kept under src/test/resources/providers.
 */
public final class TestPayloads {

    public static final String POITIERS = read("/providers/poitiers.json");

    private TestPayloads() {
    }

    private static String read(String resource) {
        try (InputStream input = TestPayloads.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing test resource " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
