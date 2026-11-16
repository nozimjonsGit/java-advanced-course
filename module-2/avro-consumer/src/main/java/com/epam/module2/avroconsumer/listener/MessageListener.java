package com.epam.module2.avroconsumer.listener;

import com.epam.module2.avroconsumer.service.DecodedMessage;
import com.epam.module2.avroconsumer.service.MessageDecoder;
import java.util.HexFormat;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Prints every message on the topic. Receives the raw bytes, rather than letting Kafka
 * deserialize them, so it can also print the header that carries the writer schema's ID.
 */
@Component
public class MessageListener {

    private static final Logger log = LoggerFactory.getLogger(MessageListener.class);

    private final MessageDecoder decoder;

    public MessageListener(MessageDecoder decoder) {
        this.decoder = decoder;
    }

    @KafkaListener(topics = "${app.consumer.topic}")
    void onMessage(ConsumerRecord<String, byte[]> record) {
        DecodedMessage decoded = decoder.decode(record.topic(), record.headers(), record.value());
        log.info("Received {}-{} at offset {}: header {}, schema id {}, schema version {}, message {}",
                record.topic(), record.partition(), record.offset(),
                HexFormat.of().formatHex(record.value(), 0, 5),
                decoded.schemaId(), decoded.schemaVersion(), decoded.message());
    }
}
