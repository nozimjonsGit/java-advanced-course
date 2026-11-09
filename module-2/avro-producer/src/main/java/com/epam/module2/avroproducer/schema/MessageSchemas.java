package com.epam.module2.avroproducer.schema;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.Map;
import org.apache.avro.Schema;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Loads every version of the message schema once, so a missing or invalid schema file fails
 * the application at startup rather than on the first send.
 */
@Component
public class MessageSchemas {

    private final Map<SchemaVersion, Schema> schemas = new EnumMap<>(SchemaVersion.class);

    public MessageSchemas() {
        for (SchemaVersion version : SchemaVersion.values()) {
            schemas.put(version, load(version));
        }
    }

    public Schema get(SchemaVersion version) {
        return schemas.get(version);
    }

    private static Schema load(SchemaVersion version) {
        try (InputStream schemaFile = new ClassPathResource(version.location()).getInputStream()) {
            return new Schema.Parser().parse(schemaFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read schema %s from %s".formatted(version, version.location()), e);
        }
    }
}
