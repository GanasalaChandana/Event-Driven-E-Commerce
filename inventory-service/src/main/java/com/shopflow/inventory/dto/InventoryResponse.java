package com.shopflow.inventory.dto;

import com.shopflow.inventory.entity.Inventory;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data @Builder
public class InventoryResponse {
    private UUID productId;
    private String productName;
    private int totalQuantity;
    private int reservedQuantity;
    private int availableQuantity;

    public static InventoryResponse from(Inventory inv) {
        return InventoryResponse.builder()
                .productId(inv.getProductId())
                .productName(inv.getProductName())
                .totalQuantity(inv.getQuantity())
                .reservedQuantity(inv.getReservedQuantity())
                .availableQuantity(inv.getAvailableQuantity())
                .build();
    }
}
