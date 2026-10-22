package com.shopflow.product.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    private BigDecimal price;

    @NotBlank(message = "SKU is required")
    @Size(max = 100)
    private String sku;

    private UUID categoryId;

    @Size(max = 500, message = "Image URL must be at most 500 characters")
    @Pattern(regexp = "^(https?://\\S+)?$", message = "Image URL must start with http:// or https://")
    private String imageUrl;
}
