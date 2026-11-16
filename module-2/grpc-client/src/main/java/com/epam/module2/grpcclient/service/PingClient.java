package com.epam.module2.grpcclient.service;

import com.epam.module2.grpc.ping.v1.PingRequest;
import com.epam.module2.grpc.ping.v1.PingResponse;
import com.epam.module2.grpc.ping.v1.PingServiceGrpc;
import com.epam.module2.grpcclient.config.PingClientProperties;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

/**
 * Sends the configured message to the ping server.
 */
@Service
public class PingClient {

    private final PingServiceGrpc.PingServiceBlockingStub stub;
    private final PingClientProperties properties;

    public PingClient(PingServiceGrpc.PingServiceBlockingStub stub, PingClientProperties properties) {
        this.stub = stub;
        this.properties = properties;
    }

    /**
     * @throws io.grpc.StatusRuntimeException if the server rejects the request, cannot be reached,
     *                                        or does not answer before the deadline
     */
    public PingResponse ping() {
        PingRequest request = PingRequest.newBuilder()
                .setMessage(properties.message())
                .setClient(properties.client())
                .build();
        return stub
                .withDeadlineAfter(properties.deadline().toMillis(), TimeUnit.MILLISECONDS)
                .ping(request);
    }
}
