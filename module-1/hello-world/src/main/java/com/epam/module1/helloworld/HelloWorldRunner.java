package com.epam.module1.helloworld;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Prints a greeting to the console once the Spring context has started.
 */
@Component
public class HelloWorldRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(HelloWorldRunner.class);

    @Override
    public void run(String... args) {
        log.info("hello world");
    }
}
