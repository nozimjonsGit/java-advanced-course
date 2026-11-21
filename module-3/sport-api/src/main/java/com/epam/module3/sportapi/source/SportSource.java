package com.epam.module3.sportapi.source;

import reactor.core.publisher.Flux;

/**
 * Where the import gets its sports from.
 */
public interface SportSource {

    /** The source's name, for log messages. */
    String name();

    /**
     * Emits the names of the sports to import. A name may repeat, belong to a sport that is
     * already saved, or not be a valid sport name: the import skips those.
     */
    Flux<String> fetchSportNames();
}
