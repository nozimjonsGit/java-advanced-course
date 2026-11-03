package com.epam.module1.productapi.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Exercises the whole application, from HTTP down to the database.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ProductApiIntegrationTests {

    private static final String PRODUCTS_URL = "/api/v1/products";
    private static final RequestPostProcessor ADMIN =
            jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));

    @Autowired
    private MockMvc mockMvc;

    @Test
    void productCanBeCreatedReadUpdatedAndDeleted() throws Exception {
        String location = createProduct("Laptop", "999.99");

        mockMvc.perform(get(location).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(999.99));

        mockMvc.perform(put(location).with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Gaming laptop", "1299.50")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gaming laptop"));

        mockMvc.perform(get(location).with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gaming laptop"))
                .andExpect(jsonPath("$.price").value(1299.50));

        mockMvc.perform(delete(location).with(ADMIN)).andExpect(status().isNoContent());

        mockMvc.perform(get(location).with(ADMIN)).andExpect(status().isNotFound());
    }

    @Test
    void listFiltersProductsByName() throws Exception {
        createProduct("Mechanical keyboard", "120.00");
        createProduct("Wireless mouse", "45.00");

        mockMvc.perform(get(PRODUCTS_URL).with(ADMIN).param("name", "KEYBOARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Mechanical keyboard"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void sortingByUnknownFieldIsABadRequest() throws Exception {
        mockMvc.perform(get(PRODUCTS_URL).with(ADMIN).param("sort", "doesNotExist"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Cannot sort by 'doesNotExist'"));
    }

    @Test
    void sortingByNestedFieldIsABadRequest() throws Exception {
        mockMvc.perform(get(PRODUCTS_URL).with(ADMIN).param("sort", "name.value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Cannot sort by 'name.value'"));
    }

    @Test
    void pageBeyondTheDatabaseOffsetLimitIsABadRequest() throws Exception {
        mockMvc.perform(get(PRODUCTS_URL).with(ADMIN).param("page", "100000000").param("size", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Page 100000000 is too large"));
    }

    @Test
    void sortingBySupportedFieldsWorks() throws Exception {
        mockMvc.perform(get(PRODUCTS_URL).with(ADMIN)
                        .param("sort", "price,desc").param("sort", "description").param("sort", "name"))
                .andExpect(status().isOk());
    }

    private String createProduct(String name, String price) throws Exception {
        return mockMvc.perform(post(PRODUCTS_URL).with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(name, price)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn()
                .getResponse()
                .getHeader("Location");
    }

    private static String productJson(String name, String price) {
        return """
                {"name": "%s", "description": "Test product", "price": %s}
                """.formatted(name, price);
    }
}
