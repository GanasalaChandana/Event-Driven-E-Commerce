package com.shopflow.user.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserRegisteredEvent {
    private UUID userId;
    private String name;
    private String email;
    private LocalDateTime registeredAt;
}
