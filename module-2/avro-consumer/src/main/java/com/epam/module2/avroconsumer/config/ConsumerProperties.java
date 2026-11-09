package com.epam.module2.avroconsumer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param topic             topic to read messages from
 * @param schemaRegistryUrl Schema Registry to fetch writer schemas from, by the ID in each message
 */
@ConfigurationProperties("app.consumer")
public record ConsumerProperties(String topic, String schemaRegistryUrl) {
}
