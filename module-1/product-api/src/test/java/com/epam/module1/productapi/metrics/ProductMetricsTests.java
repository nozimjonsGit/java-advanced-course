package com.epam.module1.productapi.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.epam.module1.productapi.repository.ProductRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class ProductMetricsTests {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final ProductRepository repository = mock(ProductRepository.class);
    private final ProductMetrics metrics = new ProductMetrics(registry, repository);

    @Test
    void everyOperationIsReportedFromZeroBeforeItFirstHappens() {
        assertThat(operations("created")).isZero();
        assertThat(operations("updated")).isZero();
        assertThat(operations("deleted")).isZero();
    }

    @Test
    void eachOperationIncreasesOnlyItsOwnCounter() {
        metrics.recordCreated();
        metrics.recordCreated();
        metrics.recordUpdated();
        metrics.recordDeleted();

        assertThat(operations("created")).isEqualTo(2);
        assertThat(operations("updated")).isEqualTo(1);
        assertThat(operations("deleted")).isEqualTo(1);
    }

    @Test
    void gaugeReportsTheCurrentNumberOfProducts() {
        when(repository.count()).thenReturn(3L, 5L);

        assertThat(registry.get("products.count").gauge().value()).isEqualTo(3);
        assertThat(registry.get("products.count").gauge().value()).isEqualTo(5);
    }

    private double operations(String operation) {
        return registry.get("product.operations").tag("operation", operation).counter().count();
    }
}
