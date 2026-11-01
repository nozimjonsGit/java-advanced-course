package com.epam.module1.helloworld;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

@ExtendWith(OutputCaptureExtension.class)
class HelloWorldRunnerTests {

    /**
     * Starts the application directly rather than with {@code @SpringBootTest}: a cached
     * test context would not run the startup code again, so nothing would be captured.
     */
    @Test
    void printsHelloWorldToConsoleWhenApplicationStarts(CapturedOutput output) {
        try (ConfigurableApplicationContext ignored = SpringApplication.run(HelloWorldApplication.class)) {
            assertThat(output.getOut()).contains("hello world");
        }
    }
}
