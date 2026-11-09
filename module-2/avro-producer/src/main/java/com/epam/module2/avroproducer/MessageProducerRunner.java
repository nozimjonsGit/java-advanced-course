package com.epam.module2.avroproducer;

import com.epam.module2.avroproducer.config.ProducerProperties;
import com.epam.module2.avroproducer.service.MessageFactory;
import com.epam.module2.avroproducer.service.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Sends one message, built from the {@code app.producer} properties, once the Spring context has
 * started.
 */
@Component
public class MessageProducerRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MessageProducerRunner.class);

    private final MessageFactory messageFactory;
    private final MessageProducer messageProducer;
    private final ProducerProperties properties;

    public MessageProducerRunner(MessageFactory messageFactory, MessageProducer messageProducer,
            ProducerProperties properties) {
        this.messageFactory = messageFactory;
        this.messageProducer = messageProducer;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        log.info("Sending a message with schema {}", properties.schemaVersion());
        messageProducer.send(messageFactory.create(properties.schemaVersion(), properties.text(), properties.sender()));
    }
}
