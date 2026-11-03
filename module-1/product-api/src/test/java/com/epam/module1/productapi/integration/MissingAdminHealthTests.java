package com.epam.module1.productapi.integration;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.module1.productapi.entity.Role;
import com.epam.module1.productapi.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A load balancer reads the overall status, so a configuration problem must not remove an
 * instance that can still serve requests.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class MissingAdminHealthTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserRepository repository;

    @BeforeEach
    void removeAllAdmins() {
        when(repository.existsByRole(Role.ADMIN)).thenReturn(false);
    }

    @Test
    void instanceStaysAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void adminSeesTheWarning() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(jsonPath("$.components.adminAccount.status").value("WARNING"));
    }
}
