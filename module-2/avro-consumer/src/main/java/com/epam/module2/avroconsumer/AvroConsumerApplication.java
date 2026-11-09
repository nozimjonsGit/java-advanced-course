package com.epam.module2.avroconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AvroConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AvroConsumerApplication.class, args);
    }

}
