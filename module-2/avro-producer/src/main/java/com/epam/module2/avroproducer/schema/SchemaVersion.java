package com.epam.module2.avroproducer.schema;

/**
 * The versions of the message schema this producer can write.
 */
public enum SchemaVersion {

    /** {@code id}, {@code text} and {@code sentAt}. */
    V1("avro/message-v1.avsc"),

    /** Version 1 plus {@code sender}, which has a default so that version 2 stays backward compatible. */
    V2("avro/message-v2.avsc");

    private final String location;

    SchemaVersion(String location) {
        this.location = location;
    }

    /**
     * @return the schema file's location on the classpath
     */
    public String location() {
        return location;
    }
}
