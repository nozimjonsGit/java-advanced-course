package com.epam.module2.grpcserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.epam.module2.grpc.ping.v1.PingRequest;
import com.epam.module2.grpc.ping.v1.PingResponse;
import com.epam.module2.grpc.ping.v1.PingServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * Calls the service through gRPC, over an in-process transport instead of port 8080.
 */
@SpringBootTest
@AutoConfigureTestGrpcTransport
class PingServiceTests {

    private static final Instant NOW = Instant.parse("2026-11-09T10:15:30.123456789Z");

    @Autowired
    private GrpcChannelFactory channels;

    private PingServiceGrpc.PingServiceBlockingStub stub;

    @BeforeEach
    void createStub() {
        stub = PingServiceGrpc.newBlockingStub(channels.createChannel("ping-server"));
    }

    @Test
    void answersPingWithPong() {
        PingResponse response = stub.ping(PingRequest.newBuilder()
                .setMessage("Ping")
                .setClient("test")
                .build());

        assertThat(response.getMessage()).isEqualTo("Pong");
        assertThat(response.getServerTime().getSeconds()).isEqualTo(NOW.getEpochSecond());
        assertThat(response.getServerTime().getNanos()).isEqualTo(NOW.getNano());
    }

    @Test
    void acceptsRequestWithoutClientName() {
        PingResponse response = stub.ping(PingRequest.newBuilder().setMessage("Ping").build());

        assertThat(response.getMessage()).isEqualTo("Pong");
    }

    @Test
    void rejectsAnyOtherMessage() {
        PingRequest request = PingRequest.newBuilder().setMessage("Hello").build();

        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> stub.ping(request))
                .satisfies(error -> {
                    assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
                    assertThat(error.getStatus().getDescription()).isEqualTo("Expected message 'Ping' but got 'Hello'");
                });
    }

    @Test
    void rejectsEmptyMessage() {
        PingRequest request = PingRequest.getDefaultInstance();

        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> stub.ping(request))
                .extracting(error -> error.getStatus().getCode())
                .isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
