package com.epam.module3.sportapi.setup;

import static org.assertj.core.api.Assertions.assertThat;

import com.epam.module3.sportapi.TestcontainersConfiguration;
import com.epam.module3.sportapi.client.ExternalApiException;
import com.epam.module3.sportapi.source.SportSource;
import com.epam.module3.sportapi.source.StubSportSource;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

/**
 * Starts the application with a source that fails, and checks that the failure is logged and that
 * the API keeps working.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class SportSetupFailureTests {

    @TestBean
    private SportSource sportSource;

    @Autowired
    private ApplicationContext context;

    static SportSource sportSource() {
        return new StubSportSource(Flux.error(new ExternalApiException("Source is down")));
    }

    @Test
    void logsFailedImportAndKeepsServingApi(CapturedOutput output) {
        Flux.interval(Duration.ofMillis(50))
                .filter(tick -> output.getOut().contains("Sport import from Stub failed after 0 new sports"))
                .blockFirst(Duration.ofSeconds(20));

        WebTestClient.bindToApplicationContext(context).build()
                .get().uri("/api/v1/sport")
                .exchange()
                .expectStatus().isOk();
        assertThat(output.getOut())
                .contains("ExternalApiException: Source is down");
    }
}
