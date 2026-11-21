package com.epam.module3.sportapi.client;

import com.epam.module3.sportapi.config.RakutenProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Builds item search responses and clients that receive them, for tests.
 */
public final class RakutenFixtures {

    public static final RakutenProperties PROPERTIES = new RakutenProperties(
            URI.create("https://rakuten.test"), "test-application-id", "test+access/key", 101070, Duration.ofSeconds(5));

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

    public static RakutenItemSearchClient client(StubExchangeFunction exchange) {
        return client(exchange, PROPERTIES);
    }

    public static RakutenItemSearchClient client(StubExchangeFunction exchange, RakutenProperties properties) {
        return new RakutenItemSearchClient(WebClient.builder().exchangeFunction(exchange), properties);
    }
}
