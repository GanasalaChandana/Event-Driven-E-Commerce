package com.shopflow.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryAdjustRequest {
    @NotNull(message = "quantity is required")
    @Min(value = 0, message = "quantity must be >= 0")
    private Integer quantity;
}
