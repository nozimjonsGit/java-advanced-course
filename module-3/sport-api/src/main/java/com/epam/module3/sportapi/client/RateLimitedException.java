package com.epam.module3.sportapi.client;

/**
 * Thrown when an external API answers with 429 Too Many Requests. Trying again later may work.
 */
public class RateLimitedException extends ExternalApiException {

    public RateLimitedException(String message) {
        super(message);
    }
}
