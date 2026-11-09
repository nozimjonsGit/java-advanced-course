package com.epam.module2.grpcclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.epam.module2.grpc.ping.v1.PingResponse;
import com.google.protobuf.Timestamp;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Starts the whole application against a fake server on a free port. Starts it directly
 * rather than with {@code @SpringBootTest}: a cached test context would not ping again.
 */
@ExtendWith(OutputCaptureExtension.class)
class GrpcClientApplicationTests {

    private Server server;

    @AfterEach
    void stopServer() {
        server.shutdownNow();
    }

    @Test
    void printsResponseFromServer(CapturedOutput output) {
        PingResponse pong = PingResponse.newBuilder()
                .setMessage("Pong")
                .setServerTime(Timestamp.newBuilder().setSeconds(1_700_000_000L))
                .build();
        server = FakePingService.answering(pong).start();

        try (ConfigurableApplicationContext ignored = runApplication()) {
            assertThat(output.getOut()).contains("Received 'Pong' (server time 2023-11-14T22:13:20Z)");
        }
    }

    @Test
    void failsToStartWhenServerRejectsPing() {
        server = FakePingService.failingWith(Status.INVALID_ARGUMENT.withDescription("bad message")).start();

        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(this::runApplication)
                .withMessage("INVALID_ARGUMENT: bad message");
    }

    private ConfigurableApplicationContext runApplication() {
        return SpringApplication.run(GrpcClientApplication.class,
                "--spring.grpc.client.channel.ping-server.target=localhost:" + server.getPort());
    }
}
