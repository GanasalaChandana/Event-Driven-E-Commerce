package com.shopflow.product.dto;

import com.shopflow.product.entity.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class ProductResponse {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String sku;
    private CategoryResponse category;
    private String imageUrl;
    private boolean active;
    private LocalDateTime createdAt;

    public static ProductResponse from(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .sku(p.getSku())
                .category(p.getCategory() != null ? CategoryResponse.from(p.getCategory()) : null)
                .imageUrl(p.getImageUrl())
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
