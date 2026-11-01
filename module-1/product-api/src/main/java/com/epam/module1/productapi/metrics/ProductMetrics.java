package com.epam.module1.productapi.metrics;

import com.epam.module1.productapi.repository.ProductRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Business metrics for the product catalog, exported to Prometheus as
 * {@code product_operations_total} and {@code products_count}.
 */
@Component
public class ProductMetrics {

    private final Counter created;
    private final Counter updated;
    private final Counter deleted;

    public ProductMetrics(MeterRegistry registry, ProductRepository repository) {
        this.created = operationCounter(registry, "created");
        this.updated = operationCounter(registry, "updated");
        this.deleted = operationCounter(registry, "deleted");
        Gauge.builder("products.count", repository, ProductRepository::count)
                .description("Number of products in the catalog")
                .register(registry);
    }

    public void recordCreated() {
        created.increment();
    }

    public void recordUpdated() {
        updated.increment();
    }

    public void recordDeleted() {
        deleted.increment();
    }

    private static Counter operationCounter(MeterRegistry registry, String operation) {
        return Counter.builder("product.operations")
                .description("Successful changes to the product catalog")
                .tag("operation", operation)
                .register(registry);
    }
}
