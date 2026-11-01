package com.epam.module1.productapi.health;

import com.epam.module1.productapi.entity.Role;
import com.epam.module1.productapi.repository.AppUserRepository;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

/**
 * Reports a misconfigured deployment: without an admin, nobody can change the catalog.
 */
@Component
public class AdminAccountHealthIndicator extends AbstractHealthIndicator {

    private final AppUserRepository repository;

    public AdminAccountHealthIndicator(AppUserRepository repository) {
        super("Admin account health check failed");
        this.repository = repository;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        if (repository.existsByRole(Role.ADMIN)) {
            builder.up();
        } else {
            builder.down().withDetail("reason", "No user with the ADMIN role exists");
        }
    }
}
