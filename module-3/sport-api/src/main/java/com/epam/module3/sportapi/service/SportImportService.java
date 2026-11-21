package com.epam.module3.sportapi.service;

import com.epam.module3.sportapi.entity.Sport;
import com.epam.module3.sportapi.repository.SportRepository;
import com.epam.module3.sportapi.source.SportSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Imports the sports of the configured {@link SportSource}.
 */
@Service
public class SportImportService {

    private static final Logger log = LoggerFactory.getLogger(SportImportService.class);

    private final SportSource source;
    private final SportRepository repository;

    public SportImportService(SportSource source, SportRepository repository) {
        this.source = source;
        this.repository = repository;
    }

    /**
     * Fetches the sport names, saves a sport for each one, one at a time, and emits the saved
     * sports. Sports that already exist are skipped, so importing again saves only new sports.
     */
    public Flux<Sport> importSports() {
        return source.fetchSportNames()
                .map(Sport::named)
                .concatMap(this::saveUnlessExists);
    }

    private Mono<Sport> saveUnlessExists(Sport sport) {
        return repository.save(sport)
                .onErrorResume(DuplicateKeyException.class, exception -> {
                    log.debug("Skipped sport {}: it already exists", sport.name());
                    return Mono.empty();
                });
    }
}
