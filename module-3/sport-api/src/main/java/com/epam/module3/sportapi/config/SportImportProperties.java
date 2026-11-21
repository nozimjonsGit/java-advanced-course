package com.epam.module3.sportapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the sport import. {@code app.sport-import.enabled=false} turns the import off.
 *
 * @param genreId   Rakuten genre whose child genres become sports. 101070 is Sports &amp; Outdoors
 * @param batchSize how many sports the import requests from upstream at a time
 */
@ConfigurationProperties("app.sport-import")
public record SportImportProperties(
        @DefaultValue("101070") int genreId,
        @DefaultValue("20") int batchSize) {

    public SportImportProperties {
        if (batchSize < 1) {
            throw new IllegalArgumentException("app.sport-import.batch-size must be at least 1, but is " + batchSize);
        }
    }
}
