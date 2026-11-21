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
 * Answers every request with the same response, and records the requests, so a WebClient can be
 * tested without a server.
 */
public final class StubExchangeFunction implements ExchangeFunction {

    private final HttpStatus status;
    private final MediaType contentType;
    private final String body;
    private final List<ClientRequest> requests = new ArrayList<>();

    private StubExchangeFunction(HttpStatus status, MediaType contentType, String body) {
        this.status = status;
        this.contentType = contentType;
        this.body = body;
    }

    public static StubExchangeFunction json(HttpStatus status, String body) {
        return new StubExchangeFunction(status, MediaType.APPLICATION_JSON, body);
    }

    public static StubExchangeFunction html(HttpStatus status, String body) {
        return new StubExchangeFunction(status, MediaType.TEXT_HTML, body);
    }

    @Override
    public Mono<ClientResponse> exchange(ClientRequest request) {
        requests.add(request);
        return Mono.just(ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, contentType.toString())
                .body(body)
                .build());
    }

    public List<ClientRequest> requests() {
        return requests;
    }
}
