package com.epam.module3.sportapi.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A genre of products on Rakuten Ichiba.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RakutenGenre(int genreId, String genreName) {
}
