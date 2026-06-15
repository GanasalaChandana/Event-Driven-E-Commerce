package com.shopflow.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Inventory {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    // Total units in warehouse
    @Column(nullable = false)
    private int quantity;

    // Units held for PENDING orders — not yet shipped
    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }
}
