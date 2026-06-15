package com.shopflow.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// Consumed from user.registered topic ← published by User Service
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserRegisteredEvent {
    private UUID userId;
    private String name;
    private String email;
    private LocalDateTime registeredAt;
}
