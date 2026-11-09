package com.epam.module2.grpcserver.service;

import com.epam.module2.grpc.ping.v1.PingRequest;
import com.epam.module2.grpc.ping.v1.PingResponse;
import com.epam.module2.grpc.ping.v1.PingServiceGrpc;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

/**
 * Answers {@code Ping} with {@code Pong}. Any other message is rejected with
 * {@code INVALID_ARGUMENT}, so a client sees at once that it sent the wrong thing.
 */
@GrpcService
public class PingService extends PingServiceGrpc.PingServiceImplBase {

    static final String PING = "Ping";
    static final String PONG = "Pong";

    private static final Logger log = LoggerFactory.getLogger(PingService.class);

    private final Clock clock;

    public PingService(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
        log.info("Received '{}' from '{}'", request.getMessage(), request.getClient());

        if (!PING.equals(request.getMessage())) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Expected message '%s' but got '%s'".formatted(PING, request.getMessage()))
                    .asRuntimeException());
            return;
        }

        responseObserver.onNext(PingResponse.newBuilder()
                .setMessage(PONG)
                .setServerTime(toTimestamp(clock.instant()))
                .build());
        responseObserver.onCompleted();
    }

    private static Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
