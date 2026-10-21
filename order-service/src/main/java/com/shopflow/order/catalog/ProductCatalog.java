package com.shopflow.order.catalog;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Source of truth for product names and prices when an order is placed.
 * Clients only send product IDs and quantities; prices are never taken from the request.
 *
 * Implementations: {@link RemoteProductCatalog} (microservices — calls product-service over HTTP)
 * and the monolith's in-process catalog backed by the product repository.
 */
public interface ProductCatalog {

    Optional<ProductSnapshot> findActiveProduct(UUID productId);

    record ProductSnapshot(UUID id, String name, BigDecimal price) {}
}
