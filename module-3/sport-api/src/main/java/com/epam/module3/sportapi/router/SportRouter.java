package com.epam.module3.sportapi.router;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import com.epam.module3.sportapi.exception.InvalidSportNameException;
import com.epam.module3.sportapi.exception.SportAlreadyExistsException;
import com.epam.module3.sportapi.handler.SportHandler;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Routes the sport API. Errors the client can fix are answered with an RFC 9457 problem document.
 */
@Configuration(proxyBeanMethods = false)
public class SportRouter {

    static final String BASE_PATH = "/api/v1/sport";

    @Bean
    RouterFunction<ServerResponse> sportRoutes(SportHandler handler) {
        return route()
                .GET(BASE_PATH, handler::search)
                .POST(BASE_PATH + "/{" + SportHandler.SPORT_NAME + "}", handler::create)
                .onError(SportAlreadyExistsException.class,
                        (exception, request) -> problem(HttpStatus.CONFLICT, exception, request))
                .onError(InvalidSportNameException.class,
                        (exception, request) -> problem(HttpStatus.BAD_REQUEST, exception, request))
                .build();
    }

    private static Mono<ServerResponse> problem(HttpStatus status, Throwable exception, ServerRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setInstance(URI.create(request.requestPath().value()));
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyValue(problem);
    }
}
