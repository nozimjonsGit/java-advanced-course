package com.epam.module3.sportapi.config;

import com.epam.module3.sportapi.client.RakutenItemSearchClient;
import com.epam.module3.sportapi.source.RakutenSportSource;
import com.epam.module3.sportapi.source.SportSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SportSourceConfig {

    @Bean
    SportSource sportSource(RakutenItemSearchClient rakutenClient, RakutenProperties rakutenProperties) {
        return new RakutenSportSource(rakutenClient, rakutenProperties.genreId());
    }
}
