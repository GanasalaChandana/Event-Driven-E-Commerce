package com.shopflow.product.dto;

import com.shopflow.product.entity.Category;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data @Builder
public class CategoryResponse {
    private UUID id;
    private String name;
    private String description;

    public static CategoryResponse from(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .build();
    }
}
