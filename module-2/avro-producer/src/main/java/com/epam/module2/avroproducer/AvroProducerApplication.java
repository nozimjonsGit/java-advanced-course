package com.epam.module2.avroproducer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AvroProducerApplication {

    /**
     * Closes the context once the message is sent, which flushes and closes the Kafka producer,
     * and exits with the application's exit code.
     */
    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(AvroProducerApplication.class, args)));
    }

}
