package com.shopflow.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Published by Order Service → consumed by Notification Service
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderConfirmedEvent {
    private UUID orderId;
    private UUID userId;
    private String userEmail;
    private BigDecimal totalAmount;
    private LocalDateTime confirmedAt;
}
