package com.epam.module3.sportapi.service;

import com.epam.module3.sportapi.entity.Sport;
import com.epam.module3.sportapi.exception.InvalidSportNameException;
import com.epam.module3.sportapi.exception.SportAlreadyExistsException;
import com.epam.module3.sportapi.repository.SportRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class SportService {

    private final SportRepository repository;

    public SportService(SportRepository repository) {
        this.repository = repository;
    }

    /**
     * Saves a new sport. The name's leading and trailing whitespace is removed.
     *
     * @return the saved sport, or an {@link InvalidSportNameException} if the name is not valid,
     *         or a {@link SportAlreadyExistsException} if a sport with this name exists
     */
    public Mono<Sport> create(String name) {
        return Mono.fromCallable(() -> Sport.named(name))
                .flatMap(sport -> repository.save(sport)
                        // The unique index on the name rejects the insert, even when two requests race
                        .onErrorMap(DuplicateKeyException.class,
                                exception -> new SportAlreadyExistsException(sport.name(), exception)));
    }

    /**
     * Finds the sports whose name contains the query, ignoring case, sorted by name. A blank
     * query finds all sports.
     */
    public Flux<Sport> search(String query) {
        if (query.isBlank()) {
            return repository.findAllByOrderByNameAsc();
        }
        return repository.findByNameContainingIgnoreCaseOrderByNameAsc(query.strip());
    }
}
