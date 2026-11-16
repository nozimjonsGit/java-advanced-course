package com.epam.module2.grpcclient;

import com.epam.module2.grpc.ping.v1.PingResponse;
import com.epam.module2.grpcclient.service.PingClient;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Pings the server once the Spring context has started, and prints the response.
 */
@Component
public class PingRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PingRunner.class);

    private final PingClient pingClient;

    public PingRunner(PingClient pingClient) {
        this.pingClient = pingClient;
    }

    @Override
    public void run(String @NonNull ... args) {
        PingResponse response = pingClient.ping();
        log.info("Received '{}' (server time {})", response.getMessage(), toInstant(response.getServerTime()));
    }

    private static Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
