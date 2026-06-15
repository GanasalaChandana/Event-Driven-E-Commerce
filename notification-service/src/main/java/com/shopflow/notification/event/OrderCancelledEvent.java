package com.shopflow.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// Consumed from order.cancelled topic ← published by Order Service
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderCancelledEvent {
    private UUID orderId;
    private UUID userId;
    private String userEmail;
    private String reason;
    private LocalDateTime cancelledAt;
}
