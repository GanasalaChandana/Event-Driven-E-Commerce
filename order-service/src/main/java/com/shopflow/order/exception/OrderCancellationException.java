package com.shopflow.order.exception;

import java.util.UUID;

public class OrderCancellationException extends RuntimeException {
    public OrderCancellationException(UUID orderId, String reason) {
        super("Cannot cancel order " + orderId + ": " + reason);
    }
}
