package com.epam.module3.sportapi.handler;

import com.epam.module3.sportapi.dto.SportResponse;
import com.epam.module3.sportapi.service.SportService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handles the requests that {@link com.epam.module3.sportapi.router.SportRouter} routes here.
 */
@Component
public class SportHandler {

    public static final String SPORT_NAME = "sportName";
    public static final String QUERY = "q";

    private final SportService sportService;

    public SportHandler(SportService sportService) {
        this.sportService = sportService;
    }

    /** Creates a sport named after the path variable, and answers with 201 and the sport. */
    public Mono<ServerResponse> create(ServerRequest request) {
        return sportService.create(request.pathVariable(SPORT_NAME))
                .map(SportResponse::from)
                .flatMap(sport -> ServerResponse.status(HttpStatus.CREATED).bodyValue(sport));
    }

    /** Answers with the sports whose name contains the {@code q} parameter, or all sports without it. */
    public Mono<ServerResponse> search(ServerRequest request) {
        String query = request.queryParam(QUERY).orElse("");
        return ServerResponse.ok()
                .body(sportService.search(query).map(SportResponse::from), SportResponse.class);
    }
}
