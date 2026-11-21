package com.epam.module3.sportapi.client;

import com.epam.module3.sportapi.config.RakutenProperties;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Calls the Rakuten Ichiba
 * <a href="https://webservice.rakuten.co.jp/explorer/api/IchibaItem/Search">item search API</a>.
 */
@Component
public class RakutenItemSearchClient {

    static final String ITEM_SEARCH_PATH = "/ichibams/api/IchibaItem/Search/20220601";

    /** Longest part of an error body that goes into the exception message. */
    private static final int MAX_ERROR_BODY_LENGTH = 500;

    private final WebClient webClient;
    private final RakutenProperties properties;

    public RakutenItemSearchClient(WebClient.Builder webClientBuilder, RakutenProperties properties) {
        this.webClient = webClientBuilder.baseUrl(properties.baseUrl().toString()).build();
        this.properties = properties;
    }

    /**
     * Searches the items of a genre and emits the genre's direct child genres, which the response
     * lists in its {@code GenreInformation} block. Only one item is asked for, since the items
     * themselves are not used.
     *
     * @throws RakutenApiException, as an error signal, if the credentials are not set, the API
     *                             answers with an error, or the response has no genre information
     */
    public Flux<RakutenGenre> findChildGenres(int genreId) {
        if (!properties.hasCredentials()) {
            return Flux.error(new RakutenApiException(
                    "Rakuten credentials are not set: set RAKUTEN_APPLICATION_ID and RAKUTEN_ACCESS_KEY"));
        }
        return webClient.get()
                .uri(uri -> uri.path(ITEM_SEARCH_PATH)
                        .queryParam("applicationId", "{applicationId}")
                        .queryParam("accessKey", "{accessKey}")
                        .queryParam("format", "json")
                        .queryParam("formatVersion", 2)
                        .queryParam("genreId", genreId)
                        .queryParam("genreInformationFlag", 1)
                        .queryParam("hits", 1)
                        // Variables, unlike literal values, are fully encoded
                        .build(Map.of(
                                "applicationId", properties.applicationId(),
                                "accessKey", properties.accessKey())))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toApiException)
                .bodyToMono(ItemSearchResponse.class)
                .timeout(properties.timeout())
                .flatMapIterable(response -> childGenres(response, genreId));
    }

    private Mono<RakutenApiException> toApiException(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new RakutenApiException("Rakuten item search failed with status %d: %s"
                        .formatted(response.statusCode().value(), abbreviate(body))));
    }

    private static List<RakutenGenre> childGenres(ItemSearchResponse response, int genreId) {
        List<ItemSearchResponse.GenreInformation> information = response.genreInformation();
        if (information == null || information.isEmpty()) {
            throw new RakutenApiException(
                    "Rakuten item search response for genre %d has no GenreInformation".formatted(genreId));
        }
        List<RakutenGenre> children = information.getFirst().children();
        return children == null ? List.of() : children;
    }

    /** Puts the body on one line, and cuts it to {@value #MAX_ERROR_BODY_LENGTH} characters. */
    private static String abbreviate(String body) {
        String oneLine = body.strip().replaceAll("\\s+", " ");
        return oneLine.length() <= MAX_ERROR_BODY_LENGTH ? oneLine : oneLine.substring(0, MAX_ERROR_BODY_LENGTH) + "...";
    }
}
