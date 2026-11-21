package com.epam.module3.sportapi.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.epam.module3.sportapi.client.RakutenItemSearchClient;
import com.epam.module3.sportapi.client.WikidataSportClient;
import com.epam.module3.sportapi.source.RakutenSportSource;
import com.epam.module3.sportapi.source.SportSource;
import com.epam.module3.sportapi.source.WikidataSportSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

class SportSourceConfigTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(SourceConfiguration.class);

    @Test
    void takesSportsFromWikidataByDefault() {
        runner.run(context -> assertThat(context).getBean(SportSource.class).isInstanceOf(WikidataSportSource.class));
    }

    @Test
    void takesSportsFromRakutenWhenConfigured() {
        runner.withPropertyValues("app.sport-import.source=rakuten")
                .run(context -> assertThat(context).getBean(SportSource.class).isInstanceOf(RakutenSportSource.class));
    }

    @Test
    void failsToStartWithUnknownSource() {
        runner.withPropertyValues("app.sport-import.source=decathlon")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("decathlon"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({SportImportProperties.class, RakutenProperties.class, WikidataProperties.class})
    @Import({SportSourceConfig.class, RakutenItemSearchClient.class, WikidataSportClient.class})
    static class SourceConfiguration {

        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }
}
