package com.epam.module2.avroconsumer.config;

import org.apache.kafka.common.errors.SerializationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
public class KafkaConsumerConfig {

    /**
     * Retries a failed message twice, one second apart, then logs it and moves on. A message
     * that cannot be deserialized is skipped at once, because retrying cannot fix it.
     */
    @Bean
    DefaultErrorHandler errorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(1000L, 2));
        errorHandler.addNotRetryableExceptions(SerializationException.class);
        return errorHandler;
    }
}
