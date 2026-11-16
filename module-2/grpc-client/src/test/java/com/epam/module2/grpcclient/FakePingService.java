package com.epam.module2.grpcclient;

import com.epam.module2.grpc.ping.v1.PingRequest;
import com.epam.module2.grpc.ping.v1.PingResponse;
import com.epam.module2.grpc.ping.v1.PingServiceGrpc;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stands in for the ping server, so the client can be tested without starting it.
 * Answers with the given response, fails with the given status, or never answers.
 */
public class FakePingService extends PingServiceGrpc.PingServiceImplBase {

    // Written by the server's threads, read by the test's thread
    private final List<PingRequest> requests = new CopyOnWriteArrayList<>();
    private final PingResponse response;
    private final Status error;

    private FakePingService(PingResponse response, Status error) {
        this.response = response;
        this.error = error;
    }

    public static FakePingService answering(PingResponse response) {
        return new FakePingService(response, null);
    }

    public static FakePingService failingWith(Status error) {
        return new FakePingService(null, error);
    }

    public static FakePingService silent() {
        return new FakePingService(null, null);
    }

    /**
     * Starts a plaintext server for this service on a free port. Clients reach it at
     * {@code localhost:<port>}, where the port is {@link Server#getPort()}.
     */
    public Server start() {
        try {
            return Grpc.newServerBuilderForPort(0, InsecureServerCredentials.create())
                    .addService(this)
                    .build()
                    .start();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not start the fake ping server", e);
        }
    }

    public List<PingRequest> requests() {
        return requests;
    }

    @Override
    public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
        requests.add(request);
        if (error != null) {
            responseObserver.onError(error.asRuntimeException());
        } else if (response != null) {
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
