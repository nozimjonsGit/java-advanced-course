package com.epam.module3.sportapi.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Connection to the Wikidata Query Service, which needs no keys.
 *
 * @param baseUrl               host of the query service
 * @param userAgent             the User-Agent header. Wikimedia asks clients to name themselves,
 *                              and to add a way to contact them if they can
 * @param limit                 how many sports to fetch, best-known first
 * @param timeout               how long to wait for a response
 * @param rateLimitRetries      how many times to try again when the service answers 429 Too Many Requests
 * @param rateLimitRetryDelay   how long to wait before trying again. The service may allow as little
 *                              as one request a minute
 */
@ConfigurationProperties("app.wikidata")
public record WikidataProperties(
        @DefaultValue("https://query.wikidata.org") URI baseUrl,
        @DefaultValue("sport-api/0.0.1 (Spring WebClient)") String userAgent,
        @DefaultValue("500") int limit,
        @DefaultValue("30s") Duration timeout,
        @DefaultValue("2") int rateLimitRetries,
        @DefaultValue("60s") Duration rateLimitRetryDelay) {

    public WikidataProperties {
        if (limit < 1) {
            throw new IllegalArgumentException("app.wikidata.limit must be at least 1, but is " + limit);
        }
        if (rateLimitRetries < 0) {
            throw new IllegalArgumentException(
                    "app.wikidata.rate-limit-retries must not be negative, but is " + rateLimitRetries);
        }
    }
}
