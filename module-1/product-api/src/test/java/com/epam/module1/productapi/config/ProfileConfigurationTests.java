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

    /** Passed as arguments so they take priority over variables set in the developer's shell. */
    private static final String[] DATABASE_SETTINGS = {
        "--DB_HOST=db.internal", "--DB_PORT=5432", "--DB_USERNAME=app", "--DB_PASSWORD=secret"
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

    /** A deployment that forgets its profile must not start with the public local credentials. */
    @Test
    void localIsNeverSelectedByDefault() {
        assertThat(databaseUrl()).isNull();
    }

    private static String databaseUrl(String... profiles) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(NoBeans.class)
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .profiles(profiles);
        try (ConfigurableApplicationContext context = builder.run(DATABASE_SETTINGS)) {
            return context.getEnvironment().getProperty("spring.datasource.url");
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class NoBeans {
    }
}
