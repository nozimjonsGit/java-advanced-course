package com.epam.module2.avroconsumer.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.epam.module2.avroconsumer.WriterSchemas;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.time.Duration;
import java.time.Instant;
import org.apache.avro.Schema;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

/**
 * Runs the consumer against an embedded Kafka broker and an in-memory Schema Registry, and
 * checks what it prints for each message.
 */
@SpringBootTest(properties = "app.consumer.schema-registry-url=mock://" + MessageListenerTests.REGISTRY)
@EmbeddedKafka(topics = "messages", partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@ExtendWith(OutputCaptureExtension.class)
class MessageListenerTests {

    static final String REGISTRY = "message-listener-tests";

    private static final String TOPIC = "messages";
    private static final Instant SENT_AT = Instant.parse("2026-11-09T10:15:30.123Z");
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final SchemaRegistryClient registry = MockSchemaRegistry.getClientForScope(REGISTRY);
    private final KafkaAvroSerializer serializer = WriterSchemas.serializer("mock://" + REGISTRY);

    @Autowired
    private EmbeddedKafkaBroker broker;

    @AfterEach
    void closeSerializer() {
        serializer.close();
    }

    @AfterAll
    static void dropRegistry() {
        MockSchemaRegistry.dropScope(REGISTRY);
    }

    @Test
    void printsMessagesWrittenWithEitherSchemaVersion(CapturedOutput output) {
        send(serializer.serialize(TOPIC, WriterSchemas.version1("id-1", "Hello", SENT_AT)));
        send(serializer.serialize(TOPIC, WriterSchemas.version2("id-2", "Hi", SENT_AT, "alice")));

        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(output.getOut())
                .contains(header(WriterSchemas.V1) + ", schema version 1, message "
                        + "{\"id\": \"id-1\", \"text\": \"Hello\", \"sentAt\": \"2026-11-09T10:15:30.123Z\", \"sender\": \"unknown\"}")
                .contains(header(WriterSchemas.V2) + ", schema version 2, message "
                        + "{\"id\": \"id-2\", \"text\": \"Hi\", \"sentAt\": \"2026-11-09T10:15:30.123Z\", \"sender\": \"alice\"}"));
    }

    @Test
    void skipsPayloadItCannotDecodeAndCarriesOn(CapturedOutput output) {
        send("not avro".getBytes());
        send(serializer.serialize(TOPIC, WriterSchemas.version2("id-3", "Still here", SENT_AT, "alice")));

        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(output.getOut())
                .contains("magic byte 110 instead of 0")
                .contains("\"id\": \"id-3\""));
    }

    /**
     * @return the start of the line the listener prints for a message written with the schema
     */
    private String header(Schema writerSchema) throws Exception {
        int schemaId = registry.getId("messages-value", new AvroSchema(writerSchema));
        return "header 00%08x, schema id %d".formatted(schemaId, schemaId);
    }

    private void send(byte[] payload) {
        var producerFactory = new DefaultKafkaProducerFactory<>(
                KafkaTestUtils.producerProps(broker), new StringSerializer(), new ByteArraySerializer());
        try {
            new KafkaTemplate<>(producerFactory).send(TOPIC, payload).join();
        } finally {
            producerFactory.destroy();
        }
    }
}
