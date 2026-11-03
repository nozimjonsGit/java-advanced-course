package com.epam.module1.productapi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureMetrics
class ActuatorEndpointsTests {

    private static final String HEALTH_URL = "/actuator/health";
    private static final String PROMETHEUS_URL = "/actuator/prometheus";
    private static final RequestPostProcessor ADMIN =
            jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    private static final RequestPostProcessor USER =
            jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsPublicButHidesDetails() throws Exception {
        mockMvc.perform(get(HEALTH_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void adminSeesCustomHealthIndicators() throws Exception {
        mockMvc.perform(get(HEALTH_URL).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.productCatalog.status").value("UP"))
                .andExpect(jsonPath("$.components.productCatalog.details.products").isNumber())
                .andExpect(jsonPath("$.components.adminAccount.status").value("UP"));
    }

    @Test
    void userDoesNotSeeHealthDetails() throws Exception {
        mockMvc.perform(get(HEALTH_URL).with(USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void prometheusRequiresToken() throws Exception {
        mockMvc.perform(get(PROMETHEUS_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void prometheusIsNotAvailableToUsers() throws Exception {
        mockMvc.perform(get(PROMETHEUS_URL).with(USER)).andExpect(status().isForbidden());
    }

    @Test
    void prometheusReportsProductMetricsToAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/products").with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Monitor", "description": "27-inch", "price": 300.00}
                                """))
                .andExpect(status().isCreated());

        String metrics = mockMvc.perform(get(PROMETHEUS_URL).with(ADMIN))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(metrics)
                .containsPattern("product_operations_total\\{[^}]*operation=\"created\"[^}]*\\} [1-9]")
                .containsPattern("products_count\\{[^}]*\\} [1-9]")
                .contains("application=\"product-api\"");
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() throws Exception {
        mockMvc.perform(get("/actuator/env").with(ADMIN)).andExpect(status().isNotFound());
    }

    @Test
    void otherActuatorEndpointsRequireToken() throws Exception {
        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }
}
