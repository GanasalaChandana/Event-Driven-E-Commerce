package com.shopflow.inventory.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

// Consumed by Inventory Service ← published by Order Service when an order is cancelled
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InventoryReleaseEvent {
    private UUID orderId;
    private String reason;
}
