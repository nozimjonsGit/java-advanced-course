package com.epam.module3.sportapi.client;

import com.epam.module3.sportapi.config.WikidataProperties;
import com.epam.module3.sportapi.entity.Sport;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

/**
 * Queries the <a href="https://query.wikidata.org">Wikidata Query Service</a> for sports.
 */
@Component
public class WikidataSportClient {

    private static final Logger log = LoggerFactory.getLogger(WikidataSportClient.class);

    static final String SPARQL_PATH = "/sparql";

    /**
     * Items that are a type of sport (Q31629), with their English name. The best-known come first:
     * those that the most Wikipedia editions have an article about. Names longer than
     * {@link Sport#MAX_NAME_LENGTH} are left out.
     */
    static final String SPORTS_QUERY = """
            SELECT ?name WHERE {
              ?sport wdt:P31 wd:Q31629 ;
                     rdfs:label ?name ;
                     wikibase:sitelinks ?sitelinks .
              FILTER(LANG(?name) = "en" && STRLEN(?name) <= %d)
            }
            ORDER BY DESC(?sitelinks) ?name
            LIMIT %d
            """;

    private final WebClient webClient;
    private final WikidataProperties properties;

    public WikidataSportClient(WebClient.Builder webClientBuilder, WikidataProperties properties) {
        this.webClient = webClientBuilder
                .baseUrl(properties.baseUrl().toString())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .build();
        this.properties = properties;
    }

    /**
     * Emits the names of up to {@code app.wikidata.limit} sports, best-known first. When the
     * service answers 429 Too Many Requests, waits and tries again, up to
     * {@code app.wikidata.rate-limit-retries} times.
     *
     * @throws ExternalApiException, as an error signal, if the service answers with an error or a
     *                              body without results. A {@link RateLimitedException} if it still
     *                              answers 429 after the last retry
     */
    public Flux<String> findSportNames() {
        String query = SPORTS_QUERY.formatted(Sport.MAX_NAME_LENGTH, properties.limit());
        return webClient.get()
                .uri(uri -> uri.path(SPARQL_PATH)
                        // Sets the format of the results. An Accept header could too, but the
                        // service has rate-limited requests that sent one to 1 a minute
                        .queryParam("format", "json")
                        .queryParam("query", "{query}")
                        // A variable, unlike a literal value, is fully encoded
                        .build(Map.of("query", query)))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> ErrorResponses.toException("Wikidata sport query", response))
                .bodyToMono(SparqlResponse.class)
                .timeout(properties.timeout())
                .retryWhen(retryWhenRateLimited())
                .flatMapIterable(WikidataSportClient::rows)
                .map(row -> row.name().value());
    }

    private Retry retryWhenRateLimited() {
        return Retry.fixedDelay(properties.rateLimitRetries(), properties.rateLimitRetryDelay())
                .filter(RateLimitedException.class::isInstance)
                .doBeforeRetry(signal -> log.warn("Wikidata is rate-limiting requests, trying again in {} ({} of {})",
                        properties.rateLimitRetryDelay(), signal.totalRetries() + 1, properties.rateLimitRetries()))
                // Fail with the last 429, not with a RetryExhaustedException that hides it
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }

    private static List<SparqlResponse.Row> rows(SparqlResponse response) {
        if (response.results() == null || response.results().bindings() == null) {
            throw new ExternalApiException("Wikidata sport query response has no results");
        }
        return response.results().bindings();
    }
}
