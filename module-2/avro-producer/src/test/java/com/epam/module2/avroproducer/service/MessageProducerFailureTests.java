package com.epam.module2.avroproducer.service;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.epam.module2.avroproducer.config.ProducerProperties;
import com.epam.module2.avroproducer.schema.MessageSchemas;
import com.epam.module2.avroproducer.schema.SchemaVersion;
import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;

class MessageProducerFailureTests {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, GenericRecord> kafkaTemplate = mock(KafkaTemplate.class);
    private final MessageProducer producer = new MessageProducer(kafkaTemplate,
            new ProducerProperties("messages", SchemaVersion.V1, "Hello", "alice"));
    private final GenericRecord message =
            new MessageFactory(new MessageSchemas(), Clock.systemUTC()).create(SchemaVersion.V1, "Hello", "alice");

    @Test
    void namesMessageAndTopicWhenSendFails() {
        KafkaException failure = new KafkaException("Send failed", new TimeoutException("Topic messages not present in metadata"));
        given(kafkaTemplate.send(eq("messages"), anyString(), any())).willThrow(failure);

        assertThatIllegalStateException()
                .isThrownBy(() -> producer.send(message))
                .withMessage("Could not send message %s to topic messages", message.get("id"))
                .withCause(failure);
    }

    @Test
    void namesMessageAndTopicWhenKafkaDoesNotAcknowledge() {
        TimeoutException failure = new TimeoutException("Expiring 1 record(s)");
        given(kafkaTemplate.send(eq("messages"), anyString(), any())).willReturn(CompletableFuture.failedFuture(failure));

        assertThatIllegalStateException()
                .isThrownBy(() -> producer.send(message))
                .withMessage("Could not send message %s to topic messages", message.get("id"))
                .withCause(failure);
    }
}
