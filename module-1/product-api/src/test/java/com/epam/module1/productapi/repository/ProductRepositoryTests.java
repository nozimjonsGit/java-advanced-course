package com.epam.module1.productapi.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.epam.module1.productapi.entity.Product;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class ProductRepositoryTests {

    @Autowired
    private ProductRepository repository;

    @Test
    void searchMatchesPartOfNameIgnoringCase() {
        repository.save(new Product("Mechanical Keyboard", "Tactile", new BigDecimal("120.00")));
        repository.save(new Product("Wireless Mouse", "Silent", new BigDecimal("45.00")));

        Page<Product> result = repository.findByNameContainingIgnoreCase("keyb", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Product::getName)
                .containsExactly("Mechanical Keyboard");
    }

    @Test
    void searchReturnsOnlyTheRequestedPage() {
        repository.save(new Product("Cable A", null, new BigDecimal("1.00")));
        repository.save(new Product("Cable B", null, new BigDecimal("2.00")));
        repository.save(new Product("Cable C", null, new BigDecimal("3.00")));

        Page<Product> result = repository.findByNameContainingIgnoreCase("cable", PageRequest.of(1, 2));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void priceKeepsItsCentsWhenStored() {
        Product saved = repository.saveAndFlush(new Product("Pen", null, new BigDecimal("1999.99")));

        Product found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getPrice()).isEqualByComparingTo("1999.99");
    }

    @Test
    void databaseRejectsProductWithoutName() {
        Product product = new Product(null, "No name", new BigDecimal("1.00"));

        assertThatThrownBy(() -> repository.saveAndFlush(product))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
