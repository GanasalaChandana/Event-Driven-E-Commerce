package com.shopflow.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    @Builder.Default
    private String type = "Bearer";
    private UserResponse user;

    public AuthResponse(String token, UserResponse user) {
        this.token = token;
        this.type = "Bearer";
        this.user = user;
    }

    // Required by Lombok @Builder
    public AuthResponse(String token, String type, UserResponse user) {
        this.token = token;
        this.type = type;
        this.user = user;
    }
}
