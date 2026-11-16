package com.epam.module2.avroconsumer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.epam.module2.avroconsumer.WriterSchemas;
import com.epam.module2.avroconsumer.config.ConsumerProperties;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.time.Instant;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MessageDecoderTests {

    private static final String REGISTRY = "message-decoder-tests";
    private static final String TOPIC = "messages";
    private static final Instant SENT_AT = Instant.parse("2026-11-09T10:15:30.123Z");

    private final SchemaRegistryClient registry = MockSchemaRegistry.getClientForScope(REGISTRY);
    private final KafkaAvroSerializer serializer = WriterSchemas.serializer("mock://" + REGISTRY);
    private final MessageDecoder decoder = new MessageDecoder(new ConsumerProperties(TOPIC, "mock://" + REGISTRY));

    @AfterEach
    void tearDown() {
        decoder.close();
        serializer.close();
        MockSchemaRegistry.dropScope(REGISTRY);
    }

    @Test
    void fillsInDefaultSenderForMessageWrittenWithVersion1() throws Exception {
        byte[] payload = serialize(WriterSchemas.version1("id-1", "Hello", SENT_AT));

        DecodedMessage decoded = decode(payload);

        assertThat(decoded.schemaId()).isEqualTo(registry.getId("messages-value", new AvroSchema(WriterSchemas.V1)));
        assertThat(decoded.schemaVersion()).isEqualTo(1);
        assertThat(decoded.message().getId()).isEqualTo("id-1");
        assertThat(decoded.message().getText()).isEqualTo("Hello");
        assertThat(decoded.message().getSentAt()).isEqualTo(SENT_AT);
        assertThat(decoded.message().getSender()).isEqualTo("unknown");
    }

    @Test
    void readsSenderOfMessageWrittenWithVersion2() throws Exception {
        DecodedMessage first = decode(serialize(WriterSchemas.version1("id-1", "Hello", SENT_AT)));
        DecodedMessage second = decode(serialize(WriterSchemas.version2("id-2", "Hi", SENT_AT, "alice")));

        assertThat(second.schemaId())
                .isEqualTo(registry.getId("messages-value", new AvroSchema(WriterSchemas.V2)))
                .isNotEqualTo(first.schemaId());
        assertThat(second.schemaVersion()).isEqualTo(2);
        assertThat(second.message().getText()).isEqualTo("Hi");
        assertThat(second.message().getSender()).isEqualTo("alice");
    }

    @Test
    void rejectsPayloadWithoutMagicByte() {
        byte[] payload = "not avro".getBytes();

        assertThatExceptionOfType(SerializationException.class)
                .isThrownBy(() -> decode(payload))
                .withMessage("Payload starts with magic byte 110 instead of 0, so it is not in the Confluent wire format");
    }

    @Test
    void rejectsPayloadShorterThanHeader() {
        assertThatExceptionOfType(SerializationException.class)
                .isThrownBy(() -> decode(new byte[] {0, 0, 1}))
                .withMessage("Payload has 3 bytes, fewer than the 5-byte Confluent header");
    }

    @Test
    void rejectsSchemaIdUnknownToRegistry() {
        byte[] payload = {0, 0, 0, 0, 99, 2, 'x'};

        assertThatExceptionOfType(SerializationException.class)
                .isThrownBy(() -> decode(payload));
    }

    private byte[] serialize(GenericRecord record) {
        return serializer.serialize(TOPIC, record);
    }

    private DecodedMessage decode(byte[] payload) {
        return decoder.decode(TOPIC, new RecordHeaders(), payload);
    }
}
