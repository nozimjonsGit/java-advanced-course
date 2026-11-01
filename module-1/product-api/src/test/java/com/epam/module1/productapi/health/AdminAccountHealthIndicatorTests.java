package com.epam.module1.productapi.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.epam.module1.productapi.entity.Role;
import com.epam.module1.productapi.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

class AdminAccountHealthIndicatorTests {

    private final AppUserRepository repository = mock(AppUserRepository.class);
    private final AdminAccountHealthIndicator indicator = new AdminAccountHealthIndicator(repository);

    @Test
    void isUpWhenAnAdminExists() {
        when(repository.existsByRole(Role.ADMIN)).thenReturn(true);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void isDownWhenNobodyCanManageTheCatalog() {
        when(repository.existsByRole(Role.ADMIN)).thenReturn(false);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("reason");
    }
}
