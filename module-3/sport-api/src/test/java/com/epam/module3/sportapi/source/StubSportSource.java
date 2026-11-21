package com.epam.module3.sportapi.source;

import reactor.core.publisher.Flux;

/**
 * A source that emits the given names, for tests.
 */
public record StubSportSource(String name, Flux<String> names) implements SportSource {

    /** A source named "Stub". */
    public StubSportSource(Flux<String> names) {
        this("Stub", names);
    }

    @Override
    public Flux<String> fetchSportNames() {
        return names;
    }
}
