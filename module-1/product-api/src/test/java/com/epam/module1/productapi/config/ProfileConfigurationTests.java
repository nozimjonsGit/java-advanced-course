package com.epam.module1.productapi.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * Loads the real configuration files through Spring Boot, without starting the application.
 */
class ProfileConfigurationTests {

    private static final String[] DATABASE_ENVIRONMENT = {
        "DB_HOST=db.internal", "DB_USERNAME=app", "DB_PASSWORD=secret"
    };

    @ParameterizedTest
    @CsvSource({
        "local, jdbc:h2:mem:module1_local;DB_CLOSE_DELAY=-1",
        "dev,   jdbc:postgresql://db.internal:5432/module1_dev",
        "stg,   jdbc:postgresql://db.internal:5432/module1_stg",
        "prod,  jdbc:postgresql://db.internal:5432/module1_prod"
    })
    void eachProfileUsesItsOwnDatabase(String profile, String expectedUrl) {
        assertThat(databaseUrl(profile)).isEqualTo(expectedUrl);
    }

    @Test
    void localIsTheDefaultProfile() {
        assertThat(databaseUrl()).isEqualTo("jdbc:h2:mem:module1_local;DB_CLOSE_DELAY=-1");
    }

    private static String databaseUrl(String... profiles) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(NoBeans.class)
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .profiles(profiles)
                .properties(DATABASE_ENVIRONMENT);
        try (ConfigurableApplicationContext context = builder.run()) {
            return context.getEnvironment().getProperty("spring.datasource.url");
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class NoBeans {
    }
}
