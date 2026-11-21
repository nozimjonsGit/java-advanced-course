package com.epam.module3.sportapi.client;

import static com.epam.module3.sportapi.client.RakutenFixtures.PROPERTIES;
import static com.epam.module3.sportapi.client.RakutenFixtures.client;
import static com.epam.module3.sportapi.client.RakutenFixtures.sportsResponse;
import static org.assertj.core.api.Assertions.assertThat;

import com.epam.module3.sportapi.config.RakutenProperties;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.test.StepVerifier;

class RakutenItemSearchClientTests {

    @Test
    void searchesGenreAndAsksForGenreInformation() {
        StubExchangeFunction exchange = StubExchangeFunction.json(HttpStatus.OK, sportsResponse());

        client(exchange).findChildGenres(101070).blockLast();

        assertThat(exchange.requests()).singleElement().satisfies(request -> {
            assertThat(request.method()).isEqualTo(HttpMethod.GET);
            URI url = request.url();
            assertThat(url.getHost()).isEqualTo("rakuten.test");
            assertThat(url.getPath()).isEqualTo("/ichibams/api/IchibaItem/Search/20220601");
            UriComponents query = UriComponentsBuilder.fromUri(url).build();
            assertThat(query.getQueryParams().toSingleValueMap())
                    .containsEntry("genreId", "101070")
                    .containsEntry("genreInformationFlag", "1")
                    .containsEntry("formatVersion", "2")
                    .containsEntry("format", "json")
                    .containsEntry("hits", "1");
        });
    }

    @Test
    void encodesCredentials() {
        StubExchangeFunction exchange = StubExchangeFunction.json(HttpStatus.OK, sportsResponse());

        client(exchange).findChildGenres(101070).blockLast();

        String query = exchange.requests().getFirst().url().getRawQuery();
        assertThat(query)
                .contains("applicationId=test-application-id")
                .contains("accessKey=test%2Baccess%2Fkey");
    }

    @Test
    void emitsChildGenresOfSearchedGenre() {
        StubExchangeFunction exchange = StubExchangeFunction.json(HttpStatus.OK, sportsResponse());

        StepVerifier.create(client(exchange).findChildGenres(101070))
                .expectNext(new RakutenGenre(101077, "ゴルフ"))
                .expectNext(new RakutenGenre(101078, "テニス"))
                .expectNext(new RakutenGenre(101079, "サッカー・フットサル"))
                .verifyComplete();
    }

    @Test
    void acceptsGenreInformationAsSingleObject() {
        String body = """
                {"GenreInformation": {"children": [{"genreId": 101077, "genreName": "ゴルフ"}]}}""";
        StubExchangeFunction exchange = StubExchangeFunction.json(HttpStatus.OK, body);

        StepVerifier.create(client(exchange).findChildGenres(101070))
                .expectNext(new RakutenGenre(101077, "ゴルフ"))
                .verifyComplete();
    }

    @Test
    void emitsNothingWhenGenreHasNoChildren() {
        String body = """
                {"GenreInformation": [{"parent": [], "current": [], "children": []}]}""";
        StubExchangeFunction exchange = StubExchangeFunction.json(HttpStatus.OK, body);

        StepVerifier.create(client(exchange).findChildGenres(101077)).verifyComplete();
    }

    @Test
    void failsWhenResponseHasNoGenreInformation() {
        StubExchangeFunction exchange = StubExchangeFunction.json(HttpStatus.OK, "{\"Items\": []}");

        StepVerifier.create(client(exchange).findChildGenres(101070))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(RakutenApiException.class)
                        .hasMessage("Rakuten item search response for genre 101070 has no GenreInformation"))
                .verify();
    }

    @Test
    void failsWithStatusAndBodyWhenApiAnswersWithError() {
        String body = """
                {"errors": {"errorCode": 403, "errorMessage": "Invalid Access Key"}}""";
        StubExchangeFunction exchange = StubExchangeFunction.json(HttpStatus.FORBIDDEN, body);

        StepVerifier.create(client(exchange).findChildGenres(101070))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(RakutenApiException.class)
                        .hasMessage("Rakuten item search failed with status 403: " + body))
                .verify();
    }

    @Test
    void putsErrorBodyOnOneLine() {
        String body = """
                {
                  "errors": {
                    "errorCode": 403,
                    "errorMessage": "Invalid Access Key"
                  }
                }
                """;
        StubExchangeFunction exchange = StubExchangeFunction.json(HttpStatus.FORBIDDEN, body);

        StepVerifier.create(client(exchange).findChildGenres(101070))
                .expectErrorSatisfies(error -> assertThat(error).hasMessage(
                        "Rakuten item search failed with status 403: "
                                + "{ \"errors\": { \"errorCode\": 403, \"errorMessage\": \"Invalid Access Key\" } }"))
                .verify();
    }

    @Test
    void shortensLongErrorBody() {
        StubExchangeFunction exchange = StubExchangeFunction.html(HttpStatus.SERVICE_UNAVAILABLE, "x".repeat(600));

        StepVerifier.create(client(exchange).findChildGenres(101070))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(RakutenApiException.class)
                        .hasMessage("Rakuten item search failed with status 503: " + "x".repeat(500) + "..."))
                .verify();
    }

    @Test
    void failsWithoutRequestWhenCredentialsAreMissing() {
        StubExchangeFunction exchange = StubExchangeFunction.json(HttpStatus.OK, sportsResponse());
        RakutenProperties noAccessKey = new RakutenProperties(
                PROPERTIES.baseUrl(), PROPERTIES.applicationId(), " ", PROPERTIES.timeout());

        StepVerifier.create(client(exchange, noAccessKey).findChildGenres(101070))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(RakutenApiException.class)
                        .hasMessage("Rakuten credentials are not set: set RAKUTEN_APPLICATION_ID and RAKUTEN_ACCESS_KEY"))
                .verify();
        assertThat(exchange.requests()).isEmpty();
    }
}
