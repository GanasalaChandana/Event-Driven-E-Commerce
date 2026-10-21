package com.shopflow.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

// Published by Order Service → consumed by Inventory Service to give back stock held for a cancelled order
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InventoryReleaseEvent {
    private UUID orderId;
    private String reason;
}
