package com.epam.module3.sportapi.client;

/**
 * Thrown when the Rakuten API cannot be called, or answers with an error or an unexpected body.
 */
public class RakutenApiException extends RuntimeException {

    public RakutenApiException(String message) {
        super(message);
    }
}
