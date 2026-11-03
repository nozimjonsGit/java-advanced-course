package com.epam.module1.productapi.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Checks which roles may call each endpoint. How a real token gets its roles is covered by
 * {@link AuthorizationCodeFlowTests}.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ProductAccessTests {

    private static final String PRODUCTS_URL = "/api/v1/products";
    private static final String MISSING_PRODUCT_URL = PRODUCTS_URL + "/999999";
    private static final String BODY = """
            {"name": "Desk", "description": "Oak", "price": 150.00}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void readingWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get(PRODUCTS_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void writingWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post(PRODUCTS_URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanRead() throws Exception {
        mockMvc.perform(get(PRODUCTS_URL).with(role("USER"))).andExpect(status().isOk());
    }

    @Test
    void adminCanRead() throws Exception {
        mockMvc.perform(get(PRODUCTS_URL).with(role("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void tokenWithoutKnownRoleCannotRead() throws Exception {
        mockMvc.perform(get(PRODUCTS_URL).with(jwt())).andExpect(status().isForbidden());
    }

    @Test
    void userCannotCreate() throws Exception {
        mockMvc.perform(post(PRODUCTS_URL).with(role("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotUpdate() throws Exception {
        mockMvc.perform(put(MISSING_PRODUCT_URL).with(role("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotDelete() throws Exception {
        mockMvc.perform(delete(MISSING_PRODUCT_URL).with(role("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreate() throws Exception {
        mockMvc.perform(post(PRODUCTS_URL).with(role("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());
    }

    /** Reaches the handler: a missing product is only reported after authorization passes. */
    @Test
    void adminCanAttemptToDelete() throws Exception {
        mockMvc.perform(delete(MISSING_PRODUCT_URL).with(role("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingTokenIsReportedAsAProblemDocument() throws Exception {
        mockMvc.perform(get(PRODUCTS_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void invalidTokenIsReportedAsAProblemDocument() throws Exception {
        mockMvc.perform(get(PRODUCTS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void missingRoleIsReportedAsAProblemDocument() throws Exception {
        mockMvc.perform(post(PRODUCTS_URL).with(role("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void actuatorAccessErrorsAreReportedAsProblemDocuments() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/actuator/prometheus").with(role("USER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private static JwtRequestPostProcessor role(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
