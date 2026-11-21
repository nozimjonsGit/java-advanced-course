package com.epam.module3.sportapi.service;

import static com.epam.module3.sportapi.client.RakutenFixtures.client;
import static com.epam.module3.sportapi.client.RakutenFixtures.responseWithChildGenres;
import static com.epam.module3.sportapi.client.RakutenFixtures.sportsResponse;

import com.epam.module3.sportapi.TestcontainersConfiguration;
import com.epam.module3.sportapi.client.RakutenApiException;
import com.epam.module3.sportapi.client.StubExchangeFunction;
import com.epam.module3.sportapi.config.SportImportProperties;
import com.epam.module3.sportapi.entity.Sport;
import com.epam.module3.sportapi.repository.SportRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import reactor.test.StepVerifier;

@DataR2dbcTest
@Import(TestcontainersConfiguration.class)
class SportImportServiceTests {

    private static final SportImportProperties PROPERTIES = new SportImportProperties(101070, 20);

    @Autowired
    private SportRepository repository;

    @BeforeEach
    void deleteAllSports() {
        repository.deleteAll().block();
    }

    @Test
    void savesSportForEachChildGenre() {
        StepVerifier.create(importService(sportsResponse()).importSports().map(Sport::name))
                .expectNext("ゴルフ", "テニス", "サッカー・フットサル")
                .verifyComplete();

        StepVerifier.create(repository.findAllByOrderByNameAsc().map(Sport::name))
                .expectNext("ゴルフ", "サッカー・フットサル", "テニス")
                .verifyComplete();
    }

    @Test
    void skipsSportsThatAlreadyExist() {
        repository.save(Sport.named("ゴルフ")).block();

        StepVerifier.create(importService(sportsResponse()).importSports().map(Sport::name))
                .expectNext("テニス", "サッカー・フットサル")
                .verifyComplete();
    }

    @Test
    void savesGenresThatDifferOnlyInCaseOnce() {
        String response = responseWithChildGenres(List.of("Golf", "GOLF", "Tennis"));

        StepVerifier.create(importService(response).importSports().map(Sport::name))
                .expectNext("Golf", "Tennis")
                .verifyComplete();
    }

    @Test
    void failsWhenRakutenFails() {
        StubExchangeFunction exchange = StubExchangeFunction.json(HttpStatus.TOO_MANY_REQUESTS, "{}");
        SportImportService service = new SportImportService(client(exchange), repository, PROPERTIES);

        StepVerifier.create(service.importSports()).verifyError(RakutenApiException.class);
    }

    private SportImportService importService(String response) {
        StubExchangeFunction exchange = StubExchangeFunction.json(HttpStatus.OK, response);
        return new SportImportService(client(exchange), repository, PROPERTIES);
    }
}
