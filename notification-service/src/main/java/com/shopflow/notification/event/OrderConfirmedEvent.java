package com.shopflow.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Consumed from order.confirmed topic ← published by Order Service
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderConfirmedEvent {
    private UUID orderId;
    private UUID userId;
    private String userEmail;
    private BigDecimal totalAmount;
    private LocalDateTime confirmedAt;
}
