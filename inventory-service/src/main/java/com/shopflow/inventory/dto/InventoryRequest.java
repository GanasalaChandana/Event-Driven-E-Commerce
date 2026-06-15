package com.shopflow.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class InventoryRequest {

    @NotNull
    private UUID productId;

    @NotBlank
    private String productName;

    @Min(value = 1, message = "Must add at least 1 unit")
    private int quantityToAdd;
}
