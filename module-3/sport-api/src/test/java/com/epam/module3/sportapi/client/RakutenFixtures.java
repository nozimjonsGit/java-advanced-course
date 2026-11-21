package com.epam.module3.sportapi.client;

import com.epam.module3.sportapi.config.RakutenProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Builds item search responses and clients that receive them, for tests.
 */
public final class RakutenFixtures {

    public static final RakutenProperties PROPERTIES = new RakutenProperties(
            URI.create("https://rakuten.test"), "test-application-id", "test+access/key", Duration.ofSeconds(5));

    private RakutenFixtures() {
    }

    /** The sample response: three child genres of Sports &amp; Outdoors. */
    public static String sportsResponse() {
        try (InputStream in = RakutenFixtures.class.getResourceAsStream("/rakuten/item-search-sports.json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A response whose genre has the given child genres, named after themselves. */
    public static String responseWithChildGenres(List<String> names) {
        String children = names.stream()
                .map(name -> "{\"genreId\": %d, \"genreName\": \"%s\", \"genreLevel\": 2}"
                        .formatted(200000 + names.indexOf(name), name))
                .collect(Collectors.joining(","));
        return "{\"Items\": [], \"GenreInformation\": [{\"parent\": [], \"current\": [], \"children\": [%s]}]}"
                .formatted(children);
    }

    public static RakutenItemSearchClient client(StubExchangeFunction exchange) {
        return client(exchange, PROPERTIES);
    }

    public static RakutenItemSearchClient client(StubExchangeFunction exchange, RakutenProperties properties) {
        return new RakutenItemSearchClient(WebClient.builder().exchangeFunction(exchange), properties);
    }
}
