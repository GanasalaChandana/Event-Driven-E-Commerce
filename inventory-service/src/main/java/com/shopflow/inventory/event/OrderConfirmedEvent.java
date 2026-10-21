package com.shopflow.inventory.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

// Consumed by Inventory Service ← published by Order Service (only the order id is needed here)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderConfirmedEvent {
    private UUID orderId;
}
