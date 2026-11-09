package com.epam.module2.avroproducer.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.epam.module2.avroproducer.schema.MessageSchemas;
import com.epam.module2.avroproducer.schema.SchemaVersion;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

/**
 * Sends messages to an embedded Kafka broker, with an in-memory Schema Registry, and reads the
 * raw bytes back to check the Confluent wire format: a zero magic byte, then the schema ID in
 * bytes 1 to 4, then the Avro data.
 */
@SpringBootTest(properties = "spring.kafka.producer.properties.schema.registry.url=mock://" + MessageProducerTests.REGISTRY)
@EmbeddedKafka(topics = "messages", partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class MessageProducerTests {

    static final String REGISTRY = "avro-producer-tests";

    private static final String SUBJECT = "messages-value";

    private final SchemaRegistryClient registry = MockSchemaRegistry.getClientForScope(REGISTRY);

    @Autowired
    private MessageProducer producer;

    @Autowired
    private MessageFactory factory;

    @Autowired
    private MessageSchemas schemas;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @AfterAll
    static void dropRegistry() {
        MockSchemaRegistry.dropScope(REGISTRY);
    }

    @Test
    void sendsConfiguredMessageOnStartup() throws Exception {
        byte[] payload = readValueAt(new TopicPartition("messages", 0), 0);

        assertThat(payload[0]).isZero();
        assertThat(schemaId(payload)).isEqualTo(registry.getId(SUBJECT, new AvroSchema(schemas.get(SchemaVersion.V1))));
    }

    @Test
    void writesIdOfEachSchemaVersionInFrontOfThePayload() throws Exception {
        RecordMetadata sentWithV1 = producer.send(factory.create(SchemaVersion.V1, "first", "alice"));
        RecordMetadata sentWithV2 = producer.send(factory.create(SchemaVersion.V2, "second", "alice"));

        int v1Id = registry.getId(SUBJECT, new AvroSchema(schemas.get(SchemaVersion.V1)));
        int v2Id = registry.getId(SUBJECT, new AvroSchema(schemas.get(SchemaVersion.V2)));
        assertThat(v2Id).isNotEqualTo(v1Id);
        assertThat(registry.getAllVersions(SUBJECT)).containsExactly(1, 2);

        byte[] v1Payload = readValueAt(sentWithV1);
        byte[] v2Payload = readValueAt(sentWithV2);
        assertThat(v1Payload[0]).isZero();
        assertThat(schemaId(v1Payload)).isEqualTo(v1Id);
        assertThat(v2Payload[0]).isZero();
        assertThat(schemaId(v2Payload)).isEqualTo(v2Id);
    }

    private static int schemaId(byte[] payload) {
        return ByteBuffer.wrap(payload, 1, 4).getInt();
    }

    private byte[] readValueAt(RecordMetadata metadata) {
        return readValueAt(new TopicPartition(metadata.topic(), metadata.partition()), metadata.offset());
    }

    private byte[] readValueAt(TopicPartition partition, long offset) {
        Map<String, Object> properties = KafkaTestUtils.consumerProps(broker, "message-producer-tests", false);
        var consumerFactory = new DefaultKafkaConsumerFactory<>(properties, new StringDeserializer(), new ByteArrayDeserializer());
        try (Consumer<String, byte[]> consumer = consumerFactory.createConsumer()) {
            consumer.assign(List.of(partition));
            consumer.seek(partition, offset);
            List<ConsumerRecord<String, byte[]>> records = KafkaTestUtils.getRecords(consumer).records(partition);
            return records.getFirst().value();
        }
    }
}
