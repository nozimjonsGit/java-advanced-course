package com.epam.module3.sportapi.setup;

import static org.assertj.core.api.Assertions.assertThat;

import com.epam.module3.sportapi.SportApiApplication;
import com.epam.module3.sportapi.TestcontainersConfiguration;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import reactor.core.publisher.Flux;

/**
 * Starts the application against a Wikidata stand-in that accepts the connection but never
 * answers, closes the application while the import waits, and checks that the import is cancelled
 * rather than failing because the HTTP client was stopped under it.
 *
 * <p>The test starts the application itself, rather than through {@code @SpringBootTest}, because
 * it closes it.
 */
@ExtendWith(OutputCaptureExtension.class)
class SportSetupContextCloseTests {

    @Test
    void cancelsImportThatIsWaitingWhenApplicationCloses(CapturedOutput output) throws IOException {
        // Accepts connections in its backlog, and never reads or answers
        try (ServerSocket silentServer = new ServerSocket(0)) {
            ConfigurableApplicationContext context = new SpringApplicationBuilder(
                    SportApiApplication.class, TestcontainersConfiguration.class)
                    .properties(
                            "server.port=0",
                            "app.wikidata.base-url=http://localhost:" + silentServer.getLocalPort(),
                            "app.wikidata.timeout=1m")
                    .run();
            awaitLog(output, SportSetup.SIGNAL_LOG_CATEGORY + " ");
            // Gives the client time to connect and send its request
            Flux.interval(Duration.ofMillis(500)).blockFirst();

            context.close();
        }

        assertThat(output.getOut())
                .contains("Cancelling the sport import from Wikidata: the application is shutting down")
                .doesNotContain("Sport import from Wikidata failed");
    }

    private static void awaitLog(CapturedOutput output, String text) {
        Flux.interval(Duration.ofMillis(50))
                .filter(tick -> output.getOut().contains(text))
                .blockFirst(Duration.ofSeconds(20));
    }
}
