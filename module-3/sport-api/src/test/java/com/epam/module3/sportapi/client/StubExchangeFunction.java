package com.epam.module3.sportapi.client;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * Answers requests with prepared responses, in order, and records the requests, so a WebClient can
 * be tested without a server. Once the prepared responses run out, the last one answers every
 * further request.
 */
public final class StubExchangeFunction implements ExchangeFunction {

    private record StubResponse(HttpStatus status, MediaType contentType, String body) {
    }

    private final List<StubResponse> responses = new ArrayList<>();
    private final List<ClientRequest> requests = new ArrayList<>();

    private StubExchangeFunction() {
    }

    public static StubExchangeFunction json(HttpStatus status, String body) {
        return of(status, MediaType.APPLICATION_JSON, body);
    }

    public static StubExchangeFunction html(HttpStatus status, String body) {
        return of(status, MediaType.TEXT_HTML, body);
    }

    public static StubExchangeFunction of(HttpStatus status, MediaType contentType, String body) {
        return new StubExchangeFunction().then(status, contentType, body);
    }

    /** Adds the response to the next request. */
    public StubExchangeFunction then(HttpStatus status, MediaType contentType, String body) {
        responses.add(new StubResponse(status, contentType, body));
        return this;
    }

    @Override
    public Mono<ClientResponse> exchange(ClientRequest request) {
        StubResponse response = responses.get(Math.min(requests.size(), responses.size() - 1));
        requests.add(request);
        return Mono.just(ClientResponse.create(response.status())
                .header(HttpHeaders.CONTENT_TYPE, response.contentType().toString())
                .body(response.body())
                .build());
    }

    public List<ClientRequest> requests() {
        return requests;
    }
}
