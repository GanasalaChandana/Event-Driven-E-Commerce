package com.shopflow.inventory.controller;

import com.shopflow.inventory.dto.InventoryAdjustRequest;
import com.shopflow.inventory.dto.InventoryRequest;
import com.shopflow.inventory.dto.InventoryResponse;
import com.shopflow.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getStock(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.getByProductId(productId));
    }

    // Admin-only: set stock to an absolute quantity
    @PutMapping("/{productId}")
    public ResponseEntity<InventoryResponse> adjustStock(
            @PathVariable UUID productId,
            @Valid @RequestBody InventoryAdjustRequest request,
            Authentication auth) {
        if (auth == null || auth.getAuthorities().stream()
                .noneMatch(a -> a.equals(new SimpleGrantedAuthority("ROLE_ADMIN")))) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(inventoryService.adjustStock(productId, request));
    }

    // Admin-only: add stock to a product
    @PostMapping
    public ResponseEntity<InventoryResponse> addStock(
            @Valid @RequestBody InventoryRequest request,
            Authentication auth) {
        if (auth == null || auth.getAuthorities().stream()
                .noneMatch(a -> a.equals(new SimpleGrantedAuthority("ROLE_ADMIN")))) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(inventoryService.addStock(request));
    }
}
