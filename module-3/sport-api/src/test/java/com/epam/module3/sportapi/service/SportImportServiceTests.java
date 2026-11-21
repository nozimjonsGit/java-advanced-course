package com.epam.module3.sportapi.service;

import com.epam.module3.sportapi.TestcontainersConfiguration;
import com.epam.module3.sportapi.client.ExternalApiException;
import com.epam.module3.sportapi.entity.Sport;
import com.epam.module3.sportapi.repository.SportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@DataR2dbcTest
@Import(TestcontainersConfiguration.class)
class SportImportServiceTests {

    @Autowired
    private SportRepository repository;

    @BeforeEach
    void deleteAllSports() {
        repository.deleteAll().block();
    }

    @Test
    void savesSportForEachName() {
        SportImportService service = new SportImportService(() -> Flux.just("Golf", "Tennis", "Judo"), repository);

        StepVerifier.create(service.importSports().map(Sport::name))
                .expectNext("Golf", "Tennis", "Judo")
                .verifyComplete();

        StepVerifier.create(repository.findAllByOrderByNameAsc().map(Sport::name))
                .expectNext("Golf", "Judo", "Tennis")
                .verifyComplete();
    }

    @Test
    void skipsSportsThatAlreadyExist() {
        repository.save(Sport.named("Golf")).block();
        SportImportService service = new SportImportService(() -> Flux.just("Golf", "Tennis"), repository);

        StepVerifier.create(service.importSports().map(Sport::name))
                .expectNext("Tennis")
                .verifyComplete();
    }

    @Test
    void savesNamesThatDifferOnlyInCaseOnce() {
        SportImportService service = new SportImportService(() -> Flux.just("Golf", "GOLF", "Tennis"), repository);

        StepVerifier.create(service.importSports().map(Sport::name))
                .expectNext("Golf", "Tennis")
                .verifyComplete();
    }

    @Test
    void skipsNamesThatAreNotValid() {
        Flux<String> names = Flux.just("Golf", " ", "a".repeat(Sport.MAX_NAME_LENGTH + 1), "Tennis");
        SportImportService service = new SportImportService(() -> names, repository);

        StepVerifier.create(service.importSports().map(Sport::name))
                .expectNext("Golf", "Tennis")
                .verifyComplete();
    }

    @Test
    void failsWhenSourceFails() {
        ExternalApiException failure = new ExternalApiException("Source is down");
        SportImportService service = new SportImportService(() -> Flux.error(failure), repository);

        StepVerifier.create(service.importSports()).verifyErrorMatches(error -> error == failure);
    }
}
