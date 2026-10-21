package com.shopflow.order.exception;

import java.util.UUID;

public class ProductUnavailableException extends RuntimeException {
    public ProductUnavailableException(UUID productId) {
        super("Product not found or no longer available: " + productId);
    }
}
