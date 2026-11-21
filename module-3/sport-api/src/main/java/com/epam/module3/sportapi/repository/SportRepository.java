package com.epam.module3.sportapi.repository;

import com.epam.module3.sportapi.entity.Sport;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface SportRepository extends ReactiveCrudRepository<Sport, Integer> {

    /**
     * Finds the sports whose name contains the given text, ignoring case. {@code %} and {@code _}
     * in the text match themselves, not any characters.
     */
    Flux<Sport> findByNameContainingIgnoreCaseOrderByNameAsc(String text);

    Flux<Sport> findAllByOrderByNameAsc();
}
