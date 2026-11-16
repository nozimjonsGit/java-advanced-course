package com.epam.module2.avroproducer.service;

import com.epam.module2.avroproducer.config.ProducerProperties;
import java.util.concurrent.CompletionException;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Sends messages to the configured topic. The Avro serializer registers the message's schema in
 * Schema Registry, if it is not there yet, and writes the schema's ID in front of the payload.
 */
@Service
public class MessageProducer {

    private static final Logger log = LoggerFactory.getLogger(MessageProducer.class);

    private final KafkaTemplate<String, GenericRecord> kafkaTemplate;
    private final ProducerProperties properties;

    public MessageProducer(KafkaTemplate<String, GenericRecord> kafkaTemplate, ProducerProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    /**
     * Sends the message with its ID as the key, and waits until Kafka acknowledges it.
     *
     * @throws IllegalStateException if the message cannot be serialized or sent
     */
    public RecordMetadata send(GenericRecord message) {
        String key = message.get("id").toString();
        RecordMetadata metadata;
        try {
            metadata = kafkaTemplate.send(properties.topic(), key, message).join().getRecordMetadata();
        } catch (KafkaException e) {
            // Thrown by send() itself, when serialization fails or the topic's metadata times out
            throw notSent(key, e);
        } catch (CompletionException e) {
            // Kafka did not acknowledge the record
            throw notSent(key, e.getCause());
        }
        log.info("Sent {} to {}-{} at offset {}",
                message, metadata.topic(), metadata.partition(), metadata.offset());
        return metadata;
    }

    private IllegalStateException notSent(String key, Throwable cause) {
        return new IllegalStateException(
                "Could not send message %s to topic %s".formatted(key, properties.topic()), cause);
    }
}
