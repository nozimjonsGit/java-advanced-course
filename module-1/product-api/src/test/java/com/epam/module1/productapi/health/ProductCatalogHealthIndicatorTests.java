package com.epam.module1.productapi.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.epam.module1.productapi.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

class ProductCatalogHealthIndicatorTests {

    private final ProductRepository repository = mock(ProductRepository.class);
    private final ProductCatalogHealthIndicator indicator = new ProductCatalogHealthIndicator(repository);

    @Test
    void isUpAndReportsTheProductCount() {
        when(repository.count()).thenReturn(12L);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("products", 12L);
    }

    @Test
    void isDownWhenTheCatalogCannotBeRead() {
        when(repository.count()).thenThrow(new IllegalStateException("database unavailable"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}
