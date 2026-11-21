package com.epam.module3.sportapi.config;

import com.epam.module3.sportapi.client.RakutenItemSearchClient;
import com.epam.module3.sportapi.client.WikidataSportClient;
import com.epam.module3.sportapi.source.RakutenSportSource;
import com.epam.module3.sportapi.source.SportSource;
import com.epam.module3.sportapi.source.WikidataSportSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates the {@link SportSource} that {@code app.sport-import.source} names.
 */
@Configuration(proxyBeanMethods = false)
public class SportSourceConfig {

    @Bean
    SportSource sportSource(
            SportImportProperties importProperties,
            WikidataSportClient wikidataClient,
            RakutenItemSearchClient rakutenClient,
            RakutenProperties rakutenProperties) {
        return switch (importProperties.source()) {
            case WIKIDATA -> new WikidataSportSource(wikidataClient);
            case RAKUTEN -> new RakutenSportSource(rakutenClient, rakutenProperties.genreId());
        };
    }
}
