package com.epam.module2.avroproducer.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.epam.module2.avroproducer.schema.MessageSchemas;
import com.epam.module2.avroproducer.schema.SchemaVersion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

class MessageFactoryTests {

    private static final Instant NOW = Instant.parse("2026-11-09T10:15:30.123Z");

    private final MessageSchemas schemas = new MessageSchemas();
    private final MessageFactory factory = new MessageFactory(schemas, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createsVersion1MessageWithoutSender() {
        GenericRecord message = factory.create(SchemaVersion.V1, "Hello", "alice");

        assertThat(message.getSchema()).isEqualTo(schemas.get(SchemaVersion.V1));
        assertThat(UUID.fromString(message.get("id").toString())).isNotNull();
        assertThat(message.get("text")).isEqualTo("Hello");
        assertThat(message.get("sentAt")).isEqualTo(NOW.toEpochMilli());
        assertThat(message.hasField("sender")).isFalse();
        assertThat(GenericData.get().validate(message.getSchema(), message)).isTrue();
    }

    @Test
    void createsVersion2MessageWithSender() {
        GenericRecord message = factory.create(SchemaVersion.V2, "Hello", "alice");

        assertThat(message.getSchema()).isEqualTo(schemas.get(SchemaVersion.V2));
        assertThat(message.get("text")).isEqualTo("Hello");
        assertThat(message.get("sender")).isEqualTo("alice");
        assertThat(GenericData.get().validate(message.getSchema(), message)).isTrue();
    }

    @Test
    void givesEveryMessageItsOwnId() {
        GenericRecord first = factory.create(SchemaVersion.V1, "Hello", "alice");
        GenericRecord second = factory.create(SchemaVersion.V1, "Hello", "alice");

        assertThat(first.get("id")).isNotEqualTo(second.get("id"));
    }
}
