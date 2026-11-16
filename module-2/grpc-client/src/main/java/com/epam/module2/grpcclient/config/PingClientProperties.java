package com.epam.module2.grpcclient.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param message  message to send to the server
 * @param client   name the server logs for this client
 * @param deadline how long to wait for the response before failing with {@code DEADLINE_EXCEEDED}
 */
@ConfigurationProperties("app.ping")
public record PingClientProperties(
        @DefaultValue("Ping") String message,
        @DefaultValue("java-client") String client,
        @DefaultValue("5s") Duration deadline) {
}
