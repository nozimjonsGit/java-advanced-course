package com.epam.module3.sportapi.service;

import com.epam.module3.sportapi.client.RakutenItemSearchClient;
import com.epam.module3.sportapi.config.SportImportProperties;
import com.epam.module3.sportapi.entity.Sport;
import com.epam.module3.sportapi.repository.SportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Imports sports from Rakuten: the child genres of the configured genre become sports.
 */
@Service
public class SportImportService {

    private static final Logger log = LoggerFactory.getLogger(SportImportService.class);

    private final RakutenItemSearchClient client;
    private final SportRepository repository;
    private final SportImportProperties properties;

    public SportImportService(
            RakutenItemSearchClient client, SportRepository repository, SportImportProperties properties) {
        this.client = client;
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * Fetches the genres, saves a sport for each one, one at a time, and emits the saved sports.
     * Sports that already exist are skipped, so importing again saves only new genres.
     */
    public Flux<Sport> importSports() {
        return client.findChildGenres(properties.genreId())
                .map(genre -> Sport.named(genre.genreName()))
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
