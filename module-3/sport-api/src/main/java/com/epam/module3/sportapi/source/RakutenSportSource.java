package com.epam.module3.sportapi.source;

import com.epam.module3.sportapi.client.RakutenGenre;
import com.epam.module3.sportapi.client.RakutenItemSearchClient;
import reactor.core.publisher.Flux;

/**
 * Takes the sports from Rakuten Ichiba: each child genre of a genre, such as Sports &amp; Outdoors,
 * is a sport named after the genre.
 */
public class RakutenSportSource implements SportSource {

    private final RakutenItemSearchClient client;
    private final int genreId;

    public RakutenSportSource(RakutenItemSearchClient client, int genreId) {
        this.client = client;
        this.genreId = genreId;
    }

    @Override
    public String name() {
        return "Rakuten";
    }

    @Override
    public Flux<String> fetchSportNames() {
        return client.findChildGenres(genreId).map(RakutenGenre::genreName);
    }
}
