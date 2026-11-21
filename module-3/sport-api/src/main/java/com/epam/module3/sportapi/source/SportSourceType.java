package com.epam.module3.sportapi.source;

/**
 * The sources the import can take its sports from, set by {@code app.sport-import.source}.
 */
public enum SportSourceType {

    /** Types of sport on Wikidata. Needs no keys. */
    WIKIDATA,

    /** Child genres of a Rakuten Ichiba genre. Needs Rakuten keys. */
    RAKUTEN
}
