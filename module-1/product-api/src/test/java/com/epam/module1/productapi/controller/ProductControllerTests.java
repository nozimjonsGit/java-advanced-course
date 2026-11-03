package com.epam.module1.productapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.module1.productapi.dto.ProductRequest;
import com.epam.module1.productapi.dto.ProductResponse;
import com.epam.module1.productapi.exception.ProductNotFoundException;
import com.epam.module1.productapi.service.ProductService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Covers web behavior only. Access rules are tested in ProductAccessTests.
 */
@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTests {

    private static final String PRODUCTS_URL = "/api/v1/products";
    private static final String VALID_BODY = """
            {"name": "Laptop", "description": "14-inch", "price": 999.99}
            """;
    private static final ProductResponse LAPTOP =
            new ProductResponse(5L, "Laptop", "14-inch", new BigDecimal("999.99"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService service;

    @Test
    void createReturnsCreatedWithLocationOfTheNewProduct() throws Exception {
        when(service.create(any(ProductRequest.class))).thenReturn(LAPTOP);

        mockMvc.perform(post(PRODUCTS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/products/5"))
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void createRejectsBlankNameAndReportsTheField() throws Exception {
        String body = """
                {"name": "  ", "price": 10.00}
                """;

        mockMvc.perform(post(PRODUCTS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.name").exists());
        verify(service, never()).create(any());
    }

    @Test
    void createRejectsNegativePriceAndReportsTheField() throws Exception {
        String body = """
                {"name": "Laptop", "price": -0.01}
                """;

        mockMvc.perform(post(PRODUCTS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    void createRejectsMissingPriceAndReportsTheField() throws Exception {
        String body = """
                {"name": "Laptop"}
                """;

        mockMvc.perform(post(PRODUCTS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    void createRejectsMalformedJson() throws Exception {
        mockMvc.perform(post(PRODUCTS_URL).contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReturnsNotFoundProblemWhenProductIsMissing() throws Exception {
        when(service.findById(7L)).thenThrow(new ProductNotFoundException(7L));

        mockMvc.perform(get(PRODUCTS_URL + "/7"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getRejectsNonNumericId() throws Exception {
        mockMvc.perform(get(PRODUCTS_URL + "/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void updateRejectsInvalidBody() throws Exception {
        String body = """
                {"name": "", "price": 10.00}
                """;

        mockMvc.perform(put(PRODUCTS_URL + "/5").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
        verify(service, never()).update(eq(5L), any());
    }

    @Test
    void unexpectedFailureReturnsProblemWithoutInternalDetails() throws Exception {
        when(service.findById(5L)).thenThrow(new IllegalStateException("jdbc:postgresql://db.internal"));

        mockMvc.perform(get(PRODUCTS_URL + "/5"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }

    @Test
    void deleteReturnsNotFoundWhenProductIsMissing() throws Exception {
        doThrow(new ProductNotFoundException(7L)).when(service).delete(7L);

        mockMvc.perform(delete(PRODUCTS_URL + "/7")).andExpect(status().isNotFound());
    }
}
