package com.epam.module3.sportapi.client;

import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

/**
 * Turns error responses of external APIs into exceptions that say what failed and why.
 */
final class ErrorResponses {

    /** Longest part of an error body that goes into the exception message. */
    private static final int MAX_BODY_LENGTH = 500;

    private ErrorResponses() {
    }

    /**
     * @param request what was requested, such as "Rakuten item search"
     * @return an exception with the request, the status and the start of the body, on one line
     */
    static Mono<ExternalApiException> toException(String request, ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new ExternalApiException("%s failed with status %d: %s"
                        .formatted(request, response.statusCode().value(), abbreviate(body))));
    }

    private static String abbreviate(String body) {
        String oneLine = body.strip().replaceAll("\\s+", " ");
        return oneLine.length() <= MAX_BODY_LENGTH ? oneLine : oneLine.substring(0, MAX_BODY_LENGTH) + "...";
    }
}
