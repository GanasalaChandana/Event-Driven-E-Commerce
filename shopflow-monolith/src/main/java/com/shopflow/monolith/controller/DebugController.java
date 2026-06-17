package com.shopflow.monolith.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {

    @GetMapping("/api/v1/me")
    public ResponseEntity<String> me(Authentication auth) {
        if (auth == null) return ResponseEntity.ok("NOT_AUTHENTICATED");
        return ResponseEntity.ok("user=" + auth.getName() + " roles=" + auth.getAuthorities());
    }
}
