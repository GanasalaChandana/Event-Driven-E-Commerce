package com.shopflow.order.dto;

import com.shopflow.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {
    @NotNull(message = "status is required")
    private OrderStatus status;
}
