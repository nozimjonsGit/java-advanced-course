package com.epam.module3.sportapi.service;

import com.epam.module3.sportapi.entity.Sport;
import com.epam.module3.sportapi.exception.InvalidSportNameException;
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
     * Names that are not valid, such as blank ones, are skipped too, so one bad name does not
     * stop the import.
     */
    public Flux<Sport> importSports() {
        return source.fetchSportNames()
                .concatMap(name -> toSport(name).flatMap(this::saveUnlessExists));
    }

    private static Mono<Sport> toSport(String name) {
        return Mono.fromCallable(() -> Sport.named(name))
                .onErrorResume(InvalidSportNameException.class, exception -> {
                    log.warn("Skipped sport \"{}\": {}", name, exception.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Sport> saveUnlessExists(Sport sport) {
        return repository.save(sport)
                .onErrorResume(DuplicateKeyException.class, exception -> {
                    log.debug("Skipped sport {}: it already exists", sport.name());
                    return Mono.empty();
                });
    }
}
