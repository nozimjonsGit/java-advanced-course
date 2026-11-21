package com.epam.module3.sportapi.source;

import static com.epam.module3.sportapi.client.RakutenFixtures.client;
import static com.epam.module3.sportapi.client.RakutenFixtures.sportsResponse;
import static org.assertj.core.api.Assertions.assertThat;

import com.epam.module3.sportapi.client.StubExchangeFunction;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.test.StepVerifier;

class RakutenSportSourceTests {

    @Test
    void namesSportsAfterChildGenresOfConfiguredGenre() {
        StubExchangeFunction exchange = StubExchangeFunction.json(HttpStatus.OK, sportsResponse());
        RakutenSportSource source = new RakutenSportSource(client(exchange), 101070);

        StepVerifier.create(source.fetchSportNames())
                .expectNext("ゴルフ", "テニス", "サッカー・フットサル")
                .verifyComplete();
        assertThat(UriComponentsBuilder.fromUri(exchange.requests().getFirst().url()).build().getQueryParams())
                .containsEntry("genreId", List.of("101070"));
    }
}
