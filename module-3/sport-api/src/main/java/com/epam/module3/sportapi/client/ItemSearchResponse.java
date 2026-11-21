package com.epam.module3.sportapi.client;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * The parts of an IchibaItem/Search response, in format version 2, that the import uses. The
 * {@code GenreInformation} block is only there when the request sets {@code genreInformationFlag=1}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record ItemSearchResponse(
        // Also accepted as a single object, which is how format version 1 sends it
        @JsonProperty("GenreInformation")
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        List<GenreInformation> genreInformation) {

    /**
     * @param children the searched genre's direct child genres
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenreInformation(List<RakutenGenre> children) {
    }
}
