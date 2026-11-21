package com.epam.module3.sportapi.setup;

import static org.assertj.core.api.Assertions.assertThat;

import com.epam.module3.sportapi.config.SportImportProperties;
import com.epam.module3.sportapi.service.SportImportService;
import com.epam.module3.sportapi.source.SportSourceType;
import com.epam.module3.sportapi.source.StubSportSource;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import reactor.core.publisher.Flux;

class SportSetupShutdownTests {

    private static final SportImportProperties PROPERTIES = new SportImportProperties(SportSourceType.WIKIDATA, 20);

    private final AtomicBoolean cancelled = new AtomicBoolean();

    @Test
    void cancelsRunningImportOnShutdown() {
        SportSetup setup = setup(Flux.<String>never().doOnCancel(() -> cancelled.set(true)));

        setup.run(new DefaultApplicationArguments());
        assertThat(cancelled).isFalse();

        setup.cancelImport();
        assertThat(cancelled).isTrue();
    }

    @Test
    void shutsDownWithoutImportThatHasNotStarted() {
        SportSetup setup = setup(Flux.<String>never().doOnCancel(() -> cancelled.set(true)));

        setup.cancelImport();

        assertThat(cancelled).isFalse();
    }

    @Test
    void doesNotCancelImportThatHasFinished() {
        SportSetup setup = setup(Flux.<String>empty().doOnCancel(() -> cancelled.set(true)));

        setup.run(new DefaultApplicationArguments());
        setup.cancelImport();

        assertThat(cancelled).isFalse();
    }

    private static SportSetup setup(Flux<String> names) {
        // The sources never emit a name, so the import never saves a sport and needs no repository
        SportImportService importService = new SportImportService(new StubSportSource(names), null);
        return new SportSetup(importService, PROPERTIES);
    }
}
