package com.epam.module3.sportapi.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * The parts of a SPARQL JSON result that the sport query uses: one {@code name} per row.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record SparqlResponse(Results results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Results(List<Row> bindings) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Row(Value name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Value(String value) {
    }
}
