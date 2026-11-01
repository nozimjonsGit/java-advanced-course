package com.epam.module1.productapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.module1.productapi.dto.ProductRequest;
import com.epam.module1.productapi.dto.ProductResponse;
import com.epam.module1.productapi.entity.Product;
import com.epam.module1.productapi.exception.ProductNotFoundException;
import com.epam.module1.productapi.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {

    private static final ProductRequest REQUEST =
            new ProductRequest("Laptop", "14-inch", new BigDecimal("999.99"));

    @Mock
    private ProductRepository repository;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(repository);
    }

    @Test
    void createStoresTheRequestedValues() {
        when(repository.save(any(Product.class))).then(returnsFirstArg());

        ProductResponse response = service.create(REQUEST);

        assertThat(response.name()).isEqualTo("Laptop");
        assertThat(response.description()).isEqualTo("14-inch");
        assertThat(response.price()).isEqualByComparingTo("999.99");
    }

    @Test
    void findByIdFailsWhenProductIsMissing() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(7L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("7");
    }

    @Test
    void updateReplacesAllFieldsOfTheExistingProduct() {
        Product existing = new Product("Old", "Old description", new BigDecimal("1.00"));
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        ProductResponse response = service.update(1L, REQUEST);

        assertThat(response.name()).isEqualTo("Laptop");
        assertThat(existing.getName()).isEqualTo("Laptop");
        assertThat(existing.getDescription()).isEqualTo("14-inch");
        assertThat(existing.getPrice()).isEqualByComparingTo("999.99");
    }

    @Test
    void updateFailsWhenProductIsMissing() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(7L, REQUEST))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void deleteRemovesTheExistingProduct() {
        Product existing = new Product("Laptop", null, new BigDecimal("1.00"));
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        service.delete(1L);

        verify(repository).delete(existing);
    }

    @Test
    void deleteFailsWhenProductIsMissing() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(7L))
                .isInstanceOf(ProductNotFoundException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void findAllSearchesByNameWhenOneIsGiven() {
        Pageable pageable = PageRequest.of(0, 10);
        Product match = new Product("Laptop", null, new BigDecimal("1.00"));
        when(repository.findByNameContainingIgnoreCase("lap", pageable))
                .thenReturn(new PageImpl<>(List.of(match)));

        Page<ProductResponse> result = service.findAll("lap", pageable);

        assertThat(result.getContent()).extracting(ProductResponse::name).containsExactly("Laptop");
    }

    @Test
    void findAllReturnsEverythingWhenNameIsBlank() {
        Pageable pageable = PageRequest.of(0, 10);
        Product product = new Product("Laptop", null, new BigDecimal("1.00"));
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(product)));

        Page<ProductResponse> result = service.findAll("  ", pageable);

        assertThat(result.getContent()).extracting(ProductResponse::name).containsExactly("Laptop");
    }
}
