package com.epam.module3.sportapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SportApiApplication {

    static void main(String[] args) {
        SpringApplication.run(SportApiApplication.class, args);
    }

}
