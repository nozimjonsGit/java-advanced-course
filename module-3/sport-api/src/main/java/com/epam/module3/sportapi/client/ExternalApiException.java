package com.epam.module3.sportapi.client;

/**
 * Thrown when an external API cannot be called, or answers with an error or an unexpected body.
 */
public class ExternalApiException extends RuntimeException {

    public ExternalApiException(String message) {
        super(message);
    }
}
