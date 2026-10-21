package com.shopflow.order.catalog;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Looks products up in product-service (public GET /api/v1/products/{id}). */
public class RemoteProductCatalog implements ProductCatalog {

    private final RestClient restClient;

    public RemoteProductCatalog(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<ProductSnapshot> findActiveProduct(UUID productId) {
        try {
            ProductView product = restClient.get()
                    .uri("/api/v1/products/{id}", productId)
                    .retrieve()
                    .body(ProductView.class);
            if (product == null || !product.active() || product.price() == null) {
                return Optional.empty();
            }
            return Optional.of(new ProductSnapshot(product.id(), product.name(), product.price()));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    // Subset of product-service's ProductResponse
    record ProductView(UUID id, String name, BigDecimal price, boolean active) {}
}
