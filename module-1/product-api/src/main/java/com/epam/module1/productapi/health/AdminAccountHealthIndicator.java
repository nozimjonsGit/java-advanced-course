package com.epam.module1.productapi.health;

import com.epam.module1.productapi.entity.Role;
import com.epam.module1.productapi.repository.AppUserRepository;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

/**
 * Warns about a misconfigured deployment: without an admin, nobody can change the catalog. It
 * does not report down, because the instance can still serve requests.
 */
@Component
public class AdminAccountHealthIndicator extends AbstractHealthIndicator {

    private static final Status WARNING = new Status("WARNING");

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
            builder.status(WARNING).withDetail("reason", "No user with the ADMIN role exists");
        }
    }
}
