package com.shopflow.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

// Consumed by Order Service ← published by Inventory Service
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InventoryFailedEvent {
    private UUID orderId;
    private String reason;   // e.g. "Insufficient stock for product IPHONE-15"
}
