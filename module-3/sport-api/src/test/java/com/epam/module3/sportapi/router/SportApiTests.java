package com.epam.module3.sportapi.router;

import com.epam.module3.sportapi.TestcontainersConfiguration;
import com.epam.module3.sportapi.repository.SportRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(properties = "app.sport-import.enabled=false")
@Import(TestcontainersConfiguration.class)
class SportApiTests {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SportRepository repository;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        repository.deleteAll().block();
        client = WebTestClient.bindToApplicationContext(context).build();
    }

    @Test
    void createsSportNamedAfterPath() {
        client.post().uri("/api/v1/sport/{name}", "Golf")
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isNumber()
                .jsonPath("$.name").isEqualTo("Golf");
    }

    @Test
    void decodesNameFromPath() {
        create("Table tennis");
        create("サッカー");

        client.get().uri("/api/v1/sport")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[*].name").isEqualTo(List.of("Table tennis", "サッカー"));
    }

    @Test
    void stripsWhitespaceAroundName() {
        client.post().uri("/api/v1/sport/{name}", "  Golf ")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Golf");
    }

    @Test
    void answersConflictWhenSportExists() {
        create("Golf");

        client.post().uri("/api/v1/sport/{name}", "golf")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.title").isEqualTo("Conflict")
                .jsonPath("$.detail").isEqualTo("Sport \"golf\" already exists")
                .jsonPath("$.instance").isEqualTo("/api/v1/sport/golf");
    }

    @Test
    void answersBadRequestWhenNameIsBlank() {
        client.post().uri("/api/v1/sport/{name}", " ")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Sport name must not be blank");
    }

    @Test
    void answersBadRequestWhenNameIsTooLong() {
        client.post().uri("/api/v1/sport/{name}", "a".repeat(101))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Sport name must be at most 100 characters long, but has 101");
    }

    @Test
    void searchesSportsByNameIgnoringCase() {
        create("Tennis");
        create("Golf");
        create("Table tennis");

        client.get().uri(uri -> uri.path("/api/v1/sport").queryParam("q", "TENN").build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].name").isEqualTo("Table tennis")
                .jsonPath("$[1].name").isEqualTo("Tennis")
                .jsonPath("$[0].id").isNumber();
    }

    @Test
    void answersEmptyListWhenNoSportMatches() {
        create("Golf");

        client.get().uri(uri -> uri.path("/api/v1/sport").queryParam("q", "chess").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("[]");
    }

    @Test
    void listsAllSportsWithoutQuery() {
        create("Tennis");
        create("Golf");

        client.get().uri("/api/v1/sport")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[*].name").isEqualTo(List.of("Golf", "Tennis"));
    }

    private void create(String name) {
        client.post().uri("/api/v1/sport/{name}", name).exchange().expectStatus().isCreated();
    }
}
