package com.epam.module3.sportapi.source;

/**
 * The sources the import can take its sports from, set by {@code app.sport-import.source}.
 */
public enum SportSourceType {

    /** Types of sport on Wikidata. Needs no keys. */
    WIKIDATA("Wikidata"),

    /** Child genres of a Rakuten Ichiba genre. Needs Rakuten keys. */
    RAKUTEN("Rakuten");

    private final String label;

    SportSourceType(String label) {
        this.label = label;
    }

    /** The source's name, for log messages. */
    public String label() {
        return label;
    }
}
