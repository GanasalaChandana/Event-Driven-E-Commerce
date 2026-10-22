package com.shopflow.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/** A saved delivery address in a user's address book. */
@Entity
@Table(name = "addresses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Address {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 150)
    private String line1;

    @Column(length = 150)
    private String line2;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(length = 30)
    private String phone;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
