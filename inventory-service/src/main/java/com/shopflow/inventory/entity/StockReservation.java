package com.shopflow.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stock held for one product of one order. Makes reserve/commit/release idempotent:
 * a redelivered event (or the monolith's sync path racing the Kafka path) finds the
 * existing rows instead of reserving the same stock twice.
 */
@Entity
@Table(name = "stock_reservations",
       uniqueConstraints = @UniqueConstraint(name = "uk_reservation_order_product",
                                             columnNames = {"order_id", "product_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockReservation {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Status {
        RESERVED,   // counted in Inventory.reservedQuantity
        COMMITTED,  // order confirmed — deducted from Inventory.quantity
        RELEASED    // order cancelled — stock given back
    }
}
