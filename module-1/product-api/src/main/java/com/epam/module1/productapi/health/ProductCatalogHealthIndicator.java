package com.epam.module1.productapi.health;

import com.epam.module1.productapi.repository.ProductRepository;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

/**
 * Reports whether the product catalog can be read. Any failure marks the indicator as down.
 */
@Component
public class ProductCatalogHealthIndicator extends AbstractHealthIndicator {

    private final ProductRepository repository;

    public ProductCatalogHealthIndicator(ProductRepository repository) {
        super("Product catalog health check failed");
        this.repository = repository;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        builder.up().withDetail("products", repository.count());
    }
}
