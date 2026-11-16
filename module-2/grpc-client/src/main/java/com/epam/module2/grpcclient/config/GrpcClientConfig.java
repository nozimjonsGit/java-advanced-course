package com.epam.module2.grpcclient.config;

import com.epam.module2.grpc.ping.v1.PingServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration(proxyBeanMethods = false)
public class GrpcClientConfig {

    /**
     * The channel's address is set by {@code spring.grpc.client.channel.ping-server.target}.
     */
    @Bean
    PingServiceGrpc.PingServiceBlockingStub pingServiceStub(GrpcChannelFactory channels) {
        return PingServiceGrpc.newBlockingStub(channels.createChannel("ping-server"));
    }
}
