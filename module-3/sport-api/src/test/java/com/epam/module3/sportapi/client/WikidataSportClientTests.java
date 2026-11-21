package com.epam.module3.sportapi.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.epam.module3.sportapi.config.WikidataProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.test.StepVerifier;

class WikidataSportClientTests {

    /** Content type the service sends the results with. */
    private static final MediaType SPARQL_RESULTS_JSON = MediaType.parseMediaType("application/sparql-results+json");

    private static final WikidataProperties PROPERTIES = new WikidataProperties(
            URI.create("https://wikidata.test"), "sport-api-tests/1.0", 500, Duration.ofSeconds(5), 2, Duration.ofMillis(1));

    @Test
    void queriesTypesOfSportBestKnownFirst() {
        StubExchangeFunction exchange = sparqlResults(sportsResponse());

        client(exchange).findSportNames().blockLast();

        ClientRequest request = exchange.requests().getFirst();
        assertThat(request.method()).isEqualTo(HttpMethod.GET);
        assertThat(request.url().getHost()).isEqualTo("wikidata.test");
        assertThat(request.url().getPath()).isEqualTo("/sparql");
        assertThat(UriComponentsBuilder.fromUri(request.url()).build().getQueryParams().getFirst("format"))
                .isEqualTo("json");
        assertThat(request.url().getQuery())
                .contains("?sport wdt:P31 wd:Q31629")
                .contains("FILTER(LANG(?name) = \"en\" && STRLEN(?name) <= 100)")
                .contains("ORDER BY DESC(?sitelinks) ?name")
                .contains("LIMIT 500");
    }

    @Test
    void namesItselfAndSendsNoAcceptHeader() {
        StubExchangeFunction exchange = sparqlResults(sportsResponse());

        client(exchange).findSportNames().blockLast();

        HttpHeaders headers = exchange.requests().getFirst().headers();
        assertThat(headers.getFirst(HttpHeaders.USER_AGENT)).isEqualTo("sport-api-tests/1.0");
        assertThat(headers.getAccept()).isEmpty();
    }

    @Test
    void encodesQuery() {
        StubExchangeFunction exchange = sparqlResults(sportsResponse());

        client(exchange).findSportNames().blockLast();

        assertThat(exchange.requests().getFirst().url().getRawQuery())
                .contains("query=SELECT%20%3Fname%20WHERE%20%7B");
    }

    @Test
    void emitsNamesInOrderOfResponse() {
        StepVerifier.create(client(sparqlResults(sportsResponse())).findSportNames())
                .expectNext("association football", "chess", "tennis")
                .verifyComplete();
    }

    @Test
    void emitsNothingWhenQueryFindsNothing() {
        String body = """
                {"head": {"vars": ["name"]}, "results": {"bindings": []}}""";

        StepVerifier.create(client(sparqlResults(body)).findSportNames()).verifyComplete();
    }

    @Test
    void failsWhenResponseHasNoResults() {
        StepVerifier.create(client(sparqlResults("{\"head\": {}}")).findSportNames())
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(ExternalApiException.class)
                        .hasMessage("Wikidata sport query response has no results"))
                .verify();
    }

    @Test
    void triesAgainWhenRateLimited() {
        StubExchangeFunction exchange = StubExchangeFunction
                .of(HttpStatus.TOO_MANY_REQUESTS, MediaType.TEXT_HTML, "Aggressively rate-limiting to 1 req / min")
                .then(HttpStatus.OK, SPARQL_RESULTS_JSON, sportsResponse());

        StepVerifier.create(client(exchange).findSportNames())
                .expectNext("association football", "chess", "tennis")
                .verifyComplete();
        assertThat(exchange.requests()).hasSize(2);
    }

    @Test
    void failsWithLastRateLimitWhenRetriesRunOut() {
        StubExchangeFunction exchange = StubExchangeFunction.html(HttpStatus.TOO_MANY_REQUESTS, "Slow down");

        StepVerifier.create(client(exchange).findSportNames())
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(RateLimitedException.class)
                        .hasMessage("Wikidata sport query failed with status 429: Slow down"))
                .verify();
        assertThat(exchange.requests()).hasSize(3);
    }

    @Test
    void doesNotTryAgainAfterOtherErrors() {
        StubExchangeFunction exchange = StubExchangeFunction.html(HttpStatus.SERVICE_UNAVAILABLE, "Down");

        StepVerifier.create(client(exchange).findSportNames())
                .expectErrorSatisfies(error -> assertThat(error)
                        .isExactlyInstanceOf(ExternalApiException.class)
                        .hasMessage("Wikidata sport query failed with status 503: Down"))
                .verify();
        assertThat(exchange.requests()).hasSize(1);
    }

    private static WikidataSportClient client(StubExchangeFunction exchange) {
        return new WikidataSportClient(WebClient.builder().exchangeFunction(exchange), PROPERTIES);
    }

    private static StubExchangeFunction sparqlResults(String body) {
        return StubExchangeFunction.of(HttpStatus.OK, SPARQL_RESULTS_JSON, body);
    }

    /** A sample of a real response: the three best-known sports. */
    private static String sportsResponse() {
        try (InputStream in = WikidataSportClientTests.class.getResourceAsStream("/wikidata/sports.json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
