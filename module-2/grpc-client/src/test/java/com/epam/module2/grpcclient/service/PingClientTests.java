package com.epam.module2.grpcclient.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.epam.module2.grpc.ping.v1.PingRequest;
import com.epam.module2.grpc.ping.v1.PingResponse;
import com.epam.module2.grpc.ping.v1.PingServiceGrpc;
import com.epam.module2.grpcclient.FakePingService;
import com.epam.module2.grpcclient.config.PingClientProperties;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PingClientTests {

    private static final PingClientProperties PROPERTIES =
            new PingClientProperties("Ping", "test-client", Duration.ofSeconds(5));

    private Server server;
    private ManagedChannel channel;

    @AfterEach
    void shutDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void sendsConfiguredMessageAndClientName() {
        FakePingService service = FakePingService.answering(PingResponse.newBuilder().setMessage("Pong").build());

        PingResponse response = clientFor(service, PROPERTIES).ping();

        assertThat(response.getMessage()).isEqualTo("Pong");
        assertThat(service.requests())
                .containsExactly(PingRequest.newBuilder().setMessage("Ping").setClient("test-client").build());
    }

    @Test
    void throwsStatusFromServer() {
        FakePingService service = FakePingService.failingWith(Status.INVALID_ARGUMENT.withDescription("bad message"));
        PingClient client = clientFor(service, PROPERTIES);

        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(client::ping)
                .satisfies(error -> {
                    assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
                    assertThat(error.getStatus().getDescription()).isEqualTo("bad message");
                });
    }

    @Test
    void failsWhenServerDoesNotAnswerBeforeDeadline() {
        PingClientProperties shortDeadline = new PingClientProperties("Ping", "test-client", Duration.ofMillis(100));
        PingClient client = clientFor(FakePingService.silent(), shortDeadline);

        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(client::ping)
                .extracting(error -> error.getStatus().getCode())
                .isEqualTo(Status.Code.DEADLINE_EXCEEDED);
    }

    private PingClient clientFor(FakePingService service, PingClientProperties properties) {
        server = service.start();
        channel = Grpc.newChannelBuilder("localhost:" + server.getPort(), InsecureChannelCredentials.create()).build();
        return new PingClient(PingServiceGrpc.newBlockingStub(channel), properties);
    }
}
