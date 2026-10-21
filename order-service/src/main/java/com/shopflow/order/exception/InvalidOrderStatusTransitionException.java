package com.shopflow.order.exception;

import com.shopflow.order.entity.OrderStatus;

import java.util.UUID;

public class InvalidOrderStatusTransitionException extends RuntimeException {
    public InvalidOrderStatusTransitionException(UUID orderId, OrderStatus from, OrderStatus to) {
        super("Order " + orderId + " cannot move from " + from + " to " + to);
    }
}
