package com.epam.module3.sportapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the sport import. {@code app.sport-import.enabled=false} turns the import off.
 *
 * @param genreId Rakuten genre whose child genres become sports. 101070 is Sports &amp; Outdoors
 */
@ConfigurationProperties("app.sport-import")
public record SportImportProperties(@DefaultValue("101070") int genreId) {
}
