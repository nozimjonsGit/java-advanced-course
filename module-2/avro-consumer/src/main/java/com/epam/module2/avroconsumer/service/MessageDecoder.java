package com.epam.module2.avroconsumer.service;

import com.epam.module2.avro.Message;
import com.epam.module2.avroconsumer.config.ConsumerProperties;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.GenericContainerWithVersion;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import jakarta.annotation.PreDestroy;
import java.nio.ByteBuffer;
import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.springframework.stereotype.Service;

/**
 * Decodes payloads in the Confluent wire format: a zero magic byte, the writer schema's ID as a
 * 4-byte big-endian integer, then the Avro data.
 *
 * <p>The deserializer fetches the writer schema from Schema Registry by that ID, and Avro
 * resolves the data to the reader schema of the generated {@link Message} class. Fields the
 * writer did not have get their default value, and fields the reader does not have are dropped.
 */
@Service
public class MessageDecoder {

    static final byte MAGIC_BYTE = 0;
    static final int HEADER_LENGTH = 5;

    private final KafkaAvroDeserializer deserializer;

    public MessageDecoder(ConsumerProperties properties) {
        deserializer = new KafkaAvroDeserializer();
        deserializer.configure(Map.of(
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, properties.schemaRegistryUrl(),
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true), false);
    }

    /**
     * @throws SerializationException if the payload is not in the Confluent wire format, its
     *                                schema cannot be fetched, or the schema is not a message schema
     */
    public DecodedMessage decode(String topic, Headers headers, byte[] payload) {
        int schemaId = schemaId(payload);
        GenericContainerWithVersion decoded = deserializer.deserializeWithSchema(topic, headers, payload);
        if (!(decoded.container() instanceof Message message)) {
            throw new SerializationException("Schema %d describes %s, not %s"
                    .formatted(schemaId, decoded.container().getSchema().getFullName(), Message.class.getName()));
        }
        return new DecodedMessage(schemaId, decoded.version(), message);
    }

    @PreDestroy
    void close() {
        deserializer.close();
    }

    private static int schemaId(byte[] payload) {
        if (payload == null || payload.length < HEADER_LENGTH) {
            throw new SerializationException("Payload has %d bytes, fewer than the %d-byte Confluent header"
                    .formatted(payload == null ? 0 : payload.length, HEADER_LENGTH));
        }
        if (payload[0] != MAGIC_BYTE) {
            throw new SerializationException("Payload starts with magic byte %d instead of %d, so it is not in the Confluent wire format"
                    .formatted(payload[0], MAGIC_BYTE));
        }
        return ByteBuffer.wrap(payload, 1, 4).getInt();
    }
}
