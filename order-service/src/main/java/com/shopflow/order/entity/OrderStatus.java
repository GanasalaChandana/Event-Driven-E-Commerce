package com.shopflow.order.entity;

public enum OrderStatus {
    PENDING,      // Just placed — waiting for inventory confirmation
    CONFIRMED,    // Inventory reserved — ready to fulfil
    CANCELLED     // Inventory insufficient or user cancelled
}
