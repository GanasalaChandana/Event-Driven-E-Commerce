package com.shopflow.monolith.config;

import com.shopflow.order.catalog.ProductCatalog;
import com.shopflow.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** In-process product lookup for order pricing (the microservices call product-service instead). */
@Component
@RequiredArgsConstructor
public class LocalProductCatalog implements ProductCatalog {

    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductSnapshot> findActiveProduct(UUID productId) {
        return productRepository.findByIdAndActiveTrue(productId)
                .map(p -> new ProductSnapshot(p.getId(), p.getName(), p.getPrice()));
    }
}
