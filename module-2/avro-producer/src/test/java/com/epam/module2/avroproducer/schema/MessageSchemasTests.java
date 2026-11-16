package com.epam.module2.avroproducer.schema;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.avro.Schema;
import org.apache.avro.SchemaCompatibility;
import org.apache.avro.SchemaCompatibility.SchemaCompatibilityType;
import org.junit.jupiter.api.Test;

class MessageSchemasTests {

    private final MessageSchemas schemas = new MessageSchemas();

    @Test
    void loadsBothVersionsUnderTheSameName() {
        Schema v1 = schemas.get(SchemaVersion.V1);
        Schema v2 = schemas.get(SchemaVersion.V2);

        assertThat(v1.getFullName()).isEqualTo("com.epam.module2.avro.Message");
        assertThat(v2.getFullName()).isEqualTo(v1.getFullName());
        assertThat(v1.getField("sender")).isNull();
        assertThat(v2.getField("sender").defaultVal()).isEqualTo("unknown");
    }

    /**
     * Schema Registry's default compatibility mode is BACKWARD: it accepts version 2 only if
     * a reader with version 2 can read data written with version 1.
     */
    @Test
    void version2CanReadDataWrittenWithVersion1() {
        SchemaCompatibilityType compatibility = SchemaCompatibility
                .checkReaderWriterCompatibility(schemas.get(SchemaVersion.V2), schemas.get(SchemaVersion.V1))
                .getType();

        assertThat(compatibility).isEqualTo(SchemaCompatibilityType.COMPATIBLE);
    }
}
