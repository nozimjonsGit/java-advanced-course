package com.epam.module1.productapi.service;

import com.epam.module1.productapi.dto.ProductRequest;
import com.epam.module1.productapi.dto.ProductResponse;
import com.epam.module1.productapi.entity.Product;
import com.epam.module1.productapi.exception.ProductNotFoundException;
import com.epam.module1.productapi.metrics.ProductMetrics;
import com.epam.module1.productapi.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;
    private final ProductMetrics metrics;

    public ProductService(ProductRepository repository, ProductMetrics metrics) {
        this.repository = repository;
        this.metrics = metrics;
    }

    public Page<ProductResponse> findAll(String name, Pageable pageable) {
        Page<Product> products = StringUtils.hasText(name)
                ? repository.findByNameContainingIgnoreCase(name, pageable)
                : repository.findAll(pageable);
        return products.map(ProductResponse::from);
    }

    public ProductResponse findById(Long id) {
        return ProductResponse.from(getProduct(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product(request.name(), request.description(), request.price());
        ProductResponse created = ProductResponse.from(repository.save(product));
        metrics.recordCreated();
        return created;
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProduct(id);
        product.update(request.name(), request.description(), request.price());
        metrics.recordUpdated();
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getProduct(id));
        metrics.recordDeleted();
    }

    private Product getProduct(Long id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
