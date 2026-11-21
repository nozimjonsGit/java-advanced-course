package com.epam.module3.sportapi.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Connection to the Rakuten Web Service. Both keys come from the application registered at
 * <a href="https://webservice.rakuten.co.jp/app/list">webservice.rakuten.co.jp</a>.
 *
 * @param baseUrl       host of the Rakuten API
 * @param applicationId the application's ID
 * @param accessKey     the application's access key
 * @param timeout       how long to wait for a response
 */
@ConfigurationProperties("app.rakuten")
public record RakutenProperties(
        @DefaultValue("https://openapi.rakuten.co.jp") URI baseUrl,
        String applicationId,
        String accessKey,
        @DefaultValue("10s") Duration timeout) {

    public boolean hasCredentials() {
        return applicationId != null && !applicationId.isBlank()
                && accessKey != null && !accessKey.isBlank();
    }
}
