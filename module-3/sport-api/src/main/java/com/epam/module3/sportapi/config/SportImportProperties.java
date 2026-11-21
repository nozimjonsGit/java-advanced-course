package com.epam.module3.sportapi.config;

import com.epam.module3.sportapi.source.SportSourceType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the sport import. {@code app.sport-import.enabled=false} turns the import off.
 *
 * @param source    where to take the sports from
 * @param batchSize how many sports the import requests from upstream at a time
 */
@ConfigurationProperties("app.sport-import")
public record SportImportProperties(
        @DefaultValue("wikidata") SportSourceType source,
        @DefaultValue("20") int batchSize) {

    public SportImportProperties {
        if (batchSize < 1) {
            throw new IllegalArgumentException("app.sport-import.batch-size must be at least 1, but is " + batchSize);
        }
    }
}
