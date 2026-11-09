package com.epam.module2.avroproducer.config;

import com.epam.module2.avroproducer.schema.SchemaVersion;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param topic         topic to send the message to
 * @param schemaVersion schema to serialize the message with
 * @param text          the message's text
 * @param sender        the message's sender. Only version 2 of the schema has this field
 */
@ConfigurationProperties("app.producer")
public record ProducerProperties(
        @DefaultValue("messages") String topic,
        @DefaultValue("v1") SchemaVersion schemaVersion,
        @DefaultValue("Hello, Avro!") String text,
        @DefaultValue("avro-producer") String sender) {
}
