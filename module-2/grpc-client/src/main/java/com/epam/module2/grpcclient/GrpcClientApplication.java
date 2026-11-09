package com.epam.module2.grpcclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GrpcClientApplication {

    /**
     * Closes the context once the ping is done, which shuts the gRPC channel down, and exits
     * with the application's exit code.
     */
    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(GrpcClientApplication.class, args)));
    }

}
