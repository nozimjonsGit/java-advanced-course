package com.epam.module1.productapi.service;

import com.epam.module1.productapi.dto.ProductRequest;
import com.epam.module1.productapi.dto.ProductResponse;
import com.epam.module1.productapi.entity.Product;
import com.epam.module1.productapi.exception.ProductNotFoundException;
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

    public ProductService(ProductRepository repository) {
        this.repository = repository;
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
        return ProductResponse.from(repository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProduct(id);
        product.update(request.name(), request.description(), request.price());
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getProduct(id));
    }

    private Product getProduct(Long id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
