package com.epam.module1.productapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Sends requests to a real server: MockMvc has no error dispatch, so it cannot reach the error page.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RejectedRequestTests {

    @LocalServerPort
    private int port;

    @Test
    void malformedUrlIsABadRequestRatherThanARedirectToLogin() throws Exception {
        assertThat(statusOf("/api/v1/products//1")).isEqualTo(400);
    }

    @Test
    void unknownOAuthClientIsABadRequestRatherThanARedirectToLogin() throws Exception {
        assertThat(statusOf("/oauth2/authorize?response_type=code&client_id=unknown")).isEqualTo(400);
    }

    private int statusOf(String path) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build();
            return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        }
    }
}
