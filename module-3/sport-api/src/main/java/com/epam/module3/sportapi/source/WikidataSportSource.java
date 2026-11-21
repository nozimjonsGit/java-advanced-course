package com.epam.module3.sportapi.source;

import com.epam.module3.sportapi.client.WikidataSportClient;
import reactor.core.publisher.Flux;

/**
 * Takes the sports from Wikidata: the best-known types of sport, by their English name.
 */
public class WikidataSportSource implements SportSource {

    private final WikidataSportClient client;

    public WikidataSportSource(WikidataSportClient client) {
        this.client = client;
    }

    @Override
    public Flux<String> fetchSportNames() {
        return client.findSportNames();
    }
}
