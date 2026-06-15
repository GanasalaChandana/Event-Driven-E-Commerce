package com.shopflow.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// Consumed by Order Service ← published by Inventory Service
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InventoryReservedEvent {
    private UUID orderId;
    private LocalDateTime reservedAt;
}
