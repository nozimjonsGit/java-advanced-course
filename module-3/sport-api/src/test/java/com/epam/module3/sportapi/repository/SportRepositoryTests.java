package com.epam.module3.sportapi.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.epam.module3.sportapi.TestcontainersConfiguration;
import com.epam.module3.sportapi.entity.Sport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@DataR2dbcTest
@Import(TestcontainersConfiguration.class)
class SportRepositoryTests {

    @Autowired
    private SportRepository repository;

    @BeforeEach
    void deleteAllSports() {
        repository.deleteAll().block();
    }

    @Test
    void generatesIdOnSave() {
        StepVerifier.create(repository.save(Sport.named("Golf")))
                .assertNext(sport -> {
                    assertThat(sport.id()).isNotNull();
                    assertThat(sport.name()).isEqualTo("Golf");
                })
                .verifyComplete();
    }

    @Test
    void rejectsNameThatDiffersOnlyInCase() {
        StepVerifier.create(repository.save(Sport.named("Golf")).then(repository.save(Sport.named("GOLF"))))
                .verifyError(DuplicateKeyException.class);
    }

    @Test
    void findsSportsWhoseNameContainsTextIgnoringCaseSortedByName() {
        saveAll("Table tennis", "Golf", "Tennis", "Beach volleyball");

        StepVerifier.create(repository.findByNameContainingIgnoreCaseOrderByNameAsc("TENN").map(Sport::name))
                .expectNext("Table tennis", "Tennis")
                .verifyComplete();
    }

    @Test
    void matchesLikeWildcardsInTextLiterally() {
        saveAll("100% cotton", "Golf", "Snow_board");

        StepVerifier.create(repository.findByNameContainingIgnoreCaseOrderByNameAsc("%").map(Sport::name))
                .expectNext("100% cotton")
                .verifyComplete();
        StepVerifier.create(repository.findByNameContainingIgnoreCaseOrderByNameAsc("_").map(Sport::name))
                .expectNext("Snow_board")
                .verifyComplete();
    }

    @Test
    void findsAllSportsSortedByName() {
        saveAll("Tennis", "Golf");

        StepVerifier.create(repository.findAllByOrderByNameAsc().map(Sport::name))
                .expectNext("Golf", "Tennis")
                .verifyComplete();
    }

    private void saveAll(String... names) {
        Flux.fromArray(names).map(Sport::named).concatMap(repository::save).blockLast();
    }
}
