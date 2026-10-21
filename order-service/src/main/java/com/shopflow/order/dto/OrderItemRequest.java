package com.shopflow.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderItemRequest {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    /** Ignored — the product name is looked up server-side. Kept for backward compatibility. */
    private String productName;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 1000, message = "Quantity must be at most 1000")
    private int quantity;

    /** Ignored — the price is looked up server-side. Kept for backward compatibility. */
    private BigDecimal unitPrice;
}
