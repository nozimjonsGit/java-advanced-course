package com.epam.module3.sportapi.setup;

import com.epam.module3.sportapi.service.SportImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

/**
 * Imports sports from Rakuten once the application has started. The import runs in the
 * background, so the API is available while it runs, and a failed import does not stop the
 * application.
 */
@Component
@ConditionalOnBooleanProperty(name = "app.sport-import.enabled", matchIfMissing = true)
public class SportSetup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SportSetup.class);

    private final SportImportService importService;

    public SportSetup(SportImportService importService) {
        this.importService = importService;
    }

    @Override
    public void run(ApplicationArguments args) {
        importService.importSports()
                .count()
                .subscribe(
                        count -> log.info("Imported {} new sports from Rakuten", count),
                        error -> log.error("Sport import from Rakuten failed", error));
    }
}
