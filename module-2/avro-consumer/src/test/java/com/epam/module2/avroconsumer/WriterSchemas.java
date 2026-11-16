package com.epam.module2.avroconsumer;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.springframework.core.io.ClassPathResource;

/**
 * The producer's two schema versions, copied to the test resources, so the tests can write
 * messages the way the producer does without depending on it.
 */
public final class WriterSchemas {

    public static final Schema V1 = load("avro/message-v1.avsc");
    public static final Schema V2 = load("avro/message-v2.avsc");

    private WriterSchemas() {
    }

    public static GenericRecord version1(String id, String text, Instant sentAt) {
        return new GenericRecordBuilder(V1)
                .set("id", id)
                .set("text", text)
                .set("sentAt", sentAt.toEpochMilli())
                .build();
    }

    public static GenericRecord version2(String id, String text, Instant sentAt, String sender) {
        return new GenericRecordBuilder(V2)
                .set("id", id)
                .set("text", text)
                .set("sentAt", sentAt.toEpochMilli())
                .set("sender", sender)
                .build();
    }

    /**
     * @return a serializer that registers schemas in the given registry, as the producer's does
     */
    public static KafkaAvroSerializer serializer(String schemaRegistryUrl) {
        KafkaAvroSerializer serializer = new KafkaAvroSerializer();
        serializer.configure(Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl), false);
        return serializer;
    }

    private static Schema load(String location) {
        try (InputStream schemaFile = new ClassPathResource(location).getInputStream()) {
            return new Schema.Parser().parse(schemaFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read schema " + location, e);
        }
    }
}
