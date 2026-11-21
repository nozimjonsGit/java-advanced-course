package com.epam.module3.sportapi.setup;

import com.epam.module3.sportapi.config.SportImportProperties;
import com.epam.module3.sportapi.entity.Sport;
import com.epam.module3.sportapi.service.SportImportService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.SignalType;

/**
 * Imports sports from the configured source once the application has started. The import runs in the
 * background, so the API is available while it runs, and a failed import does not stop the
 * application.
 *
 * <p>The import is consumed in batches: this class requests {@code app.sport-import.batch-size}
 * sports at a time, and logs every request and every sport it receives, so the log shows
 * {@code request(20)}, then 20 {@code onNext(...)}, then {@code request(20)} again.
 *
 * <p>An import that is still running when the application shuts down, for example while it waits
 * for Wikidata or to try a rate-limited request again, is cancelled.
 */
@Component
@ConditionalOnBooleanProperty(name = "app.sport-import.enabled", matchIfMissing = true)
public class SportSetup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SportSetup.class);

    /** Logger the import's signals are logged to. */
    static final String SIGNAL_LOG_CATEGORY = "sport-import";

    private final SportImportService importService;
    private final SportImportProperties properties;

    private volatile Disposable importSubscription = Disposables.disposed();

    public SportSetup(SportImportService importService, SportImportProperties properties) {
        this.importService = importService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String source = importService.sourceName();
        AtomicInteger imported = new AtomicInteger();
        BatchRequestSubscriber<Sport> subscriber = new BatchRequestSubscriber<>(
                properties.batchSize(),
                sport -> imported.incrementAndGet(),
                error -> log.error("Sport import from {} failed after {} new sports", source, imported.get(), error),
                () -> log.info("Imported {} new sports from {}", imported.get(), source));
        importSubscription = subscriber;
        importService.importSports()
                // Errors are not in the list: the subscriber logs them, with the number of sports saved before
                .log(SIGNAL_LOG_CATEGORY, Level.INFO,
                        SignalType.ON_SUBSCRIBE, SignalType.REQUEST, SignalType.ON_NEXT, SignalType.ON_COMPLETE)
                .subscribe(subscriber);
    }

    /**
     * Cancels the import when the application starts to shut down. This runs before Spring stops
     * the HTTP client and the database pool, which would otherwise make a running import fail.
     */
    @EventListener(ContextClosedEvent.class)
    public void cancelImport() {
        if (!importSubscription.isDisposed()) {
            log.info("Cancelling the sport import from {}: the application is shutting down", importService.sourceName());
            importSubscription.dispose();
        }
    }
}
