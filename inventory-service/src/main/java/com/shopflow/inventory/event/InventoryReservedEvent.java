package com.shopflow.inventory.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// Published by Inventory Service → consumed by Order Service
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InventoryReservedEvent {
    private UUID orderId;
    private LocalDateTime reservedAt;
}
